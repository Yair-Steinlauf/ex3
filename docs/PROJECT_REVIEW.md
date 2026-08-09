# ShopEx — review against the ex3 assignment spec

Reviewed 2026-08-09. Method: full source read, then empirical verification — `mvnw clean package` with JDK 25, app booted against a **completely empty** `ex4` database, and a scripted end-to-end smoke test over HTTP (guest cart → login → checkout → admin, plus error/authorization probes). Everything below marked "verified" was actually executed, not just read.

## Verdict

The project **meets every mandatory requirement** of the exercise, and the core flows all work end-to-end. What remains are a few real but localized bugs (error handling turns 404/403 into 500s), two submission-package problems a grader would hit in the first five minutes (`./mvnw` not executable, wrong demo password in the SQL dump), and missing README items (JDK version, demo video). None require structural changes.

## Update — fixes applied (2026-08-09)

All six bugs below plus the JDK/README gaps were fixed in the commits following this review, and each fix was re-verified against a fresh build and a live run:

- `/products/9999` and unknown URLs now return **404** with the custom page; a foreign order confirmation and `/admin` for a non-admin return **403** with a new custom `error/403.html` (renders both from controller exceptions and from the security filter chain).
- Cart badge is populated on **every** page via `CartModelAdvice` (verified 2 items → badge 2 on `/`, `/products`, `/cart`; 0 after checkout clears the cart).
- `mvnw` is executable in git (mode 100755) — `./mvnw clean package` runs directly.
- Dump demo hash regenerated; bcrypt-verified that `Demo1234!` now matches.
- Home page renders 4 featured products.
- `java.version` lowered to 21; full `./mvnw clean package` re-verified green on a plain JDK 21. README documents the JDK and the start-DB-before-build ordering.

Second round (same day): the optional items were applied too, each verified against a rebuilt app —

- Atomic conditional `UPDATE` for stock (oversell impossible; verified a 9999-unit checkout is rejected with a clean message and no stock change, and that the failure rolls the order back).
- Order total now computed from the order's own lines.
- `CartService` methods synchronized + defensive copy from `getItems()`.
- Registration requires ≥ 8-character passwords (verified server-side rejection).
- Admin dashboard counts pending orders in SQL (`countByStatus`); seeder uses `findByNameIgnoreCase`.
- Removed the explicit `DaoAuthenticationProvider` bean and the redundant `hibernate.dialect` property — both startup warnings are gone; login re-verified.
- `docs/ex4_dump.sql` now ships a demo-customer order (total matches its lines; stock adjusted), so admin orders/dashboard/profile have data right after import.

Deliberately left as-is: the generic "Invalid email or password" message for disabled accounts (it avoids leaking account state) and `spring.jpa.show-sql=true` (handy for showing JPA at work during the demo).

### Third round — full adversarial test pass (same day)

A systematic test pass (authorization matrix over every route × role for GET *and* POST, CSRF, malformed input, XSS, session behaviour, concurrency, UTF-8) found **two broken features that the earlier reviews missed**, because the earlier passes never opened the admin product form and never submitted a *valid* review. Both were pre-existing (commits `93d5f3b` / `aca5ae3`), and both are now fixed:

