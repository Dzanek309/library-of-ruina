package com.libraryforuina.service;

import com.libraryforuina.dto.request.LoanRequest;
import com.libraryforuina.entity.Book;
import com.libraryforuina.entity.Ebook;
import com.libraryforuina.entity.Loan;
import com.libraryforuina.entity.PaperBook;
import com.libraryforuina.entity.User;
import com.libraryforuina.enums.BookFormat;
import com.libraryforuina.enums.LoanStatus;
import com.libraryforuina.enums.Role;
import com.libraryforuina.exception.BusinessException;
import com.libraryforuina.exception.ResourceNotFoundException;
import com.libraryforuina.repository.BookRepository;
import com.libraryforuina.repository.LoanRepository;
import com.libraryforuina.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService unit tests")
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private LoanService loanService;

    private User user;
    private PaperBook paperBook;
    private Ebook ebook;
    private Loan activeLoan;
    private LoanRequest request;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("jan")
                .email("jan@example.com")
                .password("hashed")
                .role(Role.USER)
                .build();

        paperBook = PaperBook.builder()
                .id(10L)
                .title("Pan Tadeusz")
                .isbn("978-83-000-0001-0")
                .format(BookFormat.PAPER)
                .totalCopies(3)
                .availableCopies(2)
                .build();

        ebook = Ebook.builder()
                .id(20L)
                .title("Wiedźmin")
                .isbn("978-83-000-0002-0")
                .format(BookFormat.EBOOK)
                .totalCopies(10)
                .availableCopies(10)
                .build();

        activeLoan = Loan.builder()
                .id(100L)
                .user(user)
                .book(paperBook)
                .borrowedAt(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(14))
                .status(LoanStatus.BORROWED)
                .build();

        request = new LoanRequest();
        request.setUserId(1L);
        request.setBookId(10L);
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAll: returns all loans from repository")
    void getAll_returnsList() {
        when(loanRepository.findAll()).thenReturn(List.of(activeLoan));

        List<Loan> result = loanService.getAll();

        assertThat(result).hasSize(1);
    }

    // ── getHistoryByUser ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getHistoryByUser: returns loans for given user")
    void getHistoryByUser_returnsLoans() {
        when(loanRepository.findByUserId(1L)).thenReturn(List.of(activeLoan));

        List<Loan> result = loanService.getHistoryByUser(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getId()).isEqualTo(1L);
    }

    // ── borrow ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("borrow: creates loan with BORROWED status and decrements available copies")
    void borrow_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(paperBook));
        when(loanRepository.save(any(Loan.class))).thenReturn(activeLoan);

        Loan result = loanService.borrow(request);

        assertThat(result.getStatus()).isEqualTo(LoanStatus.BORROWED);
        // available copies decremented from 2 to 1
        assertThat(paperBook.getAvailableCopies()).isEqualTo(1);
        verify(bookRepository).save(paperBook);
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    @DisplayName("borrow: due date is calculated polymorphically (14 days for PaperBook)")
    void borrow_dueDatePolymorphic_paperBook() {
        LocalDateTime before = LocalDateTime.now();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(paperBook));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        Loan result = loanService.borrow(request);

        assertThat(result.getDueDate()).isAfterOrEqualTo(before.plusDays(14));
        assertThat(result.getDueDate()).isBefore(before.plusDays(15));
    }

    @Test
    @DisplayName("borrow: due date is calculated polymorphically (7 days for Ebook)")
    void borrow_dueDatePolymorphic_ebook() {
        request.setBookId(20L);
        LocalDateTime before = LocalDateTime.now();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(20L)).thenReturn(Optional.of(ebook));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        Loan result = loanService.borrow(request);

        assertThat(result.getDueDate()).isAfterOrEqualTo(before.plusDays(ebook.getLoanPeriodDays()));
    }

    @Test
    @DisplayName("borrow: throws BusinessException when no copies available")
    void borrow_noAvailableCopies_throws() {
        paperBook.setAvailableCopies(0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(paperBook));

        assertThatThrownBy(() -> loanService.borrow(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Pan Tadeusz");
    }

    @Test
    @DisplayName("borrow: throws ResourceNotFoundException when user not found")
    void borrow_userNotFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.borrow(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("borrow: throws ResourceNotFoundException when book not found")
    void borrow_bookNotFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.borrow(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── returnBook ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("returnBook: sets status RETURNED, sets returnedAt and increments available copies")
    void returnBook_success() {
        when(loanRepository.findById(100L)).thenReturn(Optional.of(activeLoan));
        when(loanRepository.save(activeLoan)).thenReturn(activeLoan);

        Loan result = loanService.returnBook(100L);

        assertThat(result.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(result.getReturnedAt()).isNotNull();
        // available copies incremented from 2 to 3
        assertThat(paperBook.getAvailableCopies()).isEqualTo(3);
        verify(bookRepository).save(paperBook);
    }

    @Test
    @DisplayName("returnBook: throws BusinessException when book already returned")
    void returnBook_alreadyReturned_throws() {
        activeLoan.setStatus(LoanStatus.RETURNED);
        when(loanRepository.findById(100L)).thenReturn(Optional.of(activeLoan));

        assertThatThrownBy(() -> loanService.returnBook(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("zwrocona");
    }

    @Test
    @DisplayName("returnBook: throws ResourceNotFoundException when loan not found")
    void returnBook_notFound_throws() {
        when(loanRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.returnBook(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
