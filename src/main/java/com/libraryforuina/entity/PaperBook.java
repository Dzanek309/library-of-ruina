package com.libraryforuina.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("PAPER")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class PaperBook extends Book {

    @Column(name = "pages")
    private Integer pages;

    @Override
    public int getLoanPeriodDays() {
        return 14;
    }

    @Override
    public String getFormatInfo() {
        return "Ksiazka papierowa" + (pages != null ? " (" + pages + " stron)" : "")
                + " - do odbioru i zwrotu w wypozyczalni, termin 14 dni.";
    }
}
