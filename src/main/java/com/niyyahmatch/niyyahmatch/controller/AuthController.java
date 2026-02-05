package com.niyyahmatch.niyyahmatch.controller;

import com.niyyahmatch.niyyahmatch.config.JwtUtil;
import com.niyyahmatch.niyyahmatch.dto.LoginRequest;
import com.niyyahmatch.niyyahmatch.dto.LoginResponse;
import com.niyyahmatch.niyyahmatch.entity.User;
import com.niyyahmatch.niyyahmatch.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil){
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        String email = request.getEmail();
        String password = request.getPassword();

        // Find user by email - returns Optional in case user doesn't exist
        Optional<User> userOptional = userService.findUserByEmail(email);

        // If user doesn't exist, throw 401 Unauthorized (invalid credentials)
        if (userOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        User user = userOptional.get();

        // Verify password using BCrypt comparison - UserService handles the hashing logic
        boolean isPasswordCorrect = userService.verifyPassword(password, user.getPassword());

        if (!isPasswordCorrect) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        // Generate JWT token with userId and email embedded in the payload
        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .build();

        return ResponseEntity.ok(response);
    }
}
