CREATE TABLE exercise_request (
                                  id BIGINT NOT NULL AUTO_INCREMENT,
                                  exercise_id BIGINT NOT NULL,
                                  requester_id BIGINT NOT NULL,
                                  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                  note VARCHAR(500) NULL,
                                  review_note VARCHAR(500) NULL,
                                  reviewed_by BIGINT NULL,
                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  reviewed_at DATETIME NULL,
                                  PRIMARY KEY (id),
                                  CONSTRAINT fk_exreq_exercise FOREIGN KEY (exercise_id) REFERENCES exercise (id),
                                  CONSTRAINT fk_exreq_requester FOREIGN KEY (requester_id) REFERENCES users (id),
                                  CONSTRAINT fk_exreq_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id),
                                  KEY idx_exreq_status (status),
                                  KEY idx_exreq_exercise (exercise_id)
);