# Grocery Backend (Spring Boot)

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
