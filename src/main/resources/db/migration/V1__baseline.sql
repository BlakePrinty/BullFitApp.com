CREATE TABLE users (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       username VARCHAR(30) NOT NULL,
                       password_hash VARCHAR(100) NOT NULL,
                       email VARCHAR(255) NULL,
                       first_name VARCHAR(50) NULL,
                       last_name VARCHAR(50) NULL,
                       height DECIMAL(5,2) NULL,
                       weight DECIMAL(6,2) NULL,
                       birth_date DATE NULL,
                       bio VARCHAR(160) NULL,
                       role VARCHAR(20) NOT NULL DEFAULT 'USER',
                       username_color VARCHAR(7) NULL,
                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       PRIMARY KEY (id),
                       UNIQUE KEY uk_users_username (username),
                       UNIQUE KEY uk_users_email (email)
);