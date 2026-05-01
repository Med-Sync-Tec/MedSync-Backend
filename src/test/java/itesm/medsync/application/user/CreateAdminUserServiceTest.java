package itesm.medsync.application.user;

import itesm.medsync.domain.user.exception.RoleNotFoundException;
import itesm.medsync.domain.user.exception.UserAlreadyExistsException;
import itesm.medsync.domain.user.model.KnownRoles;
import itesm.medsync.domain.user.model.Role;
import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.user.model.UserWithRole;
import itesm.medsync.domain.user.repository.FirebaseUserGateway;
import itesm.medsync.domain.user.repository.RoleRepository;
import itesm.medsync.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateAdminUserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    FirebaseUserGateway firebaseUserGateway;

    @InjectMocks
    CreateAdminUserService service;

    private Role doctorRole() {
        return new Role(UUID.randomUUID(), KnownRoles.DOCTOR, "Médico", null);
    }

    @Test
    @DisplayName("Happy path: crea en Firebase, guarda en BD, devuelve UserWithRole")
    void createOk() {
        Role role = doctorRole();
        when(roleRepository.findByNombre(KnownRoles.DOCTOR)).thenReturn(Optional.of(role));
        when(userRepository.findByEmail("nuevo@tec.mx")).thenReturn(Optional.empty());
        when(firebaseUserGateway.createUser("nuevo@tec.mx", "secret123", "Nuevo")).thenReturn("fb-uid-1");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserWithRole result = service.execute("nuevo@tec.mx", "Nuevo", "secret123", KnownRoles.DOCTOR);

        assertNotNull(result);
        assertEquals(KnownRoles.DOCTOR, result.roleName());
        assertEquals("nuevo@tec.mx", result.user().getCorreo());
        verify(firebaseUserGateway).createUser("nuevo@tec.mx", "secret123", "Nuevo");
        verify(firebaseUserGateway, never()).deleteUser(any());
    }

    @Test
    @DisplayName("Rol desconocido: lanza RoleNotFoundException, NO toca Firebase")
    void createRoleNotFound() {
        when(roleRepository.findByNombre("CHEF")).thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class,
                () -> service.execute("x@tec.mx", "X", "secret123", "CHEF"));

        verifyNoInteractions(firebaseUserGateway);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Email ya existe en BD: lanza UserAlreadyExistsException antes de Firebase")
    void createEmailExists() {
        Role role = doctorRole();
        User existing = User.create("Old", "x@tec.mx", role.getId());
        when(roleRepository.findByNombre(KnownRoles.DOCTOR)).thenReturn(Optional.of(role));
        when(userRepository.findByEmail("x@tec.mx"))
                .thenReturn(Optional.of(new UserWithRole(existing, KnownRoles.DOCTOR)));

        assertThrows(UserAlreadyExistsException.class,
                () -> service.execute("x@tec.mx", "X", "secret123", KnownRoles.DOCTOR));

        verifyNoInteractions(firebaseUserGateway);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Falla DB después de crear en Firebase: rollback borra usuario de Firebase")
    void createDbFailureRollsBackFirebase() {
        Role role = doctorRole();
        when(roleRepository.findByNombre(KnownRoles.DOCTOR)).thenReturn(Optional.of(role));
        when(userRepository.findByEmail("x@tec.mx")).thenReturn(Optional.empty());
        when(firebaseUserGateway.createUser(any(), any(), any())).thenReturn("fb-uid-2");
        when(userRepository.save(any(User.class)))
                .thenThrow(new RuntimeException("DB constraint violation"));

        assertThrows(RuntimeException.class,
                () -> service.execute("x@tec.mx", "X", "secret123", KnownRoles.DOCTOR));

        verify(firebaseUserGateway).deleteUser("fb-uid-2");
    }
}
