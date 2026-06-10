CREATE TABLE reservations (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id     BIGINT      NOT NULL REFERENCES books(id),
    reserved_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
);
