package com.libraryforuina.controller;

import com.libraryforuina.entity.User;
import com.libraryforuina.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Zarządzanie użytkownikami (RBAC)")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Pobierz wszystkich użytkowników (tylko ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Pobierz użytkownika po ID (ADMIN lub właściciel konta)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> getById(
            @Parameter(description = "ID użytkownika") @PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Usuń użytkownika (tylko ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID użytkownika do usunięcia") @PathVariable Long id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/promote")
    @Operation(summary = "Nadaj użytkownikowi rolę ADMIN (tylko ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> promoteToAdmin(
            @Parameter(description = "ID użytkownika do awansu") @PathVariable Long id) {
        return ResponseEntity.ok(userService.promoteToAdmin(id));
    }
}
