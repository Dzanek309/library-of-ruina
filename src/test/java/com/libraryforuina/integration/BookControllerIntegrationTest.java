package com.libraryforuina.integration;

import com.libraryforuina.enums.BookFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class BookControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("library_test")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    // ObjectMapper tworzymy lokalnie — sluzy tylko do budowy tresci zadan JSON
    // i nie wymaga wstrzykniecia z kontekstu Springa.
    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- Przegladanie katalogu + uwierzytelnianie ---

    @Test
    @DisplayName("GET /api/books bez uwierzytelnienia -> 401 Unauthorized")
    void getBooksUnauthorized() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/books z rola USER -> 200 OK (pusty katalog to poprawna odpowiedz)")
    @WithMockUser(roles = {"USER"})
    void getBooksAsUser() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/books z rola ADMIN -> 200 OK")
    @WithMockUser(roles = {"ADMIN"})
    void getBooksAsAdmin() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk());
    }

    // --- Wyszukiwanie (wzorzec Strategy) ---

    @Test
    @DisplayName("GET /api/books/search?strategy=available z USER -> 200 OK")
    @WithMockUser(roles = {"USER"})
    void searchAvailableAsUser() throws Exception {
        mockMvc.perform(get("/api/books/search").param("strategy", "available"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/books/search?strategy=title z USER -> 200 OK")
    @WithMockUser(roles = {"USER"})
    void searchByTitleAsUser() throws Exception {
        mockMvc.perform(get("/api/books/search")
                        .param("strategy", "title")
                        .param("value", "java"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/books/search bez uwierzytelnienia -> 401 Unauthorized")
    void searchUnauthorized() throws Exception {
        mockMvc.perform(get("/api/books/search").param("strategy", "available"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/books/search z nieznana strategia -> 409 Conflict (BusinessException)")
    @WithMockUser(roles = {"USER"})
    void searchUnknownStrategy() throws Exception {
        mockMvc.perform(get("/api/books/search").param("strategy", "nieistniejaca"))
                .andExpect(status().isConflict());
    }


    @Test
    @DisplayName("DELETE /api/books/1 z rola USER -> 403 Forbidden (RBAC)")
    @WithMockUser(roles = {"USER"})
    void deleteBookForbiddenForUser() throws Exception {
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/books/1 bez uwierzytelnienia -> 401 Unauthorized")
    void deleteBookUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/books z rola USER (poprawne dane) -> 403 Forbidden (RBAC)")
    @WithMockUser(roles = {"USER"})
    void createBookForbiddenForUser() throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("title", "Testowa ksiazka");
        body.put("isbn", "978-RBAC-USER-01");
        body.put("format", BookFormat.PAPER.name());
        body.put("totalCopies", 1);
        body.put("pages", 120);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }


    @Test
    @DisplayName("ADMIN tworzy ksiazke -> 201, a nastepnie jest ona widoczna w wyszukiwaniu po tytule")
    @WithMockUser(roles = {"ADMIN"})
    void adminCreatesBookAndItIsSearchable() throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("title", "Wiedzmin Ostatnie zyczenie");
        body.put("isbn", "978-CREATE-FLOW-01");
        body.put("format", BookFormat.PAPER.name());
        body.put("totalCopies", 3);
        body.put("pages", 330);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.availableCopies").value(3));
        mockMvc.perform(get("/api/books/search")
                        .param("strategy", "title")
                        .param("value", "wiedzmin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Wiedzmin Ostatnie zyczenie"))
                .andExpect(jsonPath("$[0].formatInfo").exists());
    }
}
