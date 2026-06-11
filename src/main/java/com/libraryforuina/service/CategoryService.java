package com.libraryforuina.service;

import com.libraryforuina.dto.request.CategoryRequest;
import com.libraryforuina.entity.Category;
import com.libraryforuina.exception.BusinessException;
import com.libraryforuina.exception.ResourceNotFoundException;
import com.libraryforuina.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kategoria o id " + id + " nie istnieje"));
    }

    public Category create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessException("Kategoria '" + request.getName() + "' juz istnieje");
        }
        Category category = Category.builder().name(request.getName()).build();
        return categoryRepository.save(category);
    }

    public Category update(Long id, CategoryRequest request) {
        Category category = getById(id);
        category.setName(request.getName());
        return categoryRepository.save(category);
    }

    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Kategoria o id " + id + " nie istnieje");
        }
        categoryRepository.deleteById(id);
    }
}
