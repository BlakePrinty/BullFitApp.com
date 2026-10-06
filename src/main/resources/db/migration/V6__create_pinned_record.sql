CREATE TABLE pinned_record (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               user_id BIGINT NOT NULL,
                               exercise_id BIGINT NOT NULL,
                               slot INT NOT NULL,
                               PRIMARY KEY (id),
                               CONSTRAINT fk_pinned_user FOREIGN KEY (user_id) REFERENCES users (id),
                               CONSTRAINT fk_pinned_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id),
                               UNIQUE KEY uk_pinned_user_exercise (user_id, exercise_id),
                               UNIQUE KEY uk_pinned_user_slot (user_id, slot),
                               CONSTRAINT chk_pinned_slot CHECK (slot BETWEEN 1 AND 4)
);