# User Management

A session-based administrator console for creating, searching, updating, and deleting user accounts. Pages are server-rendered with JSP. The user table is loaded and changed through a Jersey REST API and jQuery AJAX, backed by MySQL.

## Features

- Email and password login, with BCrypt password verification
- Server-side HTTP session, regenerated after login to prevent session fixation
- 20-minute inactivity timeout
- Logout that invalidates the session
- Administrator-only dashboard and `/api/**` endpoints
- HTTP 401 for anonymous API calls and HTTP 403 for authenticated non-administrators
- Session-bound CSRF token required for login, logout, and every create, update, and delete
- Paginated user directory with search by name or email
- Add, edit, and delete without reloading the page
- Validation on the browser and again on the server
- Duplicate email handling, protection against deleting the signed-in administrator, and protection against removing or demoting the last administrator

## Architecture

```
Browser (JSP + Bootstrap + jQuery)
        |  form POST /login, POST /logout
        |  AJAX JSON /api/users and /api/stats
        v
Spring Security  (session, roles, CSRF, 401/403)
        |
        +-- Spring MVC controllers  ->  JSP views
        |
        +-- Jersey resources        ->  UserService  ->  UserDao (JdbcTemplate)
                                                              |
                                                              v
                                                           MySQL 8
```

| Layer | Package | Responsibility |
| --- | --- | --- |
| Pages | `controller`, `webapp` | Login, home, and forbidden views |
| REST | `resource` | `/api/users` and `/api/stats` |
| Rules | `service` | Validation, hashing, uniqueness, admin safeguards |
| SQL | `dao` | Parameterized JDBC through `JdbcTemplate` |
| Security | `security`, `config` | Login, roles, CSRF, session timeout |
| Model | `model`, `dto` | Database row versus safe JSON |

Password hashes stay in the database and in the DAO. API responses use `UserResponse`, which has no password field. After a successful login, the hash is removed from the session principal.

## Technology and versions

The project compiles for **Java 17** and runs on Java 17 or newer. This machine was verified with Java 21.

| Component | Version |
| --- | --- |
| Spring Boot | 3.4.5 |
| Spring Framework | 6.2.6 |
| Spring Security | 6.4.5 |
| Jersey | 3.1.10 |
| Embedded Tomcat | 10.1.40 |
| MySQL Connector/J | 9.1.0 |
| JSTL | 3.0.1 |
| Bootstrap | 5.3.3 |
| Bootstrap Icons | 1.11.3 |
| jQuery | 3.7.1 |
| JUnit 5 / Mockito | Managed by Spring Boot 3.4.5 |

Spring Boot 3 and Jersey 3 use the Jakarta namespace (`jakarta.servlet`, `jakarta.ws.rs`). They are not compatible with Tomcat 9 or Jersey 2. Deploy this WAR to **Tomcat 10.1**, or run the embedded Tomcat that Spring Boot starts.

JSP is still the page technology. Jersey, not Spring MVC `@RestController`, serves `/api/**`.

## Prerequisites

- JDK 17 or newer
- Maven 3.9 or newer
- MySQL 8, installed and running on this machine

## Database setup

1. Install MySQL Server 8 and start it so it listens on `localhost:3306`.
2. Connect with MySQL Workbench or the MySQL command-line client.
3. Execute `database/schema.sql`. From the project directory:

```bash
mysql -u root -p < database/schema.sql
```

The script creates the `user_management` database and the `users` table. It does not insert the administrator. On startup, `AdminSeeder` creates that account when the email is missing and stores only a BCrypt hash. An existing account keeps its password.

