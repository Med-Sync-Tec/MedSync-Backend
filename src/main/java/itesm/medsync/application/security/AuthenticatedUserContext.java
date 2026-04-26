package itesm.medsync.application.security;

import itesm.medsync.domain.user.model.UserWithRole;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class AuthenticatedUserContext {

    private UserWithRole currentUser;

    public UserWithRole getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserWithRole user) {
        this.currentUser = user;
    }
}
