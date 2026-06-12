package com.libraryforuina.service;

import com.libraryforuina.dto.request.BookRequest;
import com.libraryforuina.entity.*;
import com.libraryforuina.enums.BookFormat;
import com.libraryforuina.exception.BusinessException;
import com.libraryforuina.exception.ResourceNotFoundException;
import com.libraryforuina.repository.AuthorRepository;
import com.libraryforuina.repository.BookRepository;
import com.libraryforuina.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService unit tests")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private BookService bookService;

    private PaperBook paperBook;
    private Ebook ebook;
    private Audiobook audiobook;
    private Category category;
    private Author author;

    @BeforeEach
    void setUp() {
        category = Category.builder().id(1L).name("Fantasy").build();
        author = Author.builder().id(1L).firstName("Adam").lastName("Mickiewicz").build();

        paperBook = PaperBook.builder()
                .id(1L).title("Pan Tadeusz").isbn("978-83-000-0001-0")
                .format(BookFormat.PAPER).totalCopies(5).availableCopies(5)
                .pages(400).category(category).build();

        ebook = Ebook.builder()
                .id(2L).title("Wiedźmin").isbn("978-83-000-0002-0")
                .format(BookFormat.EBOOK).totalCopies(10).availableCopies(10)
                .fileFormat("EPUB").build();

        audiobook = Audiobook.builder()
                .id(3L).title("Lalka").isbn("978-83-000-0003-0")
                .format(BookFormat.AUDIOBOOK).totalCopies(3).availableCopies(3)
                .durationMinutes(720).narrator("Jan Kowalski").build();
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll: returns all books from repository")
    void getAll_returnsList() {
        when(bookRepository.findAll()).thenReturn(List.of(paperBook, ebook, audiobook));

        List<Book> result = bookService.getAll();

        assertThat(result).hasSize(3);
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById: returns book when found")
    void getById_found() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(paperBook));

        Book result = bookService.getById(1L);

        assertThat(result.getTitle()).isEqualTo("Pan Tadeusz");
    }

    @Test
    @DisplayName("getById: throws ResourceNotFoundException when not found")
    void getById_notFound_throws() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── create (Factory Method) ───────────────────────────────────────────────

    @Test
    @DisplayName("create: Factory Method creates PaperBook for PAPER format")
    void create_paperBook_factoryMethod() {
        BookRequest req = buildRequest("Pan Tadeusz 2", "978-83-000-0010-0", BookFormat.PAPER);
        req.setPages(300);
        req.setCategoryId(1L);
        req.setAuthorIds(Set.of(1L));

        when(bookRepository.existsByIsbn(req.getIsbn())).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.create(req);

        assertThat(result).isInstanceOf(PaperBook.class);
        assertThat(result.getTitle()).isEqualTo("Pan Tadeusz 2");
        assertThat(result.getAvailableCopies()).isEqualTo(result.getTotalCopies());
    }

    @Test
    @DisplayName("create: Factory Method creates Ebook for EBOOK format")
    void create_ebook_factoryMethod() {
        BookRequest req = buildRequest("E-Wiedźmin", "978-83-000-0011-0", BookFormat.EBOOK);
        req.setFileFormat("PDF");

        when(bookRepository.existsByIsbn(req.getIsbn())).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.create(req);

        assertThat(result).isInstanceOf(Ebook.class);
    }

    @Test
    @DisplayName("create: Factory Method creates Audiobook for AUDIOBOOK format")
    void create_audiobook_factoryMethod() {
        BookRequest req = buildRequest("Audio-Lalka", "978-83-000-0012-0", BookFormat.AUDIOBOOK);
        req.setDurationMinutes(600);
        req.setNarrator("Narrator X");

        when(bookRepository.existsByIsbn(req.getIsbn())).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.create(req);

        assertThat(result).isInstanceOf(Audiobook.class);
    }

    @Test
    @DisplayName("create: throws BusinessException when ISBN already exists")
    void create_duplicateIsbn_throws() {
        BookRequest req = buildRequest("Kopia", "978-83-000-0001-0", BookFormat.PAPER);
        when(bookRepository.existsByIsbn(req.getIsbn())).thenReturn(true);

        assertThatThrownBy(() -> bookService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("978-83-000-0001-0");
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: updates title and saves book")
    void update_success() {
        BookRequest req = buildRequest("Pan Tadeusz — nowe wyd.", "978-83-000-0001-0", BookFormat.PAPER);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(paperBook));
        when(bookRepository.save(paperBook)).thenReturn(paperBook);

        Book result = bookService.update(1L, req);

        assertThat(result.getTitle()).isEqualTo("Pan Tadeusz — nowe wyd.");
    }

    @Test
    @DisplayName("update: throws ResourceNotFoundException when book not found")
    void update_notFound_throws() {
        BookRequest req = buildRequest("X", "Y", BookFormat.PAPER);
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.update(99L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: deletes existing book")
    void delete_success() {
        when(bookRepository.existsById(1L)).thenReturn(true);

        bookService.delete(1L);

        verify(bookRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete: throws ResourceNotFoundException when book not found")
    void delete_notFound_throws() {
        when(bookRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> bookService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── search (Strategy Pattern) ─────────────────────────────────────────────

    @Test
    @DisplayName("search: strategy=title returns books matching title (case-insensitive)")
    void search_byTitle_found() {
        when(bookRepository.findAll()).thenReturn(List.of(paperBook, ebook, audiobook));

        List<Book> result = bookService.search("title", "tadeusz");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).containsIgnoringCase("Tadeusz");
    }

    @Test
    @DisplayName("search: strategy=author returns books matching author last name")
    void search_byAuthor_found() {
        paperBook.setAuthors(Set.of(author));
        when(bookRepository.findAll()).thenReturn(List.of(paperBook, ebook));

        List<Book> result = bookService.search("author", "Mickiewicz");

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("search: strategy=category returns books in given category")
    void search_byCategory_found() {
        when(bookRepository.findAll()).thenReturn(List.of(paperBook, ebook));

        List<Book> result = bookService.search("category", "Fantasy");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory().getName()).isEqualTo("Fantasy");
    }

    @Test
    @DisplayName("search: strategy=available returns only books with copies > 0")
    void search_byAvailable_returnsOnlyAvailable() {
        paperBook.setAvailableCopies(0);
        when(bookRepository.findAll()).thenReturn(List.of(paperBook, ebook));

        List<Book> result = bookService.search("available", "");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Wiedźmin");
    }

    @Test
    @DisplayName("search: unknown strategy throws BusinessException")
    void search_unknownStrategy_throws() {
        when(bookRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> bookService.search("unknown", "x"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("unknown");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private BookRequest buildRequest(String title, String isbn, BookFormat format) {
        BookRequest req = new BookRequest();
        req.setTitle(title);
        req.setIsbn(isbn);
        req.setFormat(format);
        req.setTotalCopies(1);
        return req;
    }
}