4. Set the database environment variables. See the next section. The development default password is `root`.
5. Start the application with `mvn spring-boot:run`.
6. Open [http://localhost:8080/login](http://localhost:8080/login).

The application also runs `src/main/resources/schema.sql` on startup (`CREATE TABLE IF NOT EXISTS`) against the database named in the JDBC URL. Create the database with `database/schema.sql` first.

## Configuration

Defaults live in `src/main/resources/application.properties` and are meant for local development. Override them with environment variables. `.env.example` lists the same values. Do not commit a real `.env` or `application-local.properties`.

The JDBC URL is built from the host, port, and database name:

`jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8`

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `user_management` | Database name |
| `DB_USERNAME` | `root` | Database user |
| `DB_PASSWORD` | `root` | Database password. Set this to the local root password |
| `ADMIN_EMAIL` | `admin@usermanagement.local` | Seeded administrator email |
| `ADMIN_PASSWORD` | `Admin@12345` | Seeded administrator password |
| `ADMIN_NAME` | `System Administrator` | Seeded display name |
| `ADMIN_PHONE` | `5550100199` | Seeded phone |

Example:

```bash
set -a
source .env.example
set +a
```

If the administrator email already exists, startup does not reset the password. Delete that row to seed it again.

## Development login

| Field | Value |
| --- | --- |
| Email | `admin@usermanagement.local` |
| Password | `Admin@12345` |

Phone is required for every user, including this account. These credentials are for local development only.

## Build and run

```bash
mvn clean test
mvn package
mvn spring-boot:run
```

Open [http://localhost:8080/login](http://localhost:8080/login). A successful administrator login goes to [http://localhost:8080/home](http://localhost:8080/home).

You can also run the WAR directly from the project directory:

```bash
java -jar target/user-management.war
```

`src/main/webapp` is used as the JSP root when that directory exists, which is what makes pages render under `mvn spring-boot:run` and `java -jar` launched from the project. Session timeout is 20 minutes.

## Deploy to Tomcat 10.1

1. Install Apache Tomcat **10.1** (not Tomcat 9).
2. Build `target/user-management.war`.
3. Copy it to `webapps/`.
4. Provide `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, and the admin variables to the Tomcat process.
5. Start Tomcat and open `http://localhost:8080/user-management/login`.

The embedded Tomcat libraries are inside the WAR. If an external Tomcat reports class conflicts, run `mvn spring-boot:run` instead. That path is the one exercised by the tests.

## Validation rules

| Field | Rule |
| --- | --- |
| Name | Required, 2–100 characters, letters, spaces, apostrophes, periods, and hyphens |
| Email | Required, valid format, at most 150 characters, unique, stored in lowercase |
| Phone | Required, 7–20 characters, 7–15 digits, digits and `+() -` spaces only |
| Password | Required on create. 8–72 characters with uppercase, lowercase, and a digit. Blank on edit keeps the current hash |
| Role | `ADMIN` or `USER` |

Search text is at most 100 characters. Page size is 1–50 and defaults to 10. Results are ordered by id descending.

## REST API

All `/api/**` routes require an administrator session and, for POST, PUT, and DELETE, the `X-CSRF-TOKEN` header. The token is in the home page as `<meta name="_csrf">`.

### List users

`GET /api/users?page=1&size=10&search=ada`

```json
{
  "users": [
    {
      "id": 2,
      "name": "Ada Lovelace",
      "email": "ada@example.com",
      "phone": "5550100123",
      "role": "USER",
      "createdAt": "2026-10-09T12:00:00",
      "updatedAt": "2026-10-09T12:00:00"
    }
  ],
  "page": 1,
  "size": 10,
  "total": 1,
  "totalPages": 1
}
```

### Get one user

`GET /api/users/{id}` returns the same user object.

### Create

`POST /api/users` with `Content-Type: application/json` returns **201** and the created user.

```json
{
  "name": "Ada Lovelace",
  "email": "ada@example.com",
  "phone": "5550100123",
  "password": "Secret123",
  "role": "USER"
}
```

### Update

`PUT /api/users/{id}` returns **200**. Send `"password": ""` to leave the hash unchanged.

### Delete

`DELETE /api/users/{id}` returns **204** with an empty body.

### Statistics

`GET /api/stats`

```json
{ "totalUsers": 3, "adminCount": 1, "standardUserCount": 2 }
```

### Errors

```json
{ "message": "Validation failed", "errors": { "email": "Enter a valid email address" } }
```

| Status | When |
| --- | --- |
| 400 | Invalid JSON, bad id, or validation failure |
| 401 | No session, or the session expired |
| 403 | Missing or invalid CSRF token, or the account is not an administrator |
| 404 | User does not exist |
| 409 | Duplicate email, deleting yourself, or removing the last administrator |
| 500 | Unexpected failure. The body is a generic message with no SQL or stack trace |

### curl session example

```bash
curl -c cookies.txt -D - http://localhost:8080/login -o login.html
TOKEN=$(sed -n 's/.*name="_csrf" value="\([^"]*\)".*/\1/p' login.html)
curl -c cookies.txt -b cookies.txt -D - -o /dev/null \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "email=admin@usermanagement.local&password=Admin@12345&_csrf=$TOKEN" \
  http://localhost:8080/login
curl -b cookies.txt http://localhost:8080/home -o home.html
CSRF=$(sed -n 's/.*name="_csrf" content="\([^"]*\)".*/\1/p' home.html)
curl -b cookies.txt -H "X-CSRF-TOKEN: $CSRF" -H "Content-Type: application/json" \
  -d '{"name":"Ada Lovelace","email":"ada@example.com","phone":"5550100123","password":"Secret123","role":"USER"}' \
  http://localhost:8080/api/users
```

Login replaces the session id. Read the CSRF token from the home page after login, not from the anonymous login page.

## Session and CSRF

- The session cookie is `JSESSIONID`, `HttpOnly`, `SameSite=Lax`, and is not written into the URL.
- Spring Security changes the session id when authentication succeeds.
- The session expires after 20 minutes of inactivity. A later page request goes to `/login?expired`. An API request returns 401, and the dashboard script sends the browser back to the login page.
- The CSRF token is stored in the HTTP session. Forms post it as `_csrf`. AJAX sends it in `X-CSRF-TOKEN`.
- Logout is a POST and requires the same token.
- Protected pages send `Cache-Control: no-store`. The dashboard also reloads if the browser restores it from the back-forward cache.

A signed-in `USER` can authenticate, then receives 403 on `/api/**` and is sent to `/forbidden` for the dashboard.

SQL from the application uses constant statements and `PreparedStatement` parameters. Search wildcards `%` and `_` are escaped before they are bound to `LIKE`.

## Tests

```bash
mvn test
```

Unit tests cover validation, BCrypt verification, hashing before save, duplicate email, self-delete, last-administrator rules, query parsing, and JSON that omits password hashes. They do not need MySQL.

`UserManagementIntegrationTest` starts the real application and talks to the local MySQL server on `localhost:3306`. It checks login, session fixation, CSRF, CRUD, search, duplicate email, SQL-injection-shaped input, role restrictions, logout, and unauthenticated access. MySQL must already be running and `database/schema.sql` must have been applied. The test is not skipped when the database is unavailable.

```bash
mvn clean test
mvn package
```

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| Communications link failure | MySQL Server is not running, or `DB_HOST` / `DB_PORT` does not match |
| Access denied for user | `DB_USERNAME` / `DB_PASSWORD` |
| Unknown database | Execute `database/schema.sql` |
| Login page says the credentials are invalid | The administrator row may predate a password change. Delete it and restart so seeding runs again |
| 403 on create or delete | The page was left open across login. Reload `/home` so the script reads the current CSRF token |
| 401 from the dashboard | The session expired. Sign in again |
| JSP 404 | Run from the project directory with `mvn spring-boot:run`. External Tomcat must be 10.1 |
| Port 8080 in use | Stop the other process or set `server.port` |

Stack traces, SQL, and database credentials are not returned to the browser. Unexpected failures are logged on the server.
