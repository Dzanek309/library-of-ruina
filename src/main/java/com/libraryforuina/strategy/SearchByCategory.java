package com.libraryforuina.strategy;

import com.libraryforuina.entity.Book;
import java.util.List;

public class SearchByCategory implements BookSearchStrategy {

    private final String categoryName;

    public SearchByCategory(String categoryName) {
        this.categoryName = categoryName;
    }

    @Override
    public List<Book> search(List<Book> books) {
        return books.stream()
                .filter(b -> b.getCategory() != null
                        && b.getCategory().getName().equalsIgnoreCase(categoryName))
                .toList();
    }

    @Override
    public String getStrategyName() {
        return "SEARCH_BY_CATEGORY";
    }
}
