CREATE TABLE friend_request (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                sender_id BIGINT NOT NULL,
                                receiver_id BIGINT NOT NULL,
                                status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                created_at DATETIME NOT NULL,
                                responded_at DATETIME NULL,
                                PRIMARY KEY (id),
                                CONSTRAINT fk_fr_sender FOREIGN KEY (sender_id) REFERENCES users (id),
                                CONSTRAINT fk_fr_receiver FOREIGN KEY (receiver_id) REFERENCES users (id),
                                KEY idx_fr_receiver_status (receiver_id, status),
                                KEY idx_fr_sender_status (sender_id, status)
);

CREATE TABLE friendship (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            user_a_id BIGINT NOT NULL,
                            user_b_id BIGINT NOT NULL,
                            created_at DATETIME NOT NULL,
                            PRIMARY KEY (id),
                            CONSTRAINT fk_friend_a FOREIGN KEY (user_a_id) REFERENCES users (id),
                            CONSTRAINT fk_friend_b FOREIGN KEY (user_b_id) REFERENCES users (id),
                            CONSTRAINT chk_friend_order CHECK (user_a_id < user_b_id),
                            UNIQUE KEY uk_friendship_pair (user_a_id, user_b_id),
                            KEY idx_friendship_b (user_b_id)
);

CREATE TABLE user_block (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            blocker_id BIGINT NOT NULL,
                            blocked_id BIGINT NOT NULL,
                            created_at DATETIME NOT NULL,
                            PRIMARY KEY (id),
                            CONSTRAINT fk_block_blocker FOREIGN KEY (blocker_id) REFERENCES users (id),
                            CONSTRAINT fk_block_blocked FOREIGN KEY (blocked_id) REFERENCES users (id),
                            UNIQUE KEY uk_block_pair (blocker_id, blocked_id),
                            KEY idx_block_blocked (blocked_id)
);