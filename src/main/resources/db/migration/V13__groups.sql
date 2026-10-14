CREATE TABLE accountability_group (
                                      id BIGINT NOT NULL AUTO_INCREMENT,
                                      owner_id BIGINT NOT NULL,
                                      name VARCHAR(50) NOT NULL,
                                      description VARCHAR(300) NULL,
                                      weekly_goal INT NOT NULL DEFAULT 3,
                                      created_at DATETIME NOT NULL,
                                      PRIMARY KEY (id),
                                      CONSTRAINT fk_group_owner FOREIGN KEY (owner_id) REFERENCES users (id),
                                      CONSTRAINT chk_group_goal CHECK (weekly_goal BETWEEN 1 AND 7),
                                      KEY idx_group_owner (owner_id)
);

CREATE TABLE group_member (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              group_id BIGINT NOT NULL,
                              user_id BIGINT NOT NULL,
                              status VARCHAR(20) NOT NULL DEFAULT 'INVITED',
                              invited_by BIGINT NOT NULL,
                              created_at DATETIME NOT NULL,
                              joined_at DATETIME NULL,
                              PRIMARY KEY (id),
                              CONSTRAINT fk_gm_group FOREIGN KEY (group_id) REFERENCES accountability_group (id) ON DELETE CASCADE,
                              CONSTRAINT fk_gm_user FOREIGN KEY (user_id) REFERENCES users (id),
                              CONSTRAINT fk_gm_inviter FOREIGN KEY (invited_by) REFERENCES users (id),
                              UNIQUE KEY uk_gm_group_user (group_id, user_id),
                              KEY idx_gm_user_status (user_id, status)
);