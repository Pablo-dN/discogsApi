CREATE TABLE artist (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    discogs_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    profile TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE albums (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    artist_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    year INT,
    release_id BIGINT NOT NULL,           -- ID único del release o master
    type ENUM('release', 'master') NOT NULL, -- Indica si es un release o master
    format VARCHAR(255),
    label VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (artist_id) REFERENCES artist(id) ON DELETE CASCADE
);

