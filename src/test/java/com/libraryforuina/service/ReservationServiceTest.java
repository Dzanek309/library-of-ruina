package com.libraryforuina.service;

import com.libraryforuina.dto.request.ReservationRequest;
import com.libraryforuina.entity.Book;
import com.libraryforuina.entity.PaperBook;
import com.libraryforuina.entity.Reservation;
import com.libraryforuina.entity.User;
import com.libraryforuina.enums.BookFormat;
import com.libraryforuina.enums.ReservationStatus;
import com.libraryforuina.enums.Role;
import com.libraryforuina.exception.BusinessException;
import com.libraryforuina.exception.ResourceNotFoundException;
import com.libraryforuina.repository.BookRepository;
import com.libraryforuina.repository.ReservationRepository;
import com.libraryforuina.repository.UserRepository;
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
@DisplayName("ReservationService unit tests")
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private ReservationService reservationService;

    private User user;
    private Book book;
    private Reservation reservation;
    private ReservationRequest request;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("jan")
                .email("jan@example.com")
                .password("hashed")
                .role(Role.USER)
                .build();

        book = PaperBook.builder()
                .id(10L)
                .title("Pan Tadeusz")
                .isbn("978-83-000-0001-0")
                .format(BookFormat.PAPER)
                .totalCopies(3)
                .availableCopies(2)
                .build();

        reservation = Reservation.builder()
                .id(100L)
                .user(user)
                .book(book)
                .status(ReservationStatus.PENDING)
                .build();

        request = new ReservationRequest();
        request.setUserId(1L);
        request.setBookId(10L);
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll: returns list from repository")
    void getAll_returnsList() {
        when(reservationRepository.findAll()).thenReturn(List.of(reservation));

        List<Reservation> result = reservationService.getAll();

        assertThat(result).hasSize(1);
    }

    // ── getByUser ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByUser: returns reservations for given user")
    void getByUser_returnsUserReservations() {
        when(reservationRepository.findByUserId(1L)).thenReturn(List.of(reservation));

        List<Reservation> result = reservationService.getByUser(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getId()).isEqualTo(1L);
    }

    // ── reserve ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("reserve: creates reservation with PENDING status when book available")
    void reserve_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        Reservation result = reservationService.reserve(request);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(result.getBook().getTitle()).isEqualTo("Pan Tadeusz");
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("reserve: throws BusinessException when no copies available")
    void reserve_noAvailableCopies_throws() {
        book.setAvailableCopies(0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> reservationService.reserve(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Pan Tadeusz");
    }

    @Test
    @DisplayName("reserve: throws ResourceNotFoundException when user not found")
    void reserve_userNotFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.reserve(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("reserve: throws ResourceNotFoundException when book not found")
    void reserve_bookNotFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.reserve(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancel: sets status to CANCELLED for PENDING reservation")
    void cancel_success() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        Reservation result = reservationService.cancel(100L);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancel: throws BusinessException when already cancelled")
    void cancel_alreadyCancelled_throws() {
        reservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancel(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("anulowana");
    }

    @Test
    @DisplayName("cancel: throws BusinessException when reservation is COMPLETED")
    void cancel_completed_throws() {
        reservation.setStatus(ReservationStatus.COMPLETED);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancel(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zakonczonej");
    }

    @Test
    @DisplayName("cancel: throws ResourceNotFoundException when reservation not found")
    void cancel_notFound_throws() {
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.cancel(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