1. **Admin product create/edit was completely unusable** — `GET /admin/products/new` and `/{id}/edit` returned 500. `product-form.html` used a conditional expression inside a fragment parameter (`head(title=${product.id == null ? ... })`), which Thymeleaf forbids in that restricted context. The title is now computed in the controller (`formTitle`). This also fixes product validation errors, which previously 500'd while re-rendering the same template.
2. **Review submission was broken and could destroy another user's review** — `@ModelAttribute` bound the URI template variable `{id}` onto the `Review` **entity's** id, so `save()` merged instead of inserting: with no matching row it threw `ObjectOptimisticLockingFailureException` (500, nothing saved); with a matching row it silently **overwrote that review** (verified: the admin's review was replaced by another user's). Replaced with a `ReviewForm` DTO, mirroring the existing `RegistrationForm` pattern, so the entity is built server-side and the id always starts null.
3. **Type-mismatch URLs returned 500** (`/products/abc`, `?page=abc`, `?categoryId=abc`, `/orders/abc/confirmation`, `cart/add?productId=abc`) — now 400 with a new `error/400.html`.
4. **A disabled account kept working in its existing session** (Spring Security only checks `isEnabled()` at login). Added `DisabledUserFilter`, which re-checks per request and ends the session — this also satisfies the spec's optional "Interceptors/Filters if relevant" item.
5. **Stored XSS in the admin pages** — product name / user email were interpolated into a `confirm(...)` JS string, and HTML escaping does not protect a JS string context (a `'` in the name broke out and executed). Values now travel via `data-` attributes read through `this.dataset`, with no user data inside the JS literal. Admin-only injection, so severity was low, but the pattern was wrong.

Verified green after the fixes, on a rebuilt app booted against an empty database:

- Authorization matrix: every `/admin` route 403 for a regular user and redirect-to-login for anonymous, on GET **and** POST; no state mutated by the rejected calls.
- CSRF: POST without a token blocked everywhere, including `logout` (session preserved).
- Concurrency: 8 simultaneous checkouts for the last unit → exactly 1 order, stock 0 (never negative), exactly 1 unit sold, and every order's total equals the sum of its lines.
- Sessions: two carts stay isolated, the cart survives login, logout clears it.
- Validation: registration (duplicate/format/mismatch/blank/short password) and product admin (negative price, zero price, negative stock, blank name) all re-render with messages and persist nothing.
- Error pages: 400 / 403 / 404 / 500 all render the custom templates with the right status.
- SQL injection in search has no effect (parameterized); public pages escape HTML correctly; Hebrew/UTF-8 round-trips through `utf8mb4`.
- **Zero unhandled exceptions** in the application log across the entire run.

### Fourth round — best-practice audit and an automated test suite

Checked against published Spring Boot / Spring Security / OWASP guidance (sources at the bottom), then closed the gaps.

