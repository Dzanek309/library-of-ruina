package com.libraryforuina.entity;

import com.libraryforuina.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @PrePersist
    public void prePersist() {
        if (reservedAt == null) {
            reservedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ReservationStatus.PENDING;
        }
    }
}
