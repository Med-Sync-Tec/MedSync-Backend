package itesm.medsync.infrastructure.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.FileInputStream;
import java.io.IOException;

@ApplicationScoped
public class FirebaseInitializer {

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
                System.out.println("Firebase Admin SDK inicializado exitosamente.");
            }
        } catch (IOException e) {
            System.err.println("Error al inicializar Firebase Admin SDK: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
