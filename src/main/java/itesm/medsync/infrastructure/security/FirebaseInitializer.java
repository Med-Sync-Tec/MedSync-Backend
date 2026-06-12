package itesm.medsync.infrastructure.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.FileInputStream;
import java.io.IOException;

@ApplicationScoped
public class FirebaseInitializer {

    private static final Logger LOG = Logger.getLogger(FirebaseInitializer.class);

    @ConfigProperty(name = "firebase.config.path")
    String firebaseConfigPath;

    public void initialize(@Observes StartupEvent ev) {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FileInputStream serviceAccount = new FileInputStream(firebaseConfigPath);

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                LOG.info("Firebase Admin SDK inicializado exitosamente.");
            }
        } catch (IOException e) {
            LOG.errorf(e, "Error al inicializar Firebase Admin SDK: %s", e.getMessage());
        }
    }
}
