CREATE TABLE bullsplit (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           owner_id BIGINT NOT NULL,
                           name VARCHAR(60) NOT NULL,
                           description VARCHAR(300) NULL,
                           day_count INT NOT NULL,
                           is_published BOOLEAN NOT NULL DEFAULT FALSE,
                           archived BOOLEAN NOT NULL DEFAULT FALSE,
                           is_copy BOOLEAN NOT NULL DEFAULT FALSE,
                           copied_from_id BIGINT NULL,
                           created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           PRIMARY KEY (id),
                           CONSTRAINT fk_split_owner FOREIGN KEY (owner_id) REFERENCES users (id),
                           CONSTRAINT fk_split_copied_from FOREIGN KEY (copied_from_id) REFERENCES bullsplit (id) ON DELETE SET NULL,
                           CONSTRAINT chk_split_days CHECK (day_count BETWEEN 1 AND 8),
                           KEY idx_split_owner (owner_id),
                           KEY idx_split_published (is_published, day_count)
);

CREATE TABLE split_day (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           split_id BIGINT NOT NULL,
                           day_number INT NOT NULL,
                           label VARCHAR(40) NULL,
                           PRIMARY KEY (id),
                           CONSTRAINT fk_day_split FOREIGN KEY (split_id) REFERENCES bullsplit (id) ON DELETE CASCADE,
                           UNIQUE KEY uk_day_split_number (split_id, day_number)
);

CREATE TABLE split_day_exercise (
                                    id BIGINT NOT NULL AUTO_INCREMENT,
                                    split_day_id BIGINT NOT NULL,
                                    exercise_id BIGINT NOT NULL,
                                    order_index INT NOT NULL,
                                    target_sets INT NULL,
                                    target_reps VARCHAR(20) NULL,
                                    PRIMARY KEY (id),
                                    CONSTRAINT fk_sde_day FOREIGN KEY (split_day_id) REFERENCES split_day (id) ON DELETE CASCADE,
                                    CONSTRAINT fk_sde_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id),
                                    UNIQUE KEY uk_sde_day_exercise (split_day_id, exercise_id)
);