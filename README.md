# ShopEx — Exercise 3 (Spring MVC final project)

A server-rendered e-commerce site built with Spring Boot MVC + Thymeleaf (no React/SPA framework, per the exercise requirement). Users can browse and search products, read/write reviews, build a cart before logging in, register, log in, check out, and view their order history. Admins get a `/admin` backend for managing products, orders, and users.

## Functionality

- **Catalog** — browse/search/filter/sort/paginate products (`/products`), product detail with reviews and average rating (`/products/{id}`).
- **Cart & checkout** — session-based cart, usable without login (`/cart`); checkout requires login and cart survives the login redirect (`/checkout`); places a transactional order and shows a confirmation page (`/orders/{id}/confirmation`).
- **Accounts** — registration (`/register`), Spring Security form login/logout, profile page with order history (`/profile`).
- **Admin backend** (`/admin`, ROLE_ADMIN only) — dashboard counts, product CRUD, order status updates, user enable/disable.
- **Data**: JPA/Hibernate against MySQL/MariaDB, database name `ex4`, 6 related tables (`users`, `categories`, `products`, `orders`, `order_items`, `reviews`).
- Boots cleanly against an **empty** database — an admin account and a sample catalog are seeded automatically on first startup (`AdminSeeder`, `ProductSeeder`), no manual SQL needed.

See [`docs/COURSE_MAPPING.md`](docs/COURSE_MAPPING.md) for how each part of this project maps to the Udemy course material it's based on.

## Compile & run

Requires:

- **JDK 21 or newer** on the `PATH`/`JAVA_HOME` (Maven itself is provided by the wrapper).
- A MySQL/MariaDB server with a database named `ex4` reachable at `localhost:3306` (a `docker-compose.yml` is included for a local MariaDB container: `docker compose up -d`). Default credentials expected by `application.properties`: db `ex4`, user/pass `shopex`/`shopex` (matching the provided `docker-compose.yml`).

**Start the database before building** — the test phase of `clean package` boots the application context, which connects to the database.

```
mvnw clean package
mvnw spring-boot:run
```

Then open `http://localhost:8080`.

## Credentials

- **Seeded admin account**: `admin@shopex.local` / `Admin123!` (created automatically on first boot if no admin exists yet — see `AdminSeeder`).
- Regular accounts are created via `/register`.
- The sample SQL dump at [`docs/ex4_dump.sql`](docs/ex4_dump.sql) also includes a demo customer account: `demo@shopex.local` / `Demo1234!`.

## Sample data / SQL dump

[`docs/ex4_dump.sql`](docs/ex4_dump.sql) contains the full schema plus the seed data the app creates automatically (admin account, 3 categories, 10 products, a couple of reviews), and additionally a demo customer with one sample pending order — so every page, including the admin order management and the profile order history, has data to play with straight after import. Import with:

```
mysql -u root -p ex4 < docs/ex4_dump.sql
```

## Demo recording

_(placeholder — add the recording link here before submission; see [`docs/DEMO_GUIDE.md`](docs/DEMO_GUIDE.md) for the recording plan)_

## Notes for the grader

- Tech stack: Spring Boot 4, Java 21+, Spring Data JPA/Hibernate, Thymeleaf, Spring Security (lambda DSL, database-backed authentication, BCrypt), Maven Wrapper.
- Session usage: the shopping cart is a `@SessionScope` injected bean (`CartService`), not raw `HttpSession` attribute access.
- CSRF protection is enabled (default); all forms use Thymeleaf's `th:action`, which auto-includes the CSRF token.
