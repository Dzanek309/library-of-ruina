package com.libraryforuina.service;

import com.libraryforuina.entity.Audiobook;
import com.libraryforuina.entity.Ebook;
import com.libraryforuina.entity.PaperBook;
import com.libraryforuina.enums.BookFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy polimorfizmu hierarchii Book.
 * Weryfikują, że każda podklasa poprawnie implementuje getLoanPeriodDays()
 * i getFormatInfo() — kluczowy element wymagań formalnych (polimorfizm).
 *
 * Nie wymagają Mockito ani Spring — to czyste testy jednostkowe encji.
 */
@DisplayName("Book polymorphism unit tests")
class BookPolymorphismTest {

    // ── getLoanPeriodDays() ───────────────────────────────────────────────────

    @Test
    @DisplayName("PaperBook.getLoanPeriodDays() returns 14")
    void paperBook_loanPeriodDays_is14() {
        PaperBook book = PaperBook.builder()
                .title("Test").isbn("X").format(BookFormat.PAPER)
                .totalCopies(1).availableCopies(1).build();

        assertThat(book.getLoanPeriodDays()).isEqualTo(14);
    }

    @Test
    @DisplayName("Ebook.getLoanPeriodDays() returns 30")
    void ebook_loanPeriodDays_is30() {
        Ebook book = Ebook.builder()
                .title("Test").isbn("X").format(BookFormat.EBOOK)
                .totalCopies(1).availableCopies(1).build();

        assertThat(book.getLoanPeriodDays()).isEqualTo(30);
    }

    @Test
    @DisplayName("Audiobook.getLoanPeriodDays() returns 21")
    void audiobook_loanPeriodDays_is21() {
        Audiobook book = Audiobook.builder()
                .title("Test").isbn("X").format(BookFormat.AUDIOBOOK)
                .totalCopies(1).availableCopies(1).build();

        assertThat(book.getLoanPeriodDays()).isEqualTo(21);
    }

    @Test
    @DisplayName("Each subclass returns a different loan period — true polymorphism")
    void allSubclasses_haveDifferentLoanPeriods() {
        int paper = new PaperBook().getLoanPeriodDays();
        int ebook = new Ebook().getLoanPeriodDays();
        int audio = new Audiobook().getLoanPeriodDays();

        assertThat(paper).isNotEqualTo(ebook);
        assertThat(ebook).isNotEqualTo(audio);
        assertThat(paper).isNotEqualTo(audio);
    }

    // ── getFormatInfo() ───────────────────────────────────────────────────────

    @Test
    @DisplayName("PaperBook.getFormatInfo() contains page count when pages set")
    void paperBook_formatInfo_containsPages() {
        PaperBook book = PaperBook.builder()
                .title("T").isbn("X").format(BookFormat.PAPER)
                .totalCopies(1).availableCopies(1).pages(250).build();

        assertThat(book.getFormatInfo()).contains("250");
    }

    @Test
    @DisplayName("PaperBook.getFormatInfo() works without pages")
    void paperBook_formatInfo_noPages() {
        PaperBook book = PaperBook.builder()
                .title("T").isbn("X").format(BookFormat.PAPER)
                .totalCopies(1).availableCopies(1).build();

        assertThat(book.getFormatInfo()).contains("papierowa");
    }

    @Test
    @DisplayName("Ebook.getFormatInfo() contains file format when set")
    void ebook_formatInfo_containsFileFormat() {
        Ebook book = Ebook.builder()
                .title("T").isbn("X").format(BookFormat.EBOOK)
                .totalCopies(1).availableCopies(1).fileFormat("EPUB").build();

        assertThat(book.getFormatInfo()).contains("EPUB");
    }

    @Test
    @DisplayName("Audiobook.getFormatInfo() contains narrator and duration when set")
    void audiobook_formatInfo_containsNarratorAndDuration() {
        Audiobook book = Audiobook.builder()
                .title("T").isbn("X").format(BookFormat.AUDIOBOOK)
                .totalCopies(1).availableCopies(1)
                .durationMinutes(120).narrator("Jan Kowalski").build();

        assertThat(book.getFormatInfo()).contains("Jan Kowalski").contains("120");
    }

    @Test
    @DisplayName("Calling getLoanPeriodDays() via Book reference demonstrates runtime polymorphism")
    void polymorphicCall_viaBookReference() {
        com.libraryforuina.entity.Book book = PaperBook.builder()
                .title("T").isbn("X").format(BookFormat.PAPER)
                .totalCopies(1).availableCopies(1).build();

        // wywołanie przez referencję do klasy abstrakcyjnej — polimorfizm w akcji
        assertThat(book.getLoanPeriodDays()).isEqualTo(14);
        assertThat(book.getFormatInfo()).contains("papierowa");
    }
}
