# 📚 Library-fo-ruina

System zarządzania księgarnią / wypożyczalnią książek — Java 25 + Spring Boot 4.

## Autorzy

| Imię i nazwisko | Nr indeksu | Rola |
|---|---|---|
| [Imię Nazwisko A] | [indeks] | Student A (Lider) |
| [Imię Nazwisko B] | [indeks] | Student B (Partner) |

## Technologie

- Java 25, Spring Boot 4.0.6
- Spring Security + JWT
- Hibernate + PostgreSQL
- Flyway (migracje V1–V7)
- Swagger / OpenAPI 3 (springdoc)
- JUnit 5 + Mockito + TestContainers
- JaCoCo (pokrycie ≥ 80%)
- Docker + Docker Compose, Maven

## Uruchomienie (Docker)

```bash
git clone https://github.com/Dzanek309/library-of-ruina
cd library-of-ruina
cp .env.example .env        # uzupelnij sekrety
docker-compose up --build
```

- Aplikacja: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

## Uruchomienie lokalne (wymaga PostgreSQL)

```bash
./mvnw spring-boot:run
```

## Wzorce i OOP

### Polimorfizm
Abstrakcyjna klasa `Book` → `PaperBook`, `Ebook`, `Audiobook`. Każda nadpisuje
`getLoanPeriodDays()` (14 / 30 / 21 dni) i `getFormatInfo()`. Termin zwrotu
wypożyczenia liczony jest polimorficznie w `LoanService`.

### Wzorzec projektowy: Strategy
Interfejs `BookSearchStrategy` + implementacje `SearchByTitle`, `SearchByAuthor`,
`SearchByCategory`, `SearchByAvailable`. Endpoint: `GET /api/books/search?strategy=title&value=java`.

### Factory Method
`BookService.create()` tworzy odpowiednią podklasę `Book` na podstawie pola `format` żądania.

## Role (RBAC)

| Operacja | USER | ADMIN |
|---|---|---|
| Rejestracja / logowanie | ✅ | ✅ |
| Przeglądanie i wyszukiwanie książek | ✅ | ✅ |
| Rezerwacja książki | ✅ | ✅ |
| Wypożyczenie / zwrot | ✅ | ✅ |
| Historia własnych wypożyczeń | ✅ | ✅ |
| Dodawanie/edycja/usuwanie książek, autorów, kategorii | ❌ | ✅ |
| Podgląd wszystkich rezerwacji/wypożyczeń | ❌ | ✅ |
| Zarządzanie użytkownikami | ❌ | ✅ |

## Główne endpointy

### Auth
- `POST /api/auth/register` — rejestracja
- `POST /api/auth/login` — logowanie (zwraca JWT)

### Books
- `GET /api/books` — katalog
- `GET /api/books/{id}` — szczegóły
- `GET /api/books/search?strategy=...&value=...` — wyszukiwanie (Strategy)
- `GET /api/books/{id}/format-info` — opis formatu (polimorfizm)
- `POST /api/books` `PUT /api/books/{id}` `DELETE /api/books/{id}` — ADMIN

### Authors
- `GET /api/authors` — lista autorów
- `POST /api/authors` `PUT /api/authors/{id}` `DELETE /api/authors/{id}` — ADMIN

### Categories
- `GET /api/categories` — lista kategorii
- `POST /api/categories` `PUT /api/categories/{id}` `DELETE /api/categories/{id}` — ADMIN

### Reservations
- `GET /api/reservations` (ADMIN), `GET /api/reservations/user/{id}`
- `POST /api/reservations` — rezerwuj
- `PATCH /api/reservations/{id}/cancel`

### Loans
- `POST /api/loans/borrow` — wypożycz
- `PATCH /api/loans/{id}/return` — zwróć
- `GET /api/loans/user/{id}/history` — historia

### Users (ADMIN)
- `GET /api/users` — wszyscy użytkownicy
- `DELETE /api/users/{id}` — usuń użytkownika

## Testy

```bash
./mvnw clean test          # testy + agent JaCoCo
./mvnw jacoco:report       # raport: target/site/jacoco/index.html
./mvnw clean verify        # build padnie jesli pokrycie < 80%
```

## Diagram ERD

<!-- TODO (Faza 8): wstaw obrazek ERD wygenerowany z dbdiagram.io / pgAdmin -->

## Screeny (do uzupełnienia w Fazie 8)

- [ ] Swagger UI z grupami endpointów (Books, Loans, Reservations, ...)
- [ ] Wywołanie POST + odpowiedź (np. rejestracja, dodanie książki, wypożyczenie)
- [ ] Raport JaCoCo ≥ 80%
- [ ] Diagram ERD
- [ ] `git log --graph --oneline --all` (struktura gałęzi i merge'ów)
- [ ] Historia Pull Requestów z komentarzami Code Review i statusem "Approved"
- [ ] Tabele w bazie (psql / pgAdmin)