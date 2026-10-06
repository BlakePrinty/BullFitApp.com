CREATE TABLE body_weight_log (
                                 id BIGINT NOT NULL AUTO_INCREMENT,
                                 user_id BIGINT NOT NULL,
                                 weight DECIMAL(5,1) NOT NULL,
                                 logged_on DATE NOT NULL,
                                 PRIMARY KEY (id),
                                 CONSTRAINT fk_bwl_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                 UNIQUE KEY uk_bwl_user_date (user_id, logged_on)
);

INSERT INTO body_weight_log (user_id, weight, logged_on)
SELECT id, ROUND(weight, 1), DATE(created_at)
FROM users
WHERE weight IS NOT NULL AND weight BETWEEN 50 AND 1000;