ALTER TABLE users ADD COLUMN time_zone VARCHAR(50) NOT NULL DEFAULT 'America/New_York';

CREATE TABLE workout (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         user_id BIGINT NOT NULL,
                         status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
                         started_at DATETIME NOT NULL,
                         ended_at DATETIME NULL,
                         PRIMARY KEY (id),
                         CONSTRAINT fk_workout_user FOREIGN KEY (user_id) REFERENCES users (id),
                         KEY idx_workout_user_started (user_id, started_at)
);

CREATE TABLE workout_exercise (
                                  id BIGINT NOT NULL AUTO_INCREMENT,
                                  workout_id BIGINT NOT NULL,
                                  exercise_id BIGINT NOT NULL,
                                  order_index INT NOT NULL,
                                  PRIMARY KEY (id),
                                  CONSTRAINT fk_we_workout FOREIGN KEY (workout_id) REFERENCES workout (id) ON DELETE CASCADE,
                                  CONSTRAINT fk_we_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id),
                                  KEY idx_we_workout (workout_id),
                                  KEY idx_we_exercise (exercise_id)
);

CREATE TABLE workout_set (
                             id BIGINT NOT NULL AUTO_INCREMENT,
                             workout_exercise_id BIGINT NOT NULL,
                             set_number INT NOT NULL,
                             weight DECIMAL(6,2) NULL,
                             reps INT NOT NULL,
                             PRIMARY KEY (id),
                             CONSTRAINT fk_set_we FOREIGN KEY (workout_exercise_id) REFERENCES workout_exercise (id) ON DELETE CASCADE,
                             KEY idx_set_we (workout_exercise_id)
);

CREATE TABLE cardio_entry (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              workout_exercise_id BIGINT NOT NULL,
                              duration_seconds INT NOT NULL,
                              distance DECIMAL(6,2) NULL,
                              speed DECIMAL(5,2) NULL,
                              incline DECIMAL(4,1) NULL,
                              PRIMARY KEY (id),
                              CONSTRAINT fk_cardio_we FOREIGN KEY (workout_exercise_id) REFERENCES workout_exercise (id) ON DELETE CASCADE,
                              KEY idx_cardio_we (workout_exercise_id)
);