package com.libraryforuina.strategy;

import com.libraryforuina.entity.Book;
import java.util.List;

public interface BookSearchStrategy {
    List<Book> search(List<Book> books);
    String getStrategyName();
}
