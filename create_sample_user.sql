-- Sample SQL to create a test user and credentials for local development
-- Adjust name, email, and password as needed.

INSERT INTO users (name, email, password_hash)
VALUES ('Test User', 'test@example.com', 'secret');

SET @uid = LAST_INSERT_ID();

INSERT INTO credentials (user_id, password_hash)
VALUES (@uid, 'secret');

-- Verify:
SELECT u.id, u.name, u.email, c.password_hash AS cred_password
FROM users u
LEFT JOIN credentials c ON c.user_id = u.id
WHERE u.email = 'test@example.com';
