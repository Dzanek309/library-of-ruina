package com.libraryforuina.service;

import com.libraryforuina.dto.request.AuthorRequest;
import com.libraryforuina.entity.Author;
import com.libraryforuina.exception.ResourceNotFoundException;
import com.libraryforuina.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository authorRepository;

    public List<Author> getAll() {
        return authorRepository.findAll();
    }

    public Author getById(Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Autor o id " + id + " nie istnieje"));
    }

    public Author create(AuthorRequest request) {
        Author author = Author.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .bio(request.getBio())
                .build();
        return authorRepository.save(author);
    }

    public Author update(Long id, AuthorRequest request) {
        Author author = getById(id);
        author.setFirstName(request.getFirstName());
        author.setLastName(request.getLastName());
        author.setBio(request.getBio());
        return authorRepository.save(author);
    }

    public void delete(Long id) {
        if (!authorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Autor o id " + id + " nie istnieje");
        }
        authorRepository.deleteById(id);
    }
}
