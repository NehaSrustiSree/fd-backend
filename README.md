# Groceries Spring Boot Auth Backend

Spring Boot REST API for the Grocery Go application with JWT authentication, BCrypt password hashing, and MySQL database.

## Quick Start

### Docker Compose (Recommended)
``powershell
docker compose up --build backend -d
``

### Maven
``powershell
mvn spring-boot:run
``

Server runs on: http://localhost:8080

## API Endpoints

| Method | Endpoint | Body | Response |
|--------|----------|------|----------|
| POST | /api/auth/signup | {name, email, password} | User object + auth cookie |
| POST | /api/auth/login | {email, password} | User object + auth cookie |
| POST | /api/auth/logout | — | {ok: true} |
| GET | /api/auth/me | — | User object (requires auth) |

## Configuration

Set environment variables for Docker or application.properties:

``properties
spring.datasource.url=jdbc:mysql://localhost:3306/groceries
spring.datasource.username=root
spring.datasource.password=changeme
app.jwt.secret=your-secret-key
app.jwt.expiration-days=7
app.cors.allowed-origins=http://localhost:5173
``

## Database Schema

- **users** — user profiles (id, name, email, password_hash)
- **credentials** — BCrypt password hashes (id, user_id, password_hash)

Schema is automatically created on startup from src/main/resources/schema.sql.

## Password Security

 **BCrypt hashing** for all passwords  
 **Never plaintext** in the database  
 Migration endpoint available for legacy plaintext data

## Kubernetes Deployment

Deploy all services:

``ash
kubectl apply -f k8s/
``

Access backend via NodePort at port 30080.

## Security Checklist for Production

- [ ] Use strong, random JWT secret (32+ characters)
- [ ] Enable HTTPS/TLS on all endpoints
- [ ] Update CORS origins with actual domain
- [ ] Restrict or remove /api/auth/migrate-passwords endpoint
- [ ] Use Kubernetes Secrets for sensitive configuration
- [ ] Implement rate limiting on authentication endpoints
- [ ] Enable database encryption at rest
- [ ] Regular dependency security updates

## Troubleshooting

**Port 8080 in use:**
``powershell
netstat -ano | findstr :8080
``

**Database connection error:**
- Verify MySQL is running: docker ps | findstr mysql
- Check connection string in application.properties

**JWT errors:**
- Ensure auth_token cookie is present
- Verify JWT secret matches between signup and login
- Check token expiration (default 7 days)

## Development

``powershell
mvn test                # Run unit tests
mvn clean package       # Build JAR
``

## Key Dependencies

- Spring Boot 3.3.5
- Spring Security 6.x with JWT
- Spring Data JPA / Hibernate
- MySQL Connector 8.3
- JJWT 0.11.5 (JWT signing/parsing)
- BCrypt password encoder

## Files

- src/ — Java source (auth controller, security, entities)
- pom.xml — Maven dependencies
- Dockerfile — Multi-stage production build
- schema.sql — Database schema
- application.properties — Spring configuration
