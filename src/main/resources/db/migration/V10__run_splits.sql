CREATE TABLE user_split (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            user_id BIGINT NOT NULL,
                            split_id BIGINT NOT NULL,
                            status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                            target_length_days INT NULL,
                            current_day_number INT NOT NULL DEFAULT 1,
                            started_at DATETIME NOT NULL,
                            ended_at DATETIME NULL,
                            PRIMARY KEY (id),
                            CONSTRAINT fk_us_user FOREIGN KEY (user_id) REFERENCES users (id),
                            CONSTRAINT fk_us_split FOREIGN KEY (split_id) REFERENCES bullsplit (id),
                            KEY idx_us_user_status (user_id, status),
                            KEY idx_us_split (split_id)
);

ALTER TABLE workout
    ADD COLUMN user_split_id BIGINT NULL,
    ADD COLUMN split_day_id BIGINT NULL,
    ADD CONSTRAINT fk_workout_user_split FOREIGN KEY (user_split_id) REFERENCES user_split (id),
    ADD CONSTRAINT fk_workout_split_day FOREIGN KEY (split_day_id) REFERENCES split_day (id);

ALTER TABLE workout_exercise
    ADD COLUMN planned BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN target_text VARCHAR(30) NULL,
    ADD COLUMN swapped_from_id BIGINT NULL,
    ADD CONSTRAINT fk_we_swapped_from FOREIGN KEY (swapped_from_id) REFERENCES exercise (id);