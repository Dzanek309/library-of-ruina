package com.libraryforuina.dto.request;

import com.libraryforuina.enums.BookFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class BookRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String isbn;

    private Integer publicationYear;

    @NotNull(message = "Format jest wymagany: PAPER / EBOOK / AUDIOBOOK")
    private BookFormat format;

    @NotNull
    @Min(value = 1, message = "Liczba egzemplarzy musi byc >= 1")
    private Integer totalCopies;

    private String description;

    private Long categoryId;

    private Set<Long> authorIds = new HashSet<>();

    // pola specyficzne dla podklas (opcjonalne — uzywane wg formatu)
    private Integer pages;            // PaperBook
    private String fileFormat;        // Ebook
    private Integer durationMinutes;  // Audiobook
    private String narrator;          // Audiobook
}
