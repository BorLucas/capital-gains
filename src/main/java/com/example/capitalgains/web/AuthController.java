package com.example.capitalgains.web;

import com.example.capitalgains.application.AuthService;
import com.example.capitalgains.application.NotAuthenticatedException;
import com.example.capitalgains.application.UserView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign-up, login and logout over the servlet container's own HTTP session: the
 * browser holds only the {@code JSESSIONID} cookie (HttpOnly, SameSite=Strict).
 */
@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Auth", description = "Broker users: sign-up and session login")
public class AuthController {

    /** Registration rules. The password only needs 6+ characters; anything goes, "123456" included. */
    public record SignUp(
            @NotBlank(message = "username is required")
            @Pattern(regexp = "^[A-Za-z0-9._-]{3,30}$",
                    message = "username must be 3-30 letters, digits, dots, dashes or underscores")
            String username,

            @NotBlank(message = "password is required")
            @Size(min = 6, max = 128, message = "password must have at least 6 characters")
            String password) {
    }

    /** Login is not validated for shape: a wrong login is always the same vague 401. */
    public record Login(String username, String password) {
    }

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Creates an account and logs it in")
    public ResponseEntity<UserView> register(@RequestBody @Valid SignUp body, HttpServletRequest request) {
        UserView user = auth.register(body.username(), body.password());
        Sessions.start(request, user.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Logs in; 401 on a wrong username or password")
    public UserView login(@RequestBody Login body, HttpServletRequest request) {
        UserView user = auth.login(body.username(), body.password() == null ? "" : body.password());
        Sessions.start(request, user.id());
        return user;
    }

    @PostMapping("/logout")
    @Operation(summary = "Ends the session")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "The logged-in user; 401 when there is no session")
    public UserView me(HttpSession session) {
        return auth.find(Sessions.userId(session)).orElseThrow(NotAuthenticatedException::new);
    }
}
