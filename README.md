Groceries Spring Boot Auth Backend

Prerequisites
- Java 21
- Maven 3.9+
- MySQL running locally

Configure DB and JWT
- Edit src/main/resources/application.properties:
  - spring.datasource.username and spring.datasource.password
  - app.jwt.secret (use a long random string)
  - app.cors.allowed-origins=http://localhost:5173 (Vite dev)
- Schema is auto-created via schema.sql and JPA (ddl-auto=update).

Database changes
- A new `credentials` table is created to hold password hashes per-user. The repo includes
  `src/main/resources/schema.sql` which now contains a `credentials` table with a
  foreign key to `users(id)`. During signup the backend also creates a credentials record.

Migration notes
- If you already have users with `password_hash` in the `users` table, the application
  will still work (signup writes both `users.password_hash` and `credentials.password_hash`).
  Consider running a one-time migration to populate `credentials` from `users` and then
  removing the `password_hash` column from `users` in a future schema cleanup.

Run
```
mvn -q spring-boot:run
```
Server: http://localhost:8080

Endpoints
- POST /api/auth/signup {name,email,password}
- POST /api/auth/login {email,password}
- POST /api/auth/logout
- GET /api/auth/me

Notes
- Session is kept in an auth_token httpOnly cookie.

