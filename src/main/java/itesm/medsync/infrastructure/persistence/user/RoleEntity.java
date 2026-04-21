package itesm.medsync.infrastructure.persistence.user;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class RoleEntity extends PanacheEntityBase {
    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    public UUID id;

    @Column(nullable = false, unique = true)
    public String nombre;

    public String descripcion;
}
