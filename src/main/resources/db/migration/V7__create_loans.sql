CREATE TABLE loans (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id     BIGINT      NOT NULL REFERENCES books(id),
    borrowed_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    due_date    TIMESTAMP   NOT NULL,
    returned_at TIMESTAMP,
    status      VARCHAR(20) NOT NULL DEFAULT 'BORROWED'
);
