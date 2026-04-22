package itesm.medsync.application.user;

import itesm.medsync.domain.user.exception.RoleNotFoundException;
import itesm.medsync.domain.user.model.Role;
import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.user.repository.RoleRepository;
import itesm.medsync.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @InjectMocks
    RegisterUserService service;

    @Test
    @DisplayName("Usuario existe → devuelve existente, no consulta rol ni guarda")
    void loginExistingUser() {
        User existing = new User(UUID.randomUUID(), "Juan", "juan@tec.mx", UUID.randomUUID(), true);
        when(userRepository.findByEmail("juan@tec.mx")).thenReturn(Optional.of(existing));

        User result = service.loginOrRegister("juan@tec.mx", "Juan");

        assertSame(existing, result);
        verify(roleRepository, never()).findByNombre(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Usuario no existe → crea con rol DOCTOR y guarda")
    void registerNewUser() {
        Role doctor = new Role(UUID.randomUUID(), "DOCTOR", "Personal médico", null);
        when(userRepository.findByEmail("nuevo@tec.mx")).thenReturn(Optional.empty());
        when(roleRepository.findByNombre("DOCTOR")).thenReturn(Optional.of(doctor));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.loginOrRegister("nuevo@tec.mx", "Nuevo Usuario");

        assertEquals("nuevo@tec.mx", result.getCorreo());
        assertEquals("Nuevo Usuario", result.getNombre());
        assertEquals(doctor.getId(), result.getRolId());
        assertTrue(result.isActivo());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("nuevo@tec.mx", captor.getValue().getCorreo());
        assertEquals(doctor.getId(), captor.getValue().getRolId());
    }

    @Test
    @DisplayName("Rol DOCTOR no sembrado → RoleNotFoundException, no guarda")
    void registerFailsIfDoctorRoleMissing() {
        when(userRepository.findByEmail("x@tec.mx")).thenReturn(Optional.empty());
        when(roleRepository.findByNombre("DOCTOR")).thenReturn(Optional.empty());

        RoleNotFoundException ex = assertThrows(RoleNotFoundException.class,
                () -> service.loginOrRegister("x@tec.mx", "X"));
        assertTrue(ex.getMessage().contains("DOCTOR"));
        verify(userRepository, never()).save(any());
    }
}
