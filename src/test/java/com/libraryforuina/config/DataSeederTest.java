package com.libraryforuina.config;

import com.libraryforuina.dto.request.BookRequest;
import com.libraryforuina.entity.Author;
import com.libraryforuina.entity.Category;
import com.libraryforuina.entity.User;
import com.libraryforuina.enums.Role;
import com.libraryforuina.repository.AuthorRepository;
import com.libraryforuina.repository.BookRepository;
import com.libraryforuina.repository.CategoryRepository;
import com.libraryforuina.repository.UserRepository;
import com.libraryforuina.service.BookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataSeeder unit tests")
class DataSeederTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookService bookService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    @DisplayName("run: seeds admin and full catalog on empty database")
    void run_emptyDatabase_seedsEverything() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("hashed");
        when(bookRepository.count()).thenReturn(0L);
        // save() nadaje ID — seedCatalog czyta getId() i pakuje je do Set.of(...),
        // ktore odrzuca null, wiec encje musza wracac z ustawionym identyfikatorem.
        AtomicLong ids = new AtomicLong(1);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(ids.getAndIncrement());
            return c;
        });
        when(authorRepository.save(any(Author.class))).thenAnswer(inv -> {
            Author a = inv.getArgument(0);
            a.setId(ids.getAndIncrement());
            return a;
        });

        dataSeeder.run();

        // admin zapisany z zakodowanym haslem i rola ADMIN
        ArgumentCaptor<User> adminCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(adminCaptor.capture());
        assertThat(adminCaptor.getValue().getUsername()).isEqualTo("admin");
        assertThat(adminCaptor.getValue().getPassword()).isEqualTo("hashed");
        assertThat(adminCaptor.getValue().getRole()).isEqualTo(Role.ADMIN);

        // katalog: 3 kategorie, 4 autorow, 5 ksiazek
        verify(categoryRepository, times(3)).save(any(Category.class));
        verify(authorRepository, times(4)).save(any(Author.class));
        verify(bookService, times(5)).create(any(BookRequest.class));
    }

    @Test
    @DisplayName("run: idempotent — skips seeding when admin and books already exist")
    void run_existingData_skipsSeeding() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);
        when(bookRepository.count()).thenReturn(5L);

        dataSeeder.run();

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
        verify(categoryRepository, never()).save(any());
        verify(authorRepository, never()).save(any());
        verify(bookService, never()).create(any());
    }
}
