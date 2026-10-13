-- 用户账号表（H2）。全部用 IF NOT EXISTS，保证每次启动重复执行也安全。
CREATE TABLE IF NOT EXISTS app_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(32)  NOT NULL,
    display_name  VARCHAR(32)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    create_time   TIMESTAMP    NOT NULL,
    CONSTRAINT uk_app_user_username UNIQUE (username)
);