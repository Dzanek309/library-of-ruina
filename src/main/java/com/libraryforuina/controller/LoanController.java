package com.libraryforuina.controller;

import com.libraryforuina.dto.request.LoanRequest;
import com.libraryforuina.entity.Loan;
import com.libraryforuina.service.LoanService;
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
@RequestMapping("/api/loans")
@Tag(name = "Loans", description = "Zarządzanie wypożyczeniami i zwrotami książek")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @GetMapping
    @Operation(summary = "Pobierz wszystkie wypożyczenia (tylko ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Loan>> getAll() {
        return ResponseEntity.ok(loanService.getAll());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Historia wypożyczeń danego użytkownika (ADMIN lub właściciel)")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<Loan>> getHistory(
            @Parameter(description = "ID użytkownika") @PathVariable Long userId) {
        return ResponseEntity.ok(loanService.getHistoryByUser(userId));
    }

    @PostMapping
    @Operation(summary = "Wypożycz książkę — termin zwrotu liczony polimorficznie z typu książki")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Loan> borrow(
            @Valid @RequestBody LoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loanService.borrow(request));
    }

    @PatchMapping("/{id}/return")
    @Operation(summary = "Zwróć wypożyczoną książkę")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Loan> returnBook(
            @Parameter(description = "ID wypożyczenia") @PathVariable Long id) {
        return ResponseEntity.ok(loanService.returnBook(id));
    }
}
