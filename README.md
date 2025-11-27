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

---

# Additional info
This repository contains the Spring Boot authentication backend for the Grocery Go demo.

Contents
- `src/` — Java source (controllers, security, JPA entities)
- `pom.xml` — Maven build file
- `Dockerfile` — builds and runs the Spring Boot fat JAR
- `k8s-deployment.yaml` — sample Kubernetes Deployment + Service
- `create_sample_user.sql` — helper to seed a test user in local MySQL

Run locally
You can run the application with Maven or via Docker.

With Maven:
```powershell
cd backend-spring
mvn -DskipTests spring-boot:run
```

Using Docker (requires Docker):
```powershell
docker build -t groceries-backend:local ./backend-spring
docker run --rm -p 8080:8080 -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/groceries" -e SPRING_DATASOURCE_USERNAME=root -e SPRING_DATASOURCE_PASSWORD=changeme -e APP_JWT_SECRET='change_me' groceries-backend:local
```

Notes
- For development the repo seeds a `credentials` table and currently stores plaintext passwords — **replace with BCrypt** before production.
- See the project root `docker-compose.yml` for a ready local stack (frontend + backend + MySQL).


