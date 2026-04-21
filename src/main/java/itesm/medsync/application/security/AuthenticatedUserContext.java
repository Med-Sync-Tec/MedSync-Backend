package itesm.medsync.application.security;

import itesm.medsync.domain.user.model.User;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class AuthenticatedUserContext {

    private User currentUser;

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
}
