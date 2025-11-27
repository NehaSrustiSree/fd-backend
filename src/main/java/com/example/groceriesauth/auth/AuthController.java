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

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final CredentialsRepository credentialsRepository;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, CredentialsRepository credentialsRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.credentialsRepository = credentialsRepository;
        this.jwtService = jwtService;
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
        u.setPasswordHash(req.password()); // In production, hash this!
        userRepository.save(u);
        // also create credentials record (keeps password in a dedicated table)
        Credentials cred = new Credentials();
        cred.setUser(u);
        cred.setPasswordHash(req.password()); // In production: store a salted hash
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
            .filter(c -> c.getPasswordHash().equals(req.password()))
            .map(c -> {
                User u = c.getUser();
                String token = jwtService.issueToken(Map.of("email", u.getEmail(), "name", u.getName()));
                ResponseCookie cookie = ResponseCookie.from("auth_token", token)
                    .httpOnly(true).secure(false).sameSite("Lax").path("/").maxAge(7 * 24 * 60 * 60).build();
                return ResponseEntity.ok()
                    .header("Set-Cookie", cookie.toString())
                    .body(Map.of("user", Map.of("name", u.getName(), "email", u.getEmail())));
            })
            .orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "Invalid credentials")));
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


