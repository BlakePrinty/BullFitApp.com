CREATE TABLE split_exercise_override (
                                         id BIGINT NOT NULL AUTO_INCREMENT,
                                         user_split_id BIGINT NOT NULL,
                                         split_day_exercise_id BIGINT NOT NULL,
                                         replacement_exercise_id BIGINT NOT NULL,
                                         PRIMARY KEY (id),
                                         CONSTRAINT fk_override_run FOREIGN KEY (user_split_id) REFERENCES user_split (id) ON DELETE CASCADE,
                                         CONSTRAINT fk_override_sde FOREIGN KEY (split_day_exercise_id) REFERENCES split_day_exercise (id) ON DELETE CASCADE,
                                         CONSTRAINT fk_override_exercise FOREIGN KEY (replacement_exercise_id) REFERENCES exercise (id),
                                         UNIQUE KEY uk_override_run_sde (user_split_id, split_day_exercise_id)
);

ALTER TABLE workout_exercise
    ADD COLUMN split_day_exercise_id BIGINT NULL,
    ADD CONSTRAINT fk_we_sde FOREIGN KEY (split_day_exercise_id)
        REFERENCES split_day_exercise (id) ON DELETE SET NULL;