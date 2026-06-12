package com.libraryforuina.service;

import com.libraryforuina.dto.request.CategoryRequest;
import com.libraryforuina.entity.Category;
import com.libraryforuina.exception.BusinessException;
import com.libraryforuina.exception.ResourceNotFoundException;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService unit tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category fantasy;
    private CategoryRequest request;

    @BeforeEach
    void setUp() {
        fantasy = Category.builder().id(1L).name("Fantasy").build();

        request = new CategoryRequest();
        request.setName("Fantasy");
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll: returns list of all categories")
    void getAll_returnsList() {
        when(categoryRepository.findAll()).thenReturn(List.of(fantasy));

        List<Category> result = categoryService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Fantasy");
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById: returns category when found")
    void getById_found() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(fantasy));

        Category result = categoryService.getById(1L);

        assertThat(result.getName()).isEqualTo("Fantasy");
    }

    @Test
    @DisplayName("getById: throws ResourceNotFoundException when not found")
    void getById_notFound_throws() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: saves new category when name is unique")
    void create_success() {
        when(categoryRepository.existsByName("Fantasy")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(fantasy);

        Category result = categoryService.create(request);

        assertThat(result.getName()).isEqualTo("Fantasy");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("create: throws BusinessException when name already exists")
    void create_duplicateName_throws() {
        when(categoryRepository.existsByName("Fantasy")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Fantasy");
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update: updates name and saves category")
    void update_success() {
        request.setName("Sci-Fi");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(fantasy));
        when(categoryRepository.save(fantasy)).thenReturn(fantasy);

        Category result = categoryService.update(1L, request);

        assertThat(result.getName()).isEqualTo("Sci-Fi");
        verify(categoryRepository).save(fantasy);
    }

    @Test
    @DisplayName("update: throws ResourceNotFoundException when category not found")
    void update_notFound_throws() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: deletes existing category")
    void delete_success() {
        when(categoryRepository.existsById(1L)).thenReturn(true);

        categoryService.delete(1L);

        verify(categoryRepository).deleteById(1L);
    }

    @Test
    @DisplayName("delete: throws ResourceNotFoundException when category not found")
    void delete_notFound_throws() {
        when(categoryRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
