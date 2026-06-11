package com.libraryforuina.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthorRequest {
    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String bio;
}
