package com.example.capitalgains.application;

import com.example.capitalgains.broker.UserAccount;
import com.example.capitalgains.broker.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * Sign-up and login. Usernames are case-insensitive (stored lowercase). The only
 * password rule is the minimum length, enforced at the web edge.
 */
@Service
public class AuthService {

    private final UserAccountRepository users;
    private final PasswordHasher hasher;
    private final Clock clock;

    public AuthService(UserAccountRepository users, PasswordHasher hasher, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.clock = clock;
    }

    @Transactional
    public UserView register(String username, String password) {
        String normalized = normalize(username);
        if (users.existsByUsername(normalized)) {
            throw new UsernameTakenException(normalized);
        }
        try {
            UserAccount account = users.saveAndFlush(
                    new UserAccount(normalized, hasher.hash(password), Instant.now(clock)));
            return UserView.of(account);
        } catch (DataIntegrityViolationException raced) {
            // two sign-ups for the same name at once: the unique constraint decides
            throw new UsernameTakenException(normalized);
        }
    }

    /** Needs the current password even with a live session, so a borrowed session cannot lock the owner out. */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        UserAccount account = users.findById(userId).orElseThrow(NotAuthenticatedException::new);
        if (!hasher.matches(currentPassword, account.getPasswordHash())) {
            throw new IncorrectPasswordException();
        }
        account.changePasswordHash(hasher.hash(newPassword));
    }

    @Transactional(readOnly = true)
    public UserView login(String username, String password) {
        return users.findByUsername(normalize(username))
                .filter(account -> hasher.matches(password, account.getPasswordHash()))
                .map(UserView::of)
                .orElseThrow(InvalidCredentialsException::new);
    }

    @Transactional(readOnly = true)
    public Optional<UserView> find(Long id) {
        return users.findById(id).map(UserView::of);
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
