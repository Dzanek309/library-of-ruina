package com.libraryforuina.service;

import com.libraryforuina.dto.request.BookRequest;
import com.libraryforuina.entity.*;
import com.libraryforuina.exception.BusinessException;
import com.libraryforuina.exception.ResourceNotFoundException;
import com.libraryforuina.repository.AuthorRepository;
import com.libraryforuina.repository.BookRepository;
import com.libraryforuina.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final AuthorRepository authorRepository;

    public List<Book> getAll() {
        return bookRepository.findAll();
    }

    public Book getById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ksiazka o id " + id + " nie istnieje"));
    }

    /**
     * FACTORY METHOD + POLIMORFIZM: na podstawie pola format tworzymy
     * konkretna podklase Book. Kod kliencki nie musi znac konkretnego typu.
     */
    public Book create(BookRequest req) {
        if (bookRepository.existsByIsbn(req.getIsbn())) {
            throw new BusinessException("Ksiazka o ISBN " + req.getIsbn() + " juz istnieje");
        }

        Category category = resolveCategory(req.getCategoryId());
        Set<Author> authors = resolveAuthors(req.getAuthorIds());

        Book book = switch (req.getFormat()) {
            case PAPER -> PaperBook.builder()
                    .pages(req.getPages())
                    .build();
            case EBOOK -> Ebook.builder()
                    .fileFormat(req.getFileFormat())
                    .build();
            case AUDIOBOOK -> Audiobook.builder()
                    .durationMinutes(req.getDurationMinutes())
                    .narrator(req.getNarrator())
                    .build();
        };

        // pola wspolne
        book.setTitle(req.getTitle());
        book.setIsbn(req.getIsbn());
        book.setPublicationYear(req.getPublicationYear());
        book.setFormat(req.getFormat());
        book.setTotalCopies(req.getTotalCopies());
        book.setAvailableCopies(req.getTotalCopies()); // na starcie wszystkie dostepne
        book.setDescription(req.getDescription());
        book.setCategory(category);
        book.setAuthors(authors);

        return bookRepository.save(book);
    }

    public Book update(Long id, BookRequest req) {
        Book book = getById(id);
        book.setTitle(req.getTitle());
        book.setPublicationYear(req.getPublicationYear());
        book.setDescription(req.getDescription());
        book.setCategory(resolveCategory(req.getCategoryId()));
        book.setAuthors(resolveAuthors(req.getAuthorIds()));
        return bookRepository.save(book);
    }

    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Ksiazka o id " + id + " nie istnieje");
        }
        bookRepository.deleteById(id);
    }

    // --- metody pomocnicze ---

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Kategoria o id " + categoryId + " nie istnieje"));
    }

    private Set<Author> resolveAuthors(Set<Long> authorIds) {
        Set<Author> authors = new HashSet<>();
        if (authorIds == null) return authors;
        for (Long aid : authorIds) {
            authors.add(authorRepository.findById(aid)
                    .orElseThrow(() -> new ResourceNotFoundException("Autor o id " + aid + " nie istnieje")));
        }
        return authors;
    }
}
