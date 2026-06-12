package com.libraryforuina.service;

import com.libraryforuina.dto.request.AuthorRequest;
import com.libraryforuina.entity.Author;
import com.libraryforuina.exception.ResourceNotFoundException;
import com.libraryforuina.repository.AuthorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorService unit tests")
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private AuthorService authorService;

    private Author sampleAuthor;
    private AuthorRequest request;

    @BeforeEach
    void setUp() {
        sampleAuthor = Author.builder()
                .id(1L)
                .firstName("Adam")
                .lastName("Mickiewicz")
                .bio("Wieszcz narodowy")
                .build();

        request = new AuthorRequest();
        request.setFirstName("Adam");
        request.setLastName("Mickiewicz");
        request.setBio("Wieszcz narodowy");
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll: returns list of all authors")
    void getAll_returnsList() {
        when(authorRepository.findAll()).thenReturn(List.of(sampleAuthor));

        List<Author> result = authorService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLastName()).isEqualTo("Mickiewicz");
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById: returns author when found")
    void getById_found() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(sampleAuthor));

        Author result = authorService.getById(1L);

        assertThat(result.getFirstName()).isEqualTo("Adam");
    }

    @Test
    @DisplayName("getById: throws ResourceNotFoundException when not found")
    void getById_notFound_throws() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: saves and returns new author")
    void create_success() {
        when(authorRepository.save(any(Author.class))).thenReturn(sampleAuthor);

        Author result = authorService.create(request);

        assertThat(result.getFirstName()).isEqualTo("Adam");
        assertThat(result.getLastName()).isEqualTo("Mickiewicz");
        verify(authorRepository).save(any(Author.class));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: updates fields and saves author")
    void update_success() {
        request.setFirstName("Juliusz");
        request.setLastName("Slowacki");
        when(authorRepository.findById(1L)).thenReturn(Optional.of(sampleAuthor));
        when(authorRepository.save(sampleAuthor)).thenReturn(sampleAuthor);

        Author result = authorService.update(1L, request);

        assertThat(result.getFirstName()).isEqualTo("Juliusz");
        assertThat(result.getLastName()).isEqualTo("Slowacki");
        verify(authorRepository).save(sampleAuthor);
    }

    @Test
    @DisplayName("update: throws ResourceNotFoundException when author not found")
    void update_notFound_throws() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: deletes existing author")
    void delete_success() {
        when(authorRepository.existsById(1L)).thenReturn(true);

        authorService.delete(1L);

        verify(authorRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete: throws ResourceNotFoundException when author not found")
    void delete_notFound_throws() {
        when(authorRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> authorService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
