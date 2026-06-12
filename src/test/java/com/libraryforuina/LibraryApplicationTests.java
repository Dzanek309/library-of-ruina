package com.libraryforuina;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Full context requires a running PostgreSQL database. " +
          "Application context is verified in integration tests (Phase 7) using TestContainers.")
class LibraryApplicationTests {

    @Test
    void contextLoads() {
    }

}
