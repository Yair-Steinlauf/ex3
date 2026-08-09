# Demo video — recording guide

The spec's hard rules: **max 12 minutes**, **one continuous take** (no editing/cuts), **every team member appears on camera and presents a part**, and the recording (or a link to it) goes in the repo/README. Informal is fine — a Zoom meeting with screen share and your camera tile is exactly what they expect.

## Before you hit record

- [ ] `docker compose up -d` (or your local MySQL) and **reset the database** so the demo starts clean: `DROP DATABASE ex4;` — the app will recreate and seed it, which lets you *show* the empty-DB requirement working.
- [ ] `./mvnw clean package` once in advance so dependencies are cached (a cold Maven download on camera wastes minutes).
- [ ] Two browser windows side by side or on two virtual desktops: a **normal window** (shopper) and an **incognito window** (admin) so you can be logged in as both.
- [ ] Terminal with a large font; browser zoom ~110–125%; notifications off; close unrelated tabs.
- [ ] Have these credentials on a sticky note: `admin@shopex.local / Admin123!`, plus a fresh email you'll register live.
- [ ] Do **one full dry run with a timer**. 12 minutes is shorter than it feels — the dry run tells you what to cut.

## Suggested 12-minute script

Times are cumulative targets. If you're a team of two, a natural split is: person A does 0:00–6:30 (shopper journey), person B does 6:30–12:00 (admin + robustness).

| Time | What to show | Say the magic words (grading criteria) |
|---|---|---|
| 0:00 | Faces on camera: names, IDs, project name, one sentence: "server-rendered e-commerce site" | "Spring Boot MVC with Thymeleaf, JPA/MySQL, Spring Security — no client framework" |
| 0:30 | Terminal: `./mvnw spring-boot:run` against the **empty** database; point at the seeding log lines when they appear | "Runs on an empty database — admin account and catalog are seeded automatically at startup" |
| 1:30 | Home page, featured products; search from the home box | |
| 2:00 | `/products`: search by name, filter by category, sort by price, click page 2; point out the URL keeps all parameters | "Browsing with multiple states — search, filter, sort and pagination combine" |
| 3:00 | Product detail: description, stock, reviews, average rating | |
| 3:30 | **As a guest** (not logged in): add 2 items to the cart, change a quantity, remove one; show the cart badge updating everywhere | "The cart is a session-scoped Spring bean — no login needed" |
| 4:30 | Click checkout → redirected to login. **Register** a new account live; first try a too-short password to show a server-side validation error, then register properly | "Server-side Bean Validation with error feedback" |
| 5:30 | Log in with the new account → land back in checkout **with the cart intact**; place the order → confirmation page | "The session cart survives login — and the order is placed in a single transaction: stock check and decrement are atomic" |
| 6:30 | Profile page: details + order history; open the order | |
| 7:00 | Write a review on a product; show it appears with the recalculated average | |
| 7:30 | Second window: log in as the **admin**. Dashboard counters → products: create a product live, edit a price; show it appears in the shop window | "Full CRUD in the admin backend, role-based access" |
| 9:30 | Admin orders: change the new order's status; refresh the shopper's profile to show it changed. Admin users: disable a user | |
| 10:30 | Robustness minute: visit `/products/9999` → custom 404; try `/admin` in the shopper window → custom 403; (optional) checkout more units than stock → clean error message | "Server-side integrity: access control on routes, custom error pages, no oversell" |
| 11:30 | Wrap-up on camera: one sentence per tech (MVC/Thymeleaf, JPA relations, session bean, Security), where the code lives, thanks | |

## Recording mechanics (Zoom, as the spec suggests)

1. Start a Zoom meeting alone (or with your teammate), cameras **on**.
2. Share the screen; your camera(s) stay as floating tiles — that satisfies "show yourself in the video".
3. Record to computer, **one take**. If you stumble, keep going — informal is allowed, cuts are not.
4. Stop, check the file length is ≤ 12:00 and audio is audible.

## Publishing the recording

- GitHub blocks files over 100 MB, and a 12-minute recording usually exceeds that. Safest: upload as **unlisted on YouTube** (or Google Drive with "anyone with the link"), then put the link in the README's *Demo recording* section.
- Open the link in a private window to confirm it works without your account.
- Commit the README change — that's the actual deliverable.

## Likely follow-up questions (know these answers)

- *Where do you use sessions?* → `CartService` is `@SessionScope`; Spring injects a proxy into singleton controllers. No raw `HttpSession` access anywhere.
- *How does login work?* → Spring Security form login; `CustomUserDetailsService` loads the user by email from JPA; passwords are BCrypt-hashed; roles gate `/admin/**` via `hasRole("ADMIN")`.
- *What happens if two people buy the last unit at once?* → stock is taken with a conditional single-statement `UPDATE ... WHERE stock >= ?` inside the order transaction; the loser gets "Not enough stock" and the whole order rolls back.
- *Why does the cart survive login?* → Spring Security's session-fixation protection changes the session id but keeps the attributes, so the session-scoped cart bean carries over.
- *What are the entity relations?* → Product→Category (many-to-one); Order→User; Order↔OrderItem (one-to-many with cascade); OrderItem→Product; Review→Product+User.
- *How does it run on an empty DB?* → `ddl-auto=update` creates the schema; `AdminSeeder`/`ProductSeeder` listen for `ApplicationReadyEvent` and seed only if missing.
