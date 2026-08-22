CREATE TABLE group_membership (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    UNIQUE (group_id, user_id)
);
