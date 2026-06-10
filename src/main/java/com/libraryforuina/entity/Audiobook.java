package com.libraryforuina.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("AUDIOBOOK")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Audiobook extends Book {

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "narrator", length = 150)
    private String narrator;

    @Override
    public int getLoanPeriodDays() {
        return 21;
    }

    @Override
    public String getFormatInfo() {
        return "Audiobook"
                + (durationMinutes != null ? " (" + durationMinutes + " min)" : "")
                + (narrator != null ? ", czyta: " + narrator : "")
                + " - odsluch online, termin 21 dni.";
    }
}
