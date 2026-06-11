package com.libraryforuina.strategy;

import com.libraryforuina.entity.Book;
import java.util.List;

public class SearchByAuthor implements BookSearchStrategy {

    private final String authorPhrase;

    public SearchByAuthor(String authorPhrase) {
        this.authorPhrase = authorPhrase.toLowerCase();
    }

    @Override
    public List<Book> search(List<Book> books) {
        return books.stream()
                .filter(b -> b.getAuthors().stream().anyMatch(a ->
                        (a.getFirstName() + " " + a.getLastName())
                                .toLowerCase().contains(authorPhrase)))
                .toList();
    }

    @Override
    public String getStrategyName() {
        return "SEARCH_BY_AUTHOR";
    }
}
