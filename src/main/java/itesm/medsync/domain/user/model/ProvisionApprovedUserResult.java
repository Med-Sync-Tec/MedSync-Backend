package itesm.medsync.domain.user.model;

/**
 * Result of provisioning an approved-solicitud user.
 * Carries the created user and the Firebase password-reset link
 * so the caller can include it in the approval email.
 */
public record ProvisionApprovedUserResult(UserWithRole user, String passwordResetLink) {}
