-- V1: Create library schema and seed data
-- Managed by Flyway — NEVER modify this file after it has been applied.
-- To change the schema, create V2__description.sql

-- ── Books table ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS books (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    title            VARCHAR(500) NOT NULL,
    author           VARCHAR(300) NOT NULL,
    isbn             VARCHAR(20),
    genre            VARCHAR(50),
    publication_year INTEGER,
    description      TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT books_pkey PRIMARY KEY (id),
    CONSTRAINT books_isbn_key UNIQUE (isbn),
    CONSTRAINT books_genre_check CHECK (
        genre IS NULL OR genre IN (
            'FICTION', 'NON_FICTION', 'TECHNOLOGY', 'SCIENCE',
            'HISTORY', 'BIOGRAPHY', 'MYSTERY', 'ROMANCE',
            'FANTASY', 'SCIENCE_FICTION', 'SELF_HELP', 'BUSINESS', 'OTHER'
        )
    )
);

-- ── Indexes ────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_books_genre ON books (genre);
CREATE INDEX IF NOT EXISTS idx_books_title_author ON books (title, author);
CREATE INDEX IF NOT EXISTS idx_books_publication_year ON books (publication_year);

-- ── Seed data (10 books for dev and integration tests) ────────────────────
-- Integration tests assert against these specific books.
-- If you need to change seed data, create V2__update_seed_data.sql.

INSERT INTO books (id, title, author, isbn, genre, publication_year, description) VALUES
(
    '11111111-1111-1111-1111-111111111111',
    'Spring in Action',
    'Craig Walls',
    '9781617294945',
    'TECHNOLOGY',
    2022,
    'Spring in Action is the definitive guide to Spring, covering Spring core, Spring MVC, Spring REST, Spring Security, and much more.'
),
(
    '22222222-2222-2222-2222-222222222222',
    'Spring Boot in Practice',
    'Somnath Musib',
    '9781617298813',
    'TECHNOLOGY',
    2022,
    'Spring Boot in Practice is a cookbook-style guide to building Spring Boot applications with practical techniques and examples.'
),
(
    '33333333-3333-3333-3333-333333333333',
    'Clean Code',
    'Robert C. Martin',
    '9780132350884',
    'TECHNOLOGY',
    2008,
    'A handbook of agile software craftsmanship. Covers best practices for writing clean, readable, and maintainable code.'
),
(
    '44444444-4444-4444-4444-444444444444',
    'The Pragmatic Programmer',
    'David Thomas, Andrew Hunt',
    '9780135957059',
    'TECHNOLOGY',
    2019,
    'The Pragmatic Programmer cuts through the increasing specialization and technicalities of modern software development to examine the core process.'
),
(
    '55555555-5555-5555-5555-555555555555',
    'Sapiens: A Brief History of Humankind',
    'Yuval Noah Harari',
    '9780062316097',
    'HISTORY',
    2015,
    'In Sapiens, Dr Yuval Noah Harari spans the whole of human history, from the very first humans to walk the earth to the radical and sometimes devastating revolutions of the twenty-first century.'
),
(
    '66666666-6666-6666-6666-666666666666',
    'The Great Gatsby',
    'F. Scott Fitzgerald',
    '9780743273565',
    'FICTION',
    1925,
    'The Great Gatsby is a 1925 novel by American writer F. Scott Fitzgerald. Set in the Jazz Age on Long Island, near New York City, the novel depicts first-person narrator Nick Carraway''s interactions with mysterious millionaire Jay Gatsby.'
),
(
    '77777777-7777-7777-7777-777777777777',
    'Thinking, Fast and Slow',
    'Daniel Kahneman',
    '9780374533557',
    'SCIENCE',
    2011,
    'In the international bestseller, Thinking, Fast and Slow, Daniel Kahneman, the renowned psychologist and winner of the Nobel Prize in Economics, takes us on a groundbreaking tour of the mind.'
),
(
    '88888888-8888-8888-8888-888888888888',
    'The Hitchhiker''s Guide to the Galaxy',
    'Douglas Adams',
    '9780345391803',
    'SCIENCE_FICTION',
    1979,
    'Seconds before Earth is demolished to make way for a hyperspace bypass, Arthur Dent is plucked off the planet by his friend Ford Prefect, a researcher for the revised edition of The Hitchhiker''s Guide to the Galaxy who has been posing as an out-of-work actor.'
),
(
    '99999999-9999-9999-9999-999999999999',
    'Atomic Habits',
    'James Clear',
    '9780735211292',
    'SELF_HELP',
    2018,
    'No matter your goals, Atomic Habits offers a proven framework for improving every day. James Clear, one of the world''s leading experts on habit formation, reveals practical strategies that will teach you exactly how to form good habits, break bad ones, and master the tiny behaviors that lead to remarkable results.'
),
(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Designing Data-Intensive Applications',
    'Martin Kleppmann',
    '9781449373320',
    'TECHNOLOGY',
    2017,
    'Data is at the center of many challenges in system design today. Difficult issues need to be figured out, such as scalability, consistency, reliability, efficiency, and maintainability. In addition, we have an overwhelming variety of tools, including relational databases, NoSQL datastores, stream or batch processors, and message brokers.'
);
