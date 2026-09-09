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
- A MySQL/MariaDB server on `localhost:3306`. The app connects as **`shopex` / `shopex`** and creates the `ex4` database itself if it is missing.

That account has to exist before the app starts. **One** of these is enough:

| Your setup | Do this |
|---|---|
| Docker | `docker compose up -d` — the included `docker-compose.yml` creates the database and the account |
| An existing MySQL (XAMPP, WAMP, phpMyAdmin…) | Import [`docs/ex4_dump.sql`](docs/ex4_dump.sql) as an administrator — one step for the account, the schema and sample data |
| You would rather use your own MySQL account | Skip the account and pass yours instead: `mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.username=root --spring.datasource.password=yourpassword"` |

Or create it by hand:

```sql
CREATE USER IF NOT EXISTS 'shopex'@'localhost' IDENTIFIED BY 'shopex';
GRANT ALL PRIVILEGES ON ex4.* TO 'shopex'@'localhost';
FLUSH PRIVILEGES;
```

The database is only needed to **run** the site: the test suite uses an in-memory H2 database, so `mvnw clean package` succeeds without a MySQL server running.

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
- UI: Bootstrap 5, served from `static/vendor/` rather than a CDN so the site looks the same offline. No hand-written CSS or JavaScript at all — the layout adapts across three sizes (one product column on phones, two from `md`, four from `lg`) and the nav collapses into a burger menu.
- Images: the sample catalog's images ship with the project in `static/images/products/` (WebP, ~7 KB each, 69 KB for all ten) instead of being pulled from a placeholder CDN, so nothing depends on the network. Each one places artwork from [Noto Emoji](https://github.com/googlefonts/noto-emoji) (Google, Apache 2.0) on a category-tinted background — see [`ATTRIBUTION.md`](src/main/resources/static/images/products/ATTRIBUTION.md) for the per-file credits and licence. Every `<img>` carries `width`/`height` so the layout never shifts while they load, the catalog grid defers off-screen images with `loading="lazy"`, and the product page marks its hero image `fetchpriority="high"`. Static URLs are content-hashed and sent with a one-year `Cache-Control`, so a repeat visit re-uses the cached files.
- Automated tests: 73 JUnit tests (`mvnw test`) covering the repositories and checkout service as slice tests (`@DataJpaTest`) and the web layer with MockMvc — access-control matrix, CSRF enforcement, the cart/checkout journey, review submission, admin product CRUD, and error pages. They run against in-memory H2 and need no external database.
- Security hardening: a Content-Security-Policy with no `unsafe-inline` (possible because the project has no inline script, style block or style attribute anywhere), cookie-only session tracking so no session id can leak into a URL, and BCrypt password storage. Verified against the running site with a 23-check probe suite covering CSRF, session fixation, IDOR, privilege escalation, stored XSS, SQL injection and overselling.
