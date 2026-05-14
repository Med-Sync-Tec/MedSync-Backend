package itesm.medsync.domain.user.usecase;

import itesm.medsync.domain.user.model.ProvisionApprovedUserResult;

/**
 * Provisions a user whose access request was approved.
 * Password is generated internally — never supplied by the caller.
 * Returns the created user and a Firebase password-reset link.
 */
public interface ProvisionApprovedUserUseCase {
    ProvisionApprovedUserResult execute(String email, String nombre, String roleName);
}
