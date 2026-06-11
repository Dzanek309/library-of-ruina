package com.libraryforuina.service;

import com.libraryforuina.dto.request.ReservationRequest;
import com.libraryforuina.entity.Book;
import com.libraryforuina.entity.Reservation;
import com.libraryforuina.entity.User;
import com.libraryforuina.enums.ReservationStatus;
import com.libraryforuina.exception.BusinessException;
import com.libraryforuina.exception.ResourceNotFoundException;
import com.libraryforuina.repository.BookRepository;
import com.libraryforuina.repository.ReservationRepository;
import com.libraryforuina.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public List<Reservation> getAll() {
        return reservationRepository.findAll();
    }

    public List<Reservation> getByUser(Long userId) {
        return reservationRepository.findByUserId(userId);
    }

    public Reservation reserve(ReservationRequest req) {
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Uzytkownik o id " + req.getUserId() + " nie istnieje"));
        Book book = bookRepository.findById(req.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ksiazka o id " + req.getBookId() + " nie istnieje"));

        if (book.getAvailableCopies() <= 0) {
            throw new BusinessException("Brak dostepnych egzemplarzy ksiazki: " + book.getTitle());
        }

        Reservation reservation = Reservation.builder()
                .user(user)
                .book(book)
                .status(ReservationStatus.PENDING)
                .build();

        return reservationRepository.save(reservation);
    }

    public Reservation cancel(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rezerwacja o id " + id + " nie istnieje"));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("Rezerwacja jest juz anulowana");
        }
        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new BusinessException("Nie mozna anulowac zakonczonej rezerwacji");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservationRepository.save(reservation);
    }
}
