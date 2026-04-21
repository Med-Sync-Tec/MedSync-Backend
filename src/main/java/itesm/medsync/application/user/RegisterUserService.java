package itesm.medsync.application.user;

import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.user.repository.UserRepository;
import itesm.medsync.infrastructure.persistence.user.RoleEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Optional;

@ApplicationScoped
public class RegisterUserService {

    @Inject
    UserRepository userRepository;

    @Transactional
    public User loginOrRegister(String email, String name) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        User newUser = new User();
        newUser.setCorreo(email);
        newUser.setNombre(name);
        newUser.setActivo(true);
        
        // Asignar rol por defecto (DOCTOR)
        RoleEntity doctorRole = RoleEntity.find("nombre", "DOCTOR").firstResult();
        if (doctorRole != null) {
            newUser.setRolId(doctorRole.id);
        }
        
        return userRepository.save(newUser);
    }
}
