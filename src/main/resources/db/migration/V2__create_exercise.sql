CREATE TABLE exercise (
                          id BIGINT NOT NULL AUTO_INCREMENT,
                          name VARCHAR(80) NOT NULL,
                          muscle_group VARCHAR(20) NOT NULL,
                          equipment VARCHAR(30) NOT NULL,
                          owner_id BIGINT NULL,
                          active BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          owner_key BIGINT GENERATED ALWAYS AS (IFNULL(owner_id, 0)) STORED,
                          PRIMARY KEY (id),
                          CONSTRAINT fk_exercise_owner FOREIGN KEY (owner_id) REFERENCES users (id),
                          UNIQUE KEY uk_exercise_owner_name (owner_key, name)
);