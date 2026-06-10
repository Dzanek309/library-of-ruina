package com.libraryforuina.controller;

import com.libraryforuina.dto.request.LoginRequest;
import com.libraryforuina.dto.request.RegisterRequest;
import com.libraryforuina.dto.response.JwtResponse;
import com.libraryforuina.entity.User;
import com.libraryforuina.security.JwtTokenProvider;
import com.libraryforuina.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Rejestracja i logowanie")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "Zaloguj użytkownika i otrzymaj token JWT")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenProvider.generateToken(authentication);
        User user = userService.findByUsername(request.getUsername());

        return ResponseEntity.ok(new JwtResponse(token, user.getId(), user.getUsername(), user.getRole().name()));
    }

    @PostMapping("/register")
    @Operation(summary = "Zarejestruj nowego użytkownika")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request.getUsername(), request.getEmail(), request.getPassword());
        return ResponseEntity.ok("Rejestracja zakończona sukcesem");
    }
}
