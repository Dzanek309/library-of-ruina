package com.libraryforuina.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservationRequest {

    @NotNull(message = "Id uzytkownika jest wymagane")
    private Long userId;

    @NotNull(message = "Id ksiazki jest wymagane")
    private Long bookId;
}
