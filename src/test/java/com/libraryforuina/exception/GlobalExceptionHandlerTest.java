package com.libraryforuina.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler unit tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleNotFound: maps ResourceNotFoundException to 404 with message")
    void handleNotFound_returns404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleNotFound(new ResourceNotFoundException("Ksiazka 7 nie istnieje"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .containsEntry("status", 404)
                .containsEntry("error", "Not Found")
                .containsEntry("message", "Ksiazka 7 nie istnieje")
                .containsKey("timestamp");
    }

    @Test
    @DisplayName("handleBusiness: maps BusinessException to 409 with message")
    void handleBusiness_returns409() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleBusiness(new BusinessException("Brak dostepnych egzemplarzy"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .containsEntry("status", 409)
                .containsEntry("error", "Conflict")
                .containsEntry("message", "Brak dostepnych egzemplarzy");
    }

    @Test
    @DisplayName("handleValidation: returns 400 with first field error")
    void handleValidation_returnsFirstFieldError() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "bookRequest");
        bindingResult.addError(new FieldError("bookRequest", "title", "nie moze byc puste"));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("status", 400)
                .containsEntry("message", "title: nie moze byc puste");
    }

    @Test
    @DisplayName("handleValidation: falls back to generic message when no field errors")
    void handleValidation_noFieldErrors_fallback() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "bookRequest");
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Blad walidacji");
    }

    @Test
    @DisplayName("handleAccessDenied: returns 403 with fixed message")
    void handleAccessDenied_returns403() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody())
                .containsEntry("status", 403)
                .containsEntry("message", "Brak uprawnien do wykonania tej operacji");
    }

    @Test
    @DisplayName("handleGeneral: returns 500 with fixed message")
    void handleGeneral_returns500() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneral(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .containsEntry("status", 500)
                .containsEntry("error", "Internal Server Error")
                .containsEntry("message", "Wewnetrzny blad serwera");
    }
}
