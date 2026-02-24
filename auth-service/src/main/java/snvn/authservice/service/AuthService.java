package snvn.authservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import snvn.authservice.dto.AuthRequest;
import snvn.authservice.dto.AuthResponse;
import snvn.authservice.dto.RegisterRequest;
import snvn.authservice.model.User;
import snvn.authservice.repository.UserRepository;
import snvn.authservice.security.JwtTokenProvider;

/**
 * Service for handling authentication business logic
 */
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Register a new user
     */
    public AuthResponse register(RegisterRequest request) {
        // Check if user already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : "USER");
        user.setEnabled(true);

        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateToken(user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        return new AuthResponse(accessToken, refreshToken, user.getUsername(), user.getRole());
    }

    /**
     * Authenticate user
     */
    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtTokenProvider.generateToken(user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        return new AuthResponse(accessToken, refreshToken, user.getUsername(), user.getRole());
    }

    /**
     * Refresh access token
     */
    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken.startsWith("Bearer ")) {
            refreshToken = refreshToken.substring(7);
        }

        if (jwtTokenProvider.validateToken(refreshToken)) {
            String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String newAccessToken = jwtTokenProvider.generateToken(username);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

            return new AuthResponse(newAccessToken, newRefreshToken, user.getUsername(), user.getRole());
        }

        throw new RuntimeException("Invalid refresh token");
    }

    /**
     * Logout user
     */
    public void logout(String token) {
        // Implement token blacklisting logic here if needed
        // For now, just a placeholder
        System.out.println("User logged out with token: " + token);
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtTokenProvider.validateToken(token);
    }
}

