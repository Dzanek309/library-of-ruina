package com.libraryforuina.strategy;

import com.libraryforuina.entity.Book;
import java.util.List;

public class SearchByTitle implements BookSearchStrategy {

    private final String title;

    public SearchByTitle(String title) {
        this.title = title;
    }

    @Override
    public List<Book> search(List<Book> books) {
        return books.stream()
                .filter(b -> b.getTitle() != null
                        && b.getTitle().toLowerCase().contains(title.toLowerCase()))
                .toList();
    }

    @Override
    public String getStrategyName() {
        return "SEARCH_BY_TITLE";
    }
}
