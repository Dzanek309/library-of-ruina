package com.libraryforuina.strategy;

import com.libraryforuina.entity.Book;
import java.util.List;

public class SearchByAvailable implements BookSearchStrategy {

    @Override
    public List<Book> search(List<Book> books) {
        return books.stream()
                .filter(b -> b.getAvailableCopies() != null && b.getAvailableCopies() > 0)
                .toList();
    }

    @Override
    public String getStrategyName() {
        return "SEARCH_BY_AVAILABLE";
    }
}
