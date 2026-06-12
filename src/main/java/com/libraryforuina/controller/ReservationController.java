package com.libraryforuina.controller;

import com.libraryforuina.dto.request.ReservationRequest;
import com.libraryforuina.entity.Reservation;
import com.libraryforuina.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations", description = "Rezerwowanie i zarządzanie rezerwacjami książek")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping
    @Operation(summary = "Pobierz wszystkie rezerwacje (tylko ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Reservation>> getAll() {
        return ResponseEntity.ok(reservationService.getAll());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Pobierz rezerwacje danego użytkownika (ADMIN lub właściciel)")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<Reservation>> getByUser(
            @Parameter(description = "ID użytkownika") @PathVariable Long userId) {
        return ResponseEntity.ok(reservationService.getByUser(userId));
    }

    @PostMapping
    @Operation(summary = "Zarezerwuj książkę")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Reservation> reserve(
            @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.reserve(request));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Anuluj rezerwację")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Reservation> cancel(
            @Parameter(description = "ID rezerwacji do anulowania") @PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancel(id));
    }
}
