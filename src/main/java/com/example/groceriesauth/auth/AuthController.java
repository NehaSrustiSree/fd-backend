package com.example.groceriesauth.auth;

import com.example.groceriesauth.security.JwtService;
import com.example.groceriesauth.user.User;
import com.example.groceriesauth.user.UserRepository;
import com.example.groceriesauth.user.Credentials;
import com.example.groceriesauth.user.CredentialsRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final CredentialsRepository credentialsRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, CredentialsRepository credentialsRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.credentialsRepository = credentialsRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public record SignupRequest(@NotBlank String name, @Email String email, @NotBlank String password) {}
    public record LoginRequest(@Email String email, @NotBlank String password) {}

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            return ResponseEntity.status(409).body(Map.of("message", "Email already registered"));
        }
        User u = new User();
        u.setName(req.name());
        u.setEmail(req.email().toLowerCase());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        userRepository.save(u);
        // also create credentials record (keeps password in a dedicated table)
        Credentials cred = new Credentials();
        cred.setUser(u);
        cred.setPasswordHash(passwordEncoder.encode(req.password()));
        credentialsRepository.save(cred);
        String token = jwtService.issueToken(Map.of("email", u.getEmail(), "name", u.getName()));
        ResponseCookie cookie = ResponseCookie.from("auth_token", token)
                .httpOnly(true).secure(false).sameSite("Lax").path("/").maxAge(7 * 24 * 60 * 60).build();
        return ResponseEntity.ok()
                .header("Set-Cookie", cookie.toString())
                .body(Map.of("user", Map.of("name", u.getName(), "email", u.getEmail())));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        // find credentials by email (via user relationship)
        String normalized = req.email().toLowerCase();
        return credentialsRepository.findByUser_Email(normalized)
            .filter(c -> passwordEncoder.matches(req.password(), c.getPasswordHash()))
            .map(c -> {
                User u = c.getUser();
                String token = jwtService.issueToken(Map.of("email", u.getEmail(), "name", u.getName()));
                ResponseCookie cookie = ResponseCookie.from("auth_token", token)
                    .httpOnly(true).secure(false).sameSite("Lax").path("/").maxAge(7 * 24 * 60 * 60).build();
                Map<String, String> userMap = Map.of("name", u.getName(), "email", u.getEmail());
                Map<String, Object> body = Map.of("user", userMap);
                return ResponseEntity.ok()
                    .header("Set-Cookie", cookie.toString())
                    .body(body);
            })
            .orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "Invalid credentials")));
    }

    // One-off migration endpoint to bcrypt-encode any plaintext credentials in the DB.
    // Call this once after deploy to convert existing entries created with plaintext passwords.
    @PostMapping("/migrate-passwords")
    public ResponseEntity<?> migratePasswords() {
        Iterable<Credentials> all = credentialsRepository.findAll();
        int updated = 0;
        for (Credentials c : all) {
            String ph = c.getPasswordHash();
            if (ph == null) continue;
            // crude check for BCrypt prefix
            if (!ph.startsWith("$2a$") && !ph.startsWith("$2b$") && !ph.startsWith("$2y$")) {
                c.setPasswordHash(passwordEncoder.encode(ph));
                credentialsRepository.save(c);
                updated++;
            }
        }
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie cookie = ResponseCookie.from("auth_token", "").httpOnly(true).secure(false).sameSite("Lax").path("/").maxAge(0).build();
        return ResponseEntity.ok().header("Set-Cookie", cookie.toString()).body(Map.of("ok", true));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
        }
        return userRepository.findByEmail(principal.getUsername())
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of("user", Map.of("name", u.getName(), "email", u.getEmail()))))
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "Not authenticated")));
    }
}


