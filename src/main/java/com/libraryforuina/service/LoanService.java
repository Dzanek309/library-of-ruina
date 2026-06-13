package com.libraryforuina.service;

import com.libraryforuina.dto.request.LoanRequest;
import com.libraryforuina.entity.Book;
import com.libraryforuina.entity.Loan;
import com.libraryforuina.entity.User;
import com.libraryforuina.enums.LoanStatus;
import com.libraryforuina.exception.BusinessException;
import com.libraryforuina.exception.ResourceNotFoundException;
import com.libraryforuina.repository.BookRepository;
import com.libraryforuina.repository.LoanRepository;
import com.libraryforuina.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public List<Loan> getAll() {
        return loanRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Loan> getHistoryByUser(Long userId) {
        return loanRepository.findByUserId(userId);
    }

    public Loan borrow(LoanRequest req) {
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Uzytkownik o id " + req.getUserId() + " nie istnieje"));
        // Blokada wiersza (FOR UPDATE) — sprawdzenie i zmniejszenie licznika musza
        // byc atomowe wzgledem innych wypozyczen/zwrotow tej samej ksiazki.
        Book book = bookRepository.findByIdForUpdate(req.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ksiazka o id " + req.getBookId() + " nie istnieje"));

        if (book.getAvailableCopies() <= 0) {
            throw new BusinessException("Brak dostepnych egzemplarzy ksiazki: " + book.getTitle());
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime due = now.plusDays(book.getLoanPeriodDays());

        Loan loan = Loan.builder()
                .user(user)
                .book(book)
                .borrowedAt(now)
                .dueDate(due)
                .status(LoanStatus.BORROWED)
                .build();

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        return loanRepository.save(loan);
    }

    public Loan returnBook(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wypozyczenie o id " + loanId + " nie istnieje"));

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException("Ksiazka zostala juz zwrocona");
        }

        loan.setReturnedAt(LocalDateTime.now());
        loan.setStatus(LoanStatus.RETURNED);

        // Blokada wiersza (FOR UPDATE) - tak samo jak przy wypozyczeniu. Bez tego
        // rownolegly zwrot/wypozyczenie tej samej ksiazki czytaloby nieaktualny
        // licznik (lost update). Pobieramy swiezy wiersz zamiast loan.getBook().
        Book book = bookRepository.findByIdForUpdate(loan.getBook().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ksiazka o id " + loan.getBook().getId() + " nie istnieje"));
        // Zwrot zwieksza pule dostepnych, ale NIGDY ponad calkowita liczbe
        // egzemplarzy (total) - inaczej blad/podwojny zwrot rozjechalby licznik.
        int next = book.getAvailableCopies() + 1;
        Integer total = book.getTotalCopies();
        if (total != null && next > total) {
            next = total;
        }
        book.setAvailableCopies(next);
        bookRepository.save(book);

        return loanRepository.save(loan);
    }
}
