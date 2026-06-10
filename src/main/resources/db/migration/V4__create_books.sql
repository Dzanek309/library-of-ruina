CREATE TABLE books (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    isbn VARCHAR(20) NOT NULL UNIQUE,
    publication_year INT,
    format VARCHAR(20) NOT NULL,
    total_copies INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1,
    description TEXT,
    category_id BIGINT REFERENCES categories(id),
    dtype VARCHAR(31) NOT NULL,
    pages INT,
    file_format VARCHAR(20),
    duration_minutes INT,
    narrator VARCHAR(150)
);
