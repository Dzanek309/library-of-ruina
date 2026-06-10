package com.libraryforuina.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("EBOOK")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Ebook extends Book {

    @Column(name = "file_format", length = 20)
    private String fileFormat;

    @Override
    public int getLoanPeriodDays() {
        return 30;
    }

    @Override
    public String getFormatInfo() {
        return "E-book" + (fileFormat != null ? " w formacie " + fileFormat : "")
                + " - dostep online, termin 30 dni.";
    }
}
