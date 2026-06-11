package com.libraryforuina.controller;

import com.libraryforuina.dto.request.BookRequest;
import com.libraryforuina.entity.Book;
import com.libraryforuina.service.BookService;
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
@RequestMapping("/api/books")
@Tag(name = "Books", description = "Przegladanie, wyszukiwanie i zarzadzanie ksiazkami")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    @Operation(summary = "Pobierz wszystkie ksiazki (przegladanie katalogu)")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<Book>> getAll() {
        return ResponseEntity.ok(bookService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Pobierz ksiazke po ID")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Book> getById(
            @Parameter(description = "ID ksiazki") @PathVariable Long id) {
        return ResponseEntity.ok(bookService.getById(id));
    }

    @GetMapping("/search")
    @Operation(summary = "Wyszukiwanie ksiazek (wzorzec Strategy)",
            description = "strategy = title | author | category | available")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<Book>> search(
            @Parameter(description = "Strategia: title/author/category/available") @RequestParam String strategy,
            @Parameter(description = "Szukana wartosc (pomijana dla 'available')") @RequestParam(required = false, defaultValue = "") String value) {
        return ResponseEntity.ok(bookService.search(strategy, value));
    }

    @GetMapping("/{id}/format-info")
    @Operation(summary = "Opis formatu ksiazki — demonstracja polimorfizmu")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<String> formatInfo(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getById(id).getFormatInfo());
    }

    @PostMapping
    @Operation(summary = "Dodaj nowa ksiazke (tylko ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Book> create(@Valid @RequestBody BookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Zaktualizuj ksiazke (tylko ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Book> update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return ResponseEntity.ok(bookService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Usun ksiazke (tylko ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
