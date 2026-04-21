package itesm.medsync.infrastructure.persistence.user;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import itesm.medsync.domain.user.model.User; // Importar el modelo de dominio
import itesm.medsync.domain.user.repository.UserRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class UserRepositoryImpl implements UserRepository, PanacheRepositoryBase<UserEntity, UUID> {
    @Override
    public Optional<User> findByEmail(String email) {
        return find("correo", email).firstResultOptional()
                .map(entity -> new User(entity.id, entity.nombre, entity.correo, entity.rolId, entity.activo));
    }

    @Override
    public User save(User user) {
        UserEntity entity;
        if (user.getId() != null) {
            entity = findById(user.getId());
            if (entity == null) {
                entity = new UserEntity();
            }
        } else {
            entity = new UserEntity();
        }

        entity.nombre = user.getNombre();
        entity.correo = user.getCorreo();
        entity.rolId = user.getRolId();
        entity.activo = user.isActivo();

        if (entity.id == null) {
            persist(entity);
            user.setId(entity.id);
        } else {
            // Se asume que el objeto ya está gestionado por el contexto de persistencia
            // o se actualizará automáticamente al final de la transacción.
        }

        return user;
    }
}