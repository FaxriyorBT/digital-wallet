package uz.digitalwallet.user;

import java.util.UUID;

/**
 * Published in-process when existing sessions of a user must stop working (password change, blocking).
 * The auth module listens to it, so the user module does not need to know how sessions are stored.
 */
public record UserAccessRevokedEvent(UUID userId) {
}
