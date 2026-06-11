package com.libraryforuina.repository;

import com.libraryforuina.entity.Loan;
import com.libraryforuina.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUserId(Long userId);
    List<Loan> findByBookId(Long bookId);
    List<Loan> findByStatus(LoanStatus status);
    Optional<Loan> findByUserIdAndBookIdAndStatus(Long userId, Long bookId, LoanStatus status);
}
