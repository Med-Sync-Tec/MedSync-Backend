package itesm.medsync.infrastructure.persistence.user;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
public class UserEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    // Definición para el ID principal
    @Column(columnDefinition = "BINARY(16)")
    public UUID id;

    public String nombre;
    public String correo;

    // Definición para la llave foránea/referencia al rol
    @Column(name = "rol_id", columnDefinition = "BINARY(16)")
    public UUID rolId;

    public boolean activo = true;
}