| Practice | Status |
|---|---|
| Constructor injection, no field `@Autowired`, no global static state | ✅ throughout |
| Bind forms to DTOs, never to JPA entities | ✅ for registration and reviews. ⚠️ `AdminProductController` still binds `Product` directly — admin-only, and the copy-onto-managed-entity in `update` keeps it safe, but it is the same pattern that caused the review bug |
| `spring.jpa.open-in-view` disabled (avoid hidden queries / N+1 during rendering) | ✅ now disabled; the confirmation page fetches its lines with `@EntityGraph` in one query |
| Transactions around writes; all-or-nothing checkout | ✅ `@Transactional` on `placeOrder`, verified by test |
| Passwords hashed with BCrypt, never stored or logged in clear | ✅ |
| CSRF protection enabled on all state-changing requests | ✅ verified by test, including `logout` |
| Session fixation protection (new session id at login) | ✅ Spring Security default (`changeSessionId`) |
| Session cookie: `HttpOnly` + `SameSite` | ✅ `HttpOnly` from the container, `SameSite=Lax` via `CookieSameSiteSupplier` |
| Baseline security headers | ✅ `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, no-store `Cache-Control` |
| Output escaping / no HTML or JS injection | ✅ `th:text` everywhere; the admin `confirm()` interpolation was removed |
| Server-side validation with feedback, not just client-side | ✅ Bean Validation + `BindingResult` on every form |
| Custom error pages with correct status codes | ✅ 400 / 403 / 404 / 500 |
| Externalised configuration, profiles for test vs. run | ✅ `application.properties` + `application-test.properties` |
| Test pyramid: many fast slice tests, fewer integration tests | ✅ added (below) |
| Tests must not depend on external infrastructure | ✅ added — the suite runs on in-memory H2 |

**Test suite added** — 47 JUnit 5 tests, all green with **MySQL stopped**, so `mvnw clean package` no longer needs a database:

| Class | Kind | Covers |
|---|---|---|
| `ProductRepositoryTest` | `@DataJpaTest` | derived queries, pagination, and the conditional `decrementStock` guard |
| `OrderServiceTest` | `@DataJpaTest` + `@Import` | order creation, stock movement, total = sum of lines, empty-cart and short-stock aborts, cross-line rollback |
| `SecurityAccessControlTest` | `@SpringBootTest` + MockMvc | the anonymous/user/admin route matrix, admin write endpoints, CSRF enforcement, security headers |
| `CartCheckoutFlowTest` | `@SpringBootTest` + MockMvc | guest cart, session isolation, quantity edits, checkout, confirmation rendering, ownership check, oversell rejection |
| `ReviewSubmissionTest` | `@SpringBootTest` + MockMvc | regression: a new review is inserted and an existing one is left untouched; rating range; anonymous blocked |
| `AdminProductFormTest` | `@SpringBootTest` + MockMvc | regression: the form renders; create/update/validation behaviour |
| `ErrorHandlingTest` | `@SpringBootTest` + MockMvc | 404 / 400 / 403 statuses and pages, out-of-range paging, SQL-injection-shaped search input |

Sources consulted: [Spring Boot testing reference](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html), [Spring Security testing reference](https://docs.spring.io/spring-security/site/docs/5.2.x/reference/html/test.html), [Zalando: testing efficiency in Spring Boot](https://engineering.zalando.com/posts/2023/11/mastering-testing-efficiency-in-spring-boot-optimization-strategies-and-best-practices.html), [Vlad Mihalcea: the Open Session In View anti-pattern](https://vladmihalcea.com/the-open-session-in-view-anti-pattern/), [OWASP Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html), [Baeldung: control the session with Spring Security](https://www.baeldung.com/spring-security-session).

### Fifth round — full refactor

Every controller now goes through the service layer; none references a repository. `UserService` is new, `ProductService` and `OrderService` absorbed the admin operations, and each service declares its transaction boundaries (`readOnly` by default, writes explicit). Forms bind to DTOs in a `dto` package — including a `ProductForm` carrying the category as a plain id, which removed the `@InitBinder` `PropertyEditor` the admin form needed. The two controller advices moved to `controller/advice`.

Dead code removed: two unused repository query variants, six unused `setId` methods, `Order.setItems`, and a dead `reviewError` branch. Product deletion now checks for referencing order lines explicitly rather than catching whatever the ORM happens to throw — which, as a test showed, varies with what the persistence context already holds.

Test suite: 47 → **71**, all green with MySQL stopped. Verified live end to end afterwards: seeding on an empty database, guest cart surviving login, checkout moving stock, reviews, admin CRUD, delete refusal with the right message, invalid status rejected, admin self-disable blocked, 400/403/404 pages — zero unhandled exceptions.

Still open: recording + linking the **demo video** — see `docs/DEMO_GUIDE.md`.

## Requirements checklist

| Spec requirement | Status | Evidence |
|---|---|---|
| 1. Full Spring Boot MVC project: Thymeleaf, controllers, beans, DI, server-side logic | ✅ | All views are Thymeleaf; constructor injection everywhere; no JS frameworks, no static mutable state |
| 2. ≥ 5 major Thymeleaf pages | ✅ | ~15 pages: home, catalog, product detail, cart, checkout, confirmation, login, register, profile, 5 admin pages, error pages |
| 3. Use sessions | ✅ verified | `CartService` is a `@SessionScope` bean; guest cart **survives login** (tested: item added anonymously was still there after form login) |
| 4. Beans + DI, no direct scope access, no global statics | ✅ | Session bean injected into singleton controllers via scoped proxy; nothing touches `HttpSession`/`ServletRequest` directly |
| 5. JPA + MySQL db named `ex4`, ≥ 4 repositories with relations | ✅ verified | 6 entities / 6 `JpaRepository` beans; relations: Product→Category, Order→User, Order↔OrderItem, OrderItem→Product, Review→Product+User |
| 6. Spring Security: authentication + authorization + registration | ✅ verified | DB-backed login (BCrypt), `/admin/**` returns **403 for a regular user** (tested), registration + duplicate-email check work (tested) |
| Runs on an empty database | ✅ verified | Dropped and recreated `ex4` empty; app booted in ~6 s, seeded admin + 3 categories + 10 products automatically |
| `mvnw clean package` / `mvnw spring-boot:run` | ✅ verified* | BUILD SUCCESS on JDK 25 — but see gotchas #2, #3, #6 below |

Also verified working: search + category filter + sort + pagination (state preserved in page links), transactional checkout (stock 45→43 after ordering 2, order total exactly 399.98), server-side validation (review with `rating=7` rejected with a rendered field error), order-ownership check (another user cannot read your confirmation), CSRF on every form via `th:action`, self-disable protection in admin users page.

## Bugs found (all reproduced live)

### 1. Custom 404/403 handling is broken — everything becomes a 500
`GlobalExceptionHandler` has a single `@ExceptionHandler(Exception.class)` that returns `error/500`. `@ExceptionHandler` resolvers run **before** Spring's `ResponseStatusException` / default resolvers, so the catch-all swallows exceptions that carry their own status:

- `GET /products/9999` → **500** "Something went wrong" (should be 404; `ProductService.getById` throws `ResponseStatusException(NOT_FOUND)`)
- Another user opening `GET /orders/1/confirmation` → **500** (the access **is** blocked, but the `FORBIDDEN` exception renders as 500)
- Any unknown URL while logged in → **500** (`NoResourceFoundException` swallowed) — so `error/404.html` is effectively unreachable

Fix: add specific handlers ahead of the catch-all, e.g.

```java
@ExceptionHandler(NoResourceFoundException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
public String handleNotFound() { return "error/404"; }

@ExceptionHandler(ResponseStatusException.class)
public String handleStatus(ResponseStatusException ex, HttpServletResponse resp, Model model) {
    resp.setStatus(ex.getStatusCode().value());
    return ex.getStatusCode() == HttpStatus.NOT_FOUND ? "error/404" : "error/500"; // or a dedicated 403 page
}
```

This matters for grading: "custom error pages" is explicitly on the robustness list, and `/products/<wrong-id>` is a two-click test.

### 2. SQL dump: the demo user's documented password is wrong
Both users in `docs/ex4_dump.sql` carry the **same** BCrypt hash. Verified with bcrypt: the hash matches `Admin123!` and does **not** match `Demo1234!`. A grader importing the dump and logging in as `demo@shopex.local / Demo1234!` (as the README instructs) gets "Invalid email or password". Fix: generate a real hash for `Demo1234!` (e.g. `new BCryptPasswordEncoder().encode("Demo1234!")`) and put it in the dump.

### 3. `mvnw` is committed without the executable bit
`git ls-files -s mvnw` → mode `100644`. On the graders' Linux/macOS machines `./mvnw clean package` fails with *Permission denied* — and the spec names that exact command as the required entry point. Fix once, permanently:

```
git update-index --chmod=+x mvnw
git commit -m "Make mvnw executable"
```

### 4. Cart badge in the header is wrong on most pages
The layout shows `${cartCount}`, but only `CartController`/`CheckoutController` put it in the model. Verified: with 1 item in the cart, `/cart` shows badge **1** while `/products` and `/` show **0**. Fix: expose it globally instead of per-controller:

```java
@ControllerAdvice
public class CartModelAdvice {
    private final CartService cart;
    CartModelAdvice(CartService cart) { this.cart = cart; }
    @ModelAttribute("cartCount")
    public int cartCount() { return cart.getItemCount(); }
}
```
(then drop the now-redundant `cartCount` attributes from the two controllers).

### 5. Home page "Featured products" is dead code
`index.html` renders a "Featured products" heading and iterates `${featuredProducts}`, but `HomeController` never populates it — the heading always sits above an empty section. Either populate it (e.g. `productRepository.findTop4ByOrderByCreatedAtDesc()`) or remove the section.

### 6. `mvnw clean package` fails if the database isn't running yet
The single `@SpringBootTest` boots the full context, which connects to MySQL. Verified: with MariaDB stopped, `package` fails in the test phase. Since `clean package` is the graders' first command, make the ordering explicit in the README ("start the database **before** building — the test suite boots the app"), or make the test independent of a live DB.

## Submission-package gaps (from the spec's submission section)

- **Demo recording** — README still has the placeholder. The spec requires a ≤ 12-minute continuous recording with all participants on camera, uploaded to the repo or linked in the README. This is a hard deliverable.
- **JDK version not documented** — the pom sets `java.version=25`. On a machine with JDK 21 the build fails immediately (verified). Add "Requires JDK 25" with a download pointer to the README — or, safer, lower to `21` (LTS); nothing in the code needs 25.
- **Dump has no orders** — the spec asks for "enough data for us to play with". The dump covers users/categories/products/reviews but `orders`/`order_items` are empty, so the admin orders page starts blank. Consider adding one sample order.
- **Leftover AI-tooling traces** — `CheckoutController`'s class comment says the route "is expected to be guarded by SecurityConfig **(owned by another agent)**", and the git history contains several `Merge branch 'worktree-agent-…'` commits. Given the spec's warnings ("do not submit someone else's code", "you may be required to demo … answer any questions"), remove that comment, and be prepared to explain every part of the code at the demo. `docs/COURSE_MAPPING.md` (mapping features to a Udemy course) is your call to keep or drop — it's honest prep material, but it's also the first thing a grader browsing `docs/` will open.

## Smaller robustness notes (grading criteria mention thread-safety, transactions, feedback)

- **Stock decrement race**: two concurrent checkouts can both pass the stock check and oversell. A single atomic guard fixes it: `@Modifying @Query("update Product p set p.stock = p.stock - :q where p.id = :id and p.stock >= :q")` and treat 0 rows as "not enough stock" (or `@Lock(PESSIMISTIC_WRITE)` on the lookup). Likely demo question territory.
- **Order total vs. line prices**: `totalAmount` comes from add-to-cart-time prices, `priceAtPurchase` from checkout-time prices — they diverge if an admin edits a price in between. Compute the total from the created items instead.
- `CartService`'s `ArrayList` isn't synchronized; a double-submit within one session can race. Making the mutators `synchronized` is enough at this scale.
- Registration accepts a 1-character password — add `@Size(min = 8)`.
- A disabled user sees "Invalid email or password", which hides what actually happened; a `failureHandler`/message distinction would improve feedback (minor).
- No `error/403.html`, so forbidden pages fall back to the default unstyled error page.
- Efficiency nits: `AdminDashboardController` loads *all* orders to count pending ones (`countByStatus` derived query does it in SQL); `ProductSeeder.getOrCreateCategory` does `findAll().stream()` instead of a `findByNameIgnoreCase`.
- Config nits: `hibernate.dialect` is auto-detected (startup warns to remove it); Spring Security warns about the explicit `DaoAuthenticationProvider` bean (it works, but the bean can simply be deleted — the `UserDetailsService` + `PasswordEncoder` beans are picked up automatically); consider turning off `spring.jpa.show-sql` for the demo.

## Suggested fix order

1. Error handling (#1) — most grader-visible bug, ~15 lines.
2. Dump demo password (#2) and `mvnw` exec bit (#3) — first-five-minutes blockers.
3. README: JDK requirement, "start DB before building", record + link the demo video.
4. Cart badge (#4) and featured-products section (#5) — quick UI-quality wins.
5. Cleanup: agent comment; optionally the robustness items above.
