CREATE TABLE app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255),
    invitation_token_hash VARCHAR(255),
    invitation_token_expires_at TIMESTAMP,
    invited_at TIMESTAMP,
    invited_by BIGINT,
    registered_at TIMESTAMP,
    failed_login_count INT NOT NULL,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
