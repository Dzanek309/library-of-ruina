package com.libraryforuina.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.libraryforuina.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class LendingFlowIntegrationTest {

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

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- pomocnicze: rejestracja, login, naglowek z tokenem ---

    private void register(String username, String email, String password) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("username", username);
        body.put("email", email);
        body.put("password", password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isOk());
    }

    /** Loguje uzytkownika i zwraca wezel JSON odpowiedzi (token, id, role...). */
    private JsonNode login(String username, String password) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("username", username);
        body.put("password", password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static String bearer(JsonNode loginResponse) {
        return "Bearer " + loginResponse.get("token").asText();
    }

    private long idFrom(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @DisplayName("Pelny przeplyw: ADMIN tworzy katalog, USER rezerwuje, wypozycza i zwraca")
    void fullLendingFlow() throws Exception {
        // 1. Rejestracja dwoch kont (oba startuja jako USER)
        register("librarian", "librarian@library.test", "password123");
        register("reader", "reader@library.test", "password123");

        // 2. Awans bibliotekarza na ADMIN (pokrywa UserService.promoteToAdmin)
        long librarianId = userService.findByUsername("librarian").getId();
        userService.promoteToAdmin(librarianId);

        // 3. Logowanie -> prawdziwe tokeny JWT (pokrywa AuthController.login,
        //    JwtResponse, JwtTokenProvider.generateToken, UserDetailsServiceImpl)
        JsonNode adminLogin = login("librarian", "password123");
        JsonNode userLogin = login("reader", "password123");
        String adminAuth = bearer(adminLogin);
        String userAuth = bearer(userLogin);
        long readerId = userLogin.get("id").asLong();

        // --- ADMIN buduje katalog (token JWT przechodzi przez filtr + RBAC) ---

        // 4. Kategoria
        ObjectNode category = objectMapper.createObjectNode();
        category.put("name", "Fantasy");
        long categoryId = idFrom(mockMvc.perform(post("/api/categories")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(category.toString()))
                .andExpect(status().isCreated())
                .andReturn());

        // 5. Autor
        ObjectNode author = objectMapper.createObjectNode();
        author.put("firstName", "Andrzej");
        author.put("lastName", "Sapkowski");
        long authorId = idFrom(mockMvc.perform(post("/api/authors")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(author.toString()))
                .andExpect(status().isCreated())
                .andReturn());

        // 6. Ksiazka papierowa (Factory Method -> PaperBook), z kategoria i autorem
        ObjectNode book = objectMapper.createObjectNode();
        book.put("title", "Ostatnie zyczenie");
        book.put("isbn", "978-83-LENDING-01");
        book.put("format", "PAPER");
        book.put("totalCopies", 2);
        book.put("pages", 333);
        book.put("categoryId", categoryId);
        ArrayNode authorIds = book.putArray("authorIds");
        authorIds.add(authorId);
        long bookId = idFrom(mockMvc.perform(post("/api/books")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(book.toString()))
                .andExpect(status().isCreated())
                // 2 egzemplarze wprowadzone -> 2 dostepne na starcie
                .andExpect(jsonPath("$.availableCopies").value(2))
                .andExpect(jsonPath("$.totalCopies").value(2))
                .andReturn());

        // format-info demonstruje polimorfizm
        mockMvc.perform(get("/api/books/" + bookId + "/format-info").header("Authorization", userAuth))
                .andExpect(status().isOk());

        // --- USER: rezerwacja, wypozyczenie, zwrot ---

        // 7. Rezerwacja
        ObjectNode reservation = objectMapper.createObjectNode();
        reservation.put("userId", readerId);
        reservation.put("bookId", bookId);
        long reservationId = idFrom(mockMvc.perform(post("/api/reservations")
                        .header("Authorization", userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservation.toString()))
                .andExpect(status().isCreated())
                .andReturn());

        // 8. Wypozyczenie (termin zwrotu liczony polimorficznie -> 14 dni dla PAPER)
        ObjectNode loan = objectMapper.createObjectNode();
        loan.put("userId", readerId);
        loan.put("bookId", bookId);
        MvcResult loanResult = mockMvc.perform(post("/api/loans")
                        .header("Authorization", userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loan.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BORROWED"))
                .andExpect(jsonPath("$.borrowedAt").isNotEmpty())
                .andExpect(jsonPath("$.dueDate").isNotEmpty())
                .andReturn();
        long loanId = idFrom(loanResult);

        // Termin PAPER = wypozyczenie + 14 dni (polimorficzny loanPeriodDays)
        JsonNode loanJson = objectMapper.readTree(loanResult.getResponse().getContentAsString());
        java.time.LocalDate borrowedDay = java.time.LocalDateTime
                .parse(loanJson.get("borrowedAt").asText()).toLocalDate();
        java.time.LocalDate dueDay = java.time.LocalDateTime
                .parse(loanJson.get("dueDate").asText()).toLocalDate();
        org.junit.jupiter.api.Assertions.assertEquals(14,
                java.time.temporal.ChronoUnit.DAYS.between(borrowedDay, dueDay),
                "Ksiazka papierowa powinna miec termin zwrotu 14 dni");

        // Wypozyczenie zmniejszylo licznik dostepnych: 2 -> 1
        mockMvc.perform(get("/api/books/" + bookId).header("Authorization", userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(1));

        // 9. Historia wypozyczen uzytkownika
        mockMvc.perform(get("/api/loans/user/" + readerId).header("Authorization", userAuth))
                .andExpect(status().isOk());

        // 10. Rezerwacje uzytkownika
        mockMvc.perform(get("/api/reservations/user/" + readerId).header("Authorization", userAuth))
                .andExpect(status().isOk());

        // 11. Zwrot ksiazki -> status RETURNED, licznik wraca do 2
        mockMvc.perform(patch("/api/loans/" + loanId + "/return").header("Authorization", userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnedAt").isNotEmpty());

        mockMvc.perform(get("/api/books/" + bookId).header("Authorization", userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(2));

        // Powtorny zwrot tego samego wypozyczenia jest odrzucany (idempotencja),
        // a licznik NIE przekracza calkowitej liczby egzemplarzy (cap na total).
        mockMvc.perform(patch("/api/loans/" + loanId + "/return").header("Authorization", userAuth))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/books/" + bookId).header("Authorization", userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCopies").value(2));

        // --- ADMIN: podglad i modyfikacje ---

        // 12. Wszystkie wypozyczenia / rezerwacje (tylko ADMIN)
        mockMvc.perform(get("/api/loans").header("Authorization", adminAuth))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/reservations").header("Authorization", adminAuth))
                .andExpect(status().isOk());

        // 13. Anulowanie rezerwacji
        mockMvc.perform(patch("/api/reservations/" + reservationId + "/cancel").header("Authorization", adminAuth))
                .andExpect(status().isOk());

        // 14. Aktualizacja katalogu (PUT covers update na kontrolerach)
        ((ObjectNode) book).put("title", "Ostatnie zyczenie (wyd. 2)");
        mockMvc.perform(put("/api/books/" + bookId)
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(book.toString()))
                .andExpect(status().isOk());

        ((ObjectNode) author).put("bio", "Polski pisarz fantasy.");
        mockMvc.perform(put("/api/authors/" + authorId)
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(author.toString()))
                .andExpect(status().isOk());

        ((ObjectNode) category).put("name", "Fantasy/SF");
        mockMvc.perform(put("/api/categories/" + categoryId)
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(category.toString()))
                .andExpect(status().isOk());

        // 15. Lista uzytkownikow i pojedynczy uzytkownik (ADMIN)
        mockMvc.perform(get("/api/users").header("Authorization", adminAuth))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/" + readerId).header("Authorization", adminAuth))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Bezpieczenstwo: brak tokena -> 401, zly token -> 401")
    void securityRejectsMissingAndInvalidToken() throws Exception {
        // brak naglowka Authorization -> entry point zwraca 401
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isUnauthorized());

        // niepoprawny token -> validateToken=false, brak uwierzytelnienia -> 401
        mockMvc.perform(get("/api/books").header("Authorization", "Bearer to.nie.jest.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RBAC: zwykly USER z prawdziwym tokenem nie moze usuwac ksiazek -> 403")
    void rbacForbidsUserFromAdminOperation() throws Exception {
        register("plainuser", "plain@library.test", "password123");
        String userAuth = bearer(login("plainuser", "password123"));

        // USER moze przegladac katalog
        mockMvc.perform(get("/api/books").header("Authorization", userAuth))
                .andExpect(status().isOk());

        // ale operacja ADMIN (DELETE) -> 403 Forbidden
        mockMvc.perform(delete("/api/books/1").header("Authorization", userAuth))
                .andExpect(status().isForbidden());
    }
}
