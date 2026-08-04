# Course mapping — ShopEx ↔ "Spring Boot 4, Spring 7 & Hibernate for Beginners" (Chad Darby, Udemy)

Purpose: for demo prep and to answer "where did you learn to do this" — maps each part of the ShopEx codebase to the course section that teaches the underlying concept. Section names below match the course's public curriculum outline; exact lecture numbers vary by course edition/updates, so this references section/topic names rather than specific video numbers — check the "Course content" tab on the course page for the current numbering.

| ShopEx feature / file(s) | Course section |
|---|---|
| `pom.xml`, Maven Wrapper (`mvnw`/`mvnw.cmd`), project structure | **Spring Boot Overview** — rapid application setup, Spring Initializr, project layout |
| Constructor injection everywhere (`final` fields, no field `@Autowired`), `@Service`/`@Repository`/`@Component` stereotypes | **Spring Core: Inversion of Control (IoC) & Dependency Injection** — constructor injection, component scanning |
| `CartService` (`@Component @SessionScope`) | **Spring Core: Bean Scopes** — session scope vs singleton/prototype, `@SessionScope` |
| `entity/*.java` (`User`, `Category`, `Product`, `Order`, `OrderItem`, `Review`) + `repository/*.java` (`JpaRepository`, derived query methods like `findByEmail`, `findByNameContainingIgnoreCase`) | **Spring Data JPA / Hibernate CRUD** — `@Entity`, `@Id`/`@GeneratedValue`, `JpaRepository`, derived query methods, `application.properties` datasource config |
| `Product.category` (`@ManyToOne`), `Order.items` / `OrderItem.order` (`@OneToMany`/`@ManyToOne`, `mappedBy`, `cascade`, `orphanRemoval`), `Review.product`/`Review.user` | **JPA Advanced Mappings** — One-to-Many, Many-to-One, cascade types, `mappedBy` |
| `controller/ProductController`, `controller/CartController`, `controller/CheckoutController`, `controller/ProfileController`, `templates/**/*.html`, `fragments/layout.html` (`th:replace`, `th:each`, `th:if`, `th:object`) | **Spring MVC** — `@Controller`, view resolution, Thymeleaf template engine, shared layout fragments |
| Registration form (`AuthController`), product/order/user admin forms (`controller/admin/*`), `@Valid` + `BindingResult`, `th:field`, inline validation error display | **Spring MVC CRUD / Form Handling & Validation** — form binding, Bean Validation (`@NotBlank`, `@Positive`, etc.), `BindingResult`, Thymeleaf form tags |
| `config/SecurityConfig` (`SecurityFilterChain`, lambda DSL, `requestMatchers`, `hasRole`, `formLogin`, `logout`), `PasswordEncoder`/`BCryptPasswordEncoder` | **Spring Security: Configuration & Password Encoding** |
| `User implements UserDetails`, `CustomUserDetailsService`, `AuthController` registration flow, `AdminSeeder` | **Spring Security: Authentication & Authorization** — `UserDetailsService`, database-backed auth, role-based access control |
| `sec:authorize` in `fragments/layout.html`, `hasRole("ADMIN")` on `/admin/**`, review-POST auth check | **Spring Security: Method/View-level authorization** — Thymeleaf Spring Security extras, role gating in views and routes |
| `GlobalExceptionHandler` (`@ControllerAdvice`), `error/404.html`, `error/500.html` | **Spring MVC: Exception Handling** — `@ControllerAdvice`, custom error pages |
| Optional: `controller/api/ProductApiController` (`/api/products/search`), `static/js/search-widget.js` | **Spring REST** — `@RestController`, JSON responses, REST endpoint design (only if the bonus widget is built) |
| *(not used)* | **Aspect-Oriented Programming (AOP)** — not needed for this project's scope; noted here for completeness |

## Notes
- The course's Spring Security section predates some lambda-DSL changes in newer Spring Security majors; ShopEx follows the current lambda-only style (`http.authorizeHttpRequests(auth -> ...)`, no `WebSecurityConfigurerAdapter`, no `.and()` chaining, no `antMatchers()`) per Spring Security's own migration guidance, which is a small deviation from older course recordings using the pre-lambda DSL — worth mentioning at the demo if asked.
- Sessions requirement (exercise spec item 3) is fulfilled by `CartService`'s `@SessionScope` bean, not raw `HttpSession` attribute access — this follows the course's DI philosophy of injecting scoped beans rather than reaching into `HttpServletRequest`/`HttpSession` manually.
