package com.libraryforuina.config;

import com.libraryforuina.dto.request.BookRequest;
import com.libraryforuina.entity.Author;
import com.libraryforuina.entity.Category;
import com.libraryforuina.entity.User;
import com.libraryforuina.enums.BookFormat;
import com.libraryforuina.enums.Role;
import com.libraryforuina.repository.AuthorRepository;
import com.libraryforuina.repository.BookRepository;
import com.libraryforuina.repository.CategoryRepository;
import com.libraryforuina.repository.UserRepository;
import com.libraryforuina.service.BookService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final BookService bookService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdmin();
        seedCatalog();
    }

    /** Konto Archiwisty (ADMIN), aby panel zarzadzania byl dostepny od razu. */
    private void seedAdmin() {
        if (userRepository.existsByUsername("admin")) {
            return;
        }
        User admin = User.builder()
                .username("admin")
                .email("admin@library.test")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);
        log.info("Seed: utworzono konto ADMIN (login: admin / haslo: admin123)");
    }

    /** Przykladowy katalog: 3 kategorie, 4 autorow, 5 ksiazek w 3 formatach. */
    private void seedCatalog() {
        if (bookRepository.count() > 0) {
            return;
        }

        Category fantasy = categoryRepository.save(Category.builder().name("Fantasy").build());
        Category scifi   = categoryRepository.save(Category.builder().name("Science Fiction").build());
        categoryRepository.save(Category.builder().name("Kryminal").build());

        Author sapkowski = authorRepository.save(author("Andrzej", "Sapkowski", "Polski pisarz fantasy, tworca sagi o wiedzminie."));
        Author lem       = authorRepository.save(author("Stanislaw", "Lem", "Polski pisarz science fiction i futurolog."));
        Author herbert   = authorRepository.save(author("Frank", "Herbert", "Amerykanski pisarz, autor cyklu Diuna."));
        Author tolkien   = authorRepository.save(author("J.R.R.", "Tolkien", "Brytyjski pisarz, tworca Srodziemia."));

        bookService.create(paper("Ostatnie zyczenie", "978-83-7469-001-1", 1993, 3, 330,
                fantasy.getId(), Set.of(sapkowski.getId()),
                "Pierwszy zbior opowiadan o wiedzminie Geralcie z Rivii."));
        bookService.create(paper("Druzyna Pierscienia", "978-83-7469-002-2", 1954, 4, 480,
                fantasy.getId(), Set.of(tolkien.getId()),
                "Pierwszy tom epickiej trylogii Wladca Pierscieni."));
        bookService.create(ebook("Solaris", "978-83-7469-003-3", 1961, 5, "EPUB",
                scifi.getId(), Set.of(lem.getId()),
                "Filozoficzna powiesc SF o kontakcie z obca inteligencja."));
        bookService.create(ebook("Diuna", "978-83-7469-004-4", 1965, 5, "PDF",
                scifi.getId(), Set.of(herbert.getId()),
                "Klasyka science fiction osadzona na pustynnej planecie Arrakis."));
        bookService.create(audiobook("Cyberiada", "978-83-7469-005-5", 1965, 2, 540, "Jan Kowalski",
                scifi.getId(), Set.of(lem.getId()),
                "Zbior groteskowych opowiadan Lema w wersji audio."));

        log.info("Seed: dodano przykladowy katalog ({} ksiazek)", bookRepository.count());
    }

    // --- buildery danych (dla czytelnosci powyzszych wywolan) ---

    private Author author(String firstName, String lastName, String bio) {
        return Author.builder().firstName(firstName).lastName(lastName).bio(bio).build();
    }

    private BookRequest base(String title, String isbn, Integer year, BookFormat format,
                             int copies, Long categoryId, Set<Long> authorIds, String desc) {
        BookRequest r = new BookRequest();
        r.setTitle(title);
        r.setIsbn(isbn);
        r.setPublicationYear(year);
        r.setFormat(format);
        r.setTotalCopies(copies);
        r.setCategoryId(categoryId);
        r.setAuthorIds(authorIds);
        r.setDescription(desc);
        return r;
    }

    private BookRequest paper(String title, String isbn, Integer year, int copies, int pages,
                              Long categoryId, Set<Long> authorIds, String desc) {
        BookRequest r = base(title, isbn, year, BookFormat.PAPER, copies, categoryId, authorIds, desc);
        r.setPages(pages);
        return r;
    }

    private BookRequest ebook(String title, String isbn, Integer year, int copies, String fileFormat,
                              Long categoryId, Set<Long> authorIds, String desc) {
        BookRequest r = base(title, isbn, year, BookFormat.EBOOK, copies, categoryId, authorIds, desc);
        r.setFileFormat(fileFormat);
        return r;
    }

    private BookRequest audiobook(String title, String isbn, Integer year, int copies,
                                  int durationMinutes, String narrator,
                                  Long categoryId, Set<Long> authorIds, String desc) {
        BookRequest r = base(title, isbn, year, BookFormat.AUDIOBOOK, copies, categoryId, authorIds, desc);
        r.setDurationMinutes(durationMinutes);
        r.setNarrator(narrator);
        return r;
    }
}
