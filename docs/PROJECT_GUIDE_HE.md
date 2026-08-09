# מדריך לפרויקט ShopEx — להבין את המבנה ואיך הכול עובד

המדריך הזה נכתב למי שהנושא טרי אצלו: הוא מסביר מה יש בפרויקט, למה כל קובץ קיים, ואיך בקשה אחת מהדפדפן עוברת דרך כל השכבות. מומלץ לקרוא אותו פעם אחת ברצף, ואז לפתוח את הקבצים לצד הסעיפים.

## 1. מה הפרויקט עושה

ShopEx הוא אתר חנות: גולשים רואים קטלוג מוצרים, מחפשים ומסננים, מוסיפים לעגלה (גם בלי להתחבר), נרשמים ומתחברים, מבצעים הזמנה, כותבים ביקורות, ורואים היסטוריית הזמנות בפרופיל. למנהל (admin) יש ממשק נפרד ב־`/admin` לניהול מוצרים, הזמנות ומשתמשים.

כל הלוגיקה רצה **בצד השרת** (זו הדרישה המרכזית של התרגיל): השרת מקבל בקשה, מריץ לוגיקה, ומחזיר דף HTML מוכן. אין React ואין JavaScript משמעותי — את ה־HTML מייצר מנוע תבניות בשם **Thymeleaf**.

## 2. התמונה הגדולה: מה קורה כשגולש נכנס לדף

כל בקשה עוברת את אותו מסלול. לדוגמה, `GET /products?q=tv`:

```
דפדפן
  │  GET /products?q=tv
  ▼
Spring Security (שרשרת פילטרים)      ← בודק: האם הנתיב הזה מותר למשתמש הזה?
  ▼
DispatcherServlet                     ← "המרכזייה" של Spring MVC, מנתב לפי ה־URL
  ▼
ProductController.list(...)           ← ה־Controller: מקבל פרמטרים, בלי לוגיקה כבדה
  ▼
ProductService.search(...)            ← ה־Service: הלוגיקה העסקית (איזו שאילתה להריץ)
  ▼
ProductRepository (Spring Data JPA)   ← ה־Repository: מתורגם אוטומטית ל־SQL
  ▼
MySQL (סכמה בשם ex4)                  ← הנתונים עצמם
  ▲
  │  התוצאות חוזרות למעלה, ה־Controller שם אותן ב־Model
  ▼
Thymeleaf: templates/products/list.html   ← התבנית + הנתונים ⇒ HTML מוכן
  ▼
דפדפן מקבל דף שלם
```

לזכור את השרשרת הזו — Controller → Service → Repository → DB ובחזרה דרך תבנית — זה 80% מהבנת הפרויקט.

## 3. מבנה התיקיות

```
ex3/
├── pom.xml                  ← הגדרת הפרויקט ל־Maven: אילו ספריות (dependencies) בשימוש
├── mvnw, mvnw.cmd           ← Maven Wrapper: מריץ Maven בלי להתקין אותו
├── docker-compose.yml       ← מרים MariaDB מקומי עם מסד ex4 בפקודה אחת
├── docs/                    ← מסמכים: dump של המסד, מדריכים
└── src/main/
    ├── java/com/internetprog/shopex/
    │   ├── ShopexApplication.java   ← נקודת הכניסה (main)
    │   ├── config/                  ← הגדרות: אבטחה, טיפול בשגיאות, badge של העגלה
    │   ├── entity/                  ← מחלקות שממופות לטבלאות במסד (JPA)
    │   ├── repository/              ← ממשקי גישה למסד (Spring Data)
    │   ├── service/                 ← לוגיקה עסקית + עגלת קניות + זריעת נתונים
    │   └── controller/              ← מקבלים בקשות HTTP ומחזירים שם של תבנית
    │       └── admin/               ← ה־controllers של ממשק הניהול
    └── resources/
        ├── application.properties   ← חיבור למסד והגדרות
        ├── static/css/style.css     ← קובץ עיצוב (מוגש כמו שהוא)
        └── templates/               ← תבניות Thymeleaf (ה"דפים" של האתר)
```

## 4. השכבות, אחת־אחת

### 4.1 Entity — "טבלה בתחפושת של מחלקה"

תיקיית `entity/` מכילה שש מחלקות עם האנוטציה `@Entity`. כל אחת מייצגת טבלה, כל שדה — עמודה. Hibernate (המנוע שמאחורי JPA) יוצר את הטבלאות לבד בזכות `ddl-auto=update`.

| מחלקה | טבלה | קשרים |
|---|---|---|
| `User` | `users` | מממשת גם `UserDetails` של Spring Security (סעיף 5.3) |
| `Category` | `categories` | — |
| `Product` | `products` | `@ManyToOne` ל־Category (להרבה מוצרים אותה קטגוריה) |
| `Order` | `orders` | `@ManyToOne` ל־User, ‏`@OneToMany` לפריטי ההזמנה |
| `OrderItem` | `order_items` | `@ManyToOne` ל־Order ול־Product |
| `Review` | `reviews` | `@ManyToOne` ל־Product ול־User |

דוגמה לקשר (מתוך `Product.java`):

```java
@ManyToOne
@JoinColumn(name = "category_id")   // עמודת המפתח הזר בטבלת products
private Category category;
```

זה בדיוק "מספר טבלאות עם קשרים" שהתרגיל דורש. שימו לב גם ל־`cascade = CascadeType.ALL` על `Order.items` — שמירת הזמנה שומרת אוטומטית גם את הפריטים שלה.

### 4.2 Repository — גישה למסד בלי לכתוב SQL

כל ממשק ב־`repository/` יורש מ־`JpaRepository<Entity, Long>` ומקבל בחינם `findAll` ,`findById` ,`save` ,`delete` ועוד. הקסם האמיתי: **שאילתות נגזרות** — Spring קורא את שם המתודה ובונה ממנו SQL:

```java
Optional<User> findByEmail(String email);                       // WHERE email = ?
Page<Product> findByNameContainingIgnoreCase(String n, Pageable p); // LIKE %..% + עימוד
long countByStatus(String status);                              // SELECT COUNT(*)
```

יש גם שאילתה מפורשת אחת (`@Query`) ב־`ProductRepository` — עדכון מלאי אטומי (סעיף 6.3).

אין שום מימוש שלנו לממשקים האלה — Spring מייצר את המימוש בזמן ריצה והופך כל ממשק ל־**Bean** (סעיף 5.1).

### 4.3 Service — הלוגיקה העסקית

- `ProductService` — חיפוש/סינון/מיון/עימוד של הקטלוג, ורשימת "מוצרים מובחרים" לדף הבית.
- `OrderService` — התהליך הטרנזקציוני של ביצוע הזמנה (סעיף 6.3).
- `ReviewService` — שליפה ושמירה של ביקורות + חישוב ממוצע דירוג.
- `CartService` — **עגלת הקניות**: bean בסקופ session (סעיף 5.2). לא ניגש למסד בכלל.
- `AdminSeeder`, `ProductSeeder` — רצים פעם אחת בעליית השרת ("זריעה", סעיף 6.5).
- `CustomUserDetailsService` — מגשר בין Spring Security לטבלת המשתמשים (סעיף 5.3).

### 4.4 Controller — הדלת של כל דף

Controller הוא מחלקה עם `@Controller`, שבה כל מתודה ממופה ל־URL עם `@GetMapping`/`@PostMapping`. המתודה מקבלת פרמטרים מהבקשה, קוראת ל־service, שמה נתונים ב־`Model`, ומחזירה **שם של תבנית**:

```java
@GetMapping("/{id}")
public String detail(@PathVariable Long id, Model model) {
    Product product = productService.getById(id);   // לוגיקה — בשירות
    model.addAttribute("product", product);          // נתונים לתבנית
    return "products/detail";                        // ⇒ templates/products/detail.html
}
```

מיפוי דפים ↔ controllers: ‏`HomeController` (דף הבית), `ProductController` (קטלוג+ביקורות), `CartController` (עגלה), `CheckoutController` (תשלום ואישור), `AuthController` (הרשמה+התחברות), `ProfileController` (פרופיל), ותחת `admin/` — dashboard, מוצרים, הזמנות, משתמשים.

### 4.5 Templates — Thymeleaf

תבנית Thymeleaf היא HTML רגיל עם אטריביוטים של `th:`:

```html
<div class="card" th:each="product : ${productPage.content}">   <!-- לולאה -->
    <h3 th:text="${product.name}">שם לדוגמה</h3>               <!-- הצבת טקסט -->
    <a th:href="@{/products/{id}(id=${product.id})}">פרטים</a>  <!-- בניית קישור -->
</div>
```

- `fragments/layout.html` — ה־header/footer המשותפים; כל דף מושך אותם עם `th:replace` (כמו include).
- `sec:authorize="isAuthenticated()"` / `hasRole('ADMIN')` — מציג חלקים מהדף רק למי שמורשה (זו הצגה בלבד! האכיפה האמיתית היא ב־SecurityConfig).
- טפסים עם `th:action` מקבלים אוטומטית שדה CSRF נסתר, ו־`th:field` + `th:errors` קושרים שדות לאובייקט ומציגים שגיאות ולידציה.

## 5. שלושת המושגים שחייבים לדעת להסביר

### 5.1 Bean ו־Dependency Injection

**Bean** = אובייקט ש־Spring יוצר ומנהל בשבילנו. כל מה שמסומן `@Component`/`@Service`/`@Controller`/`@Configuration` (או repository) הופך ל־bean יחיד (singleton) כברירת מחדל.

**Dependency Injection**: במקום ש־controller יעשה `new ProductService()`, הוא מצהיר על התלות בקונסטרקטור ו־Spring מספק אותה:

```java
public ProductController(ProductService productService, ReviewService reviewService) {
    this.productService = productService;   // Spring הזריק את ה־beans
    this.reviewService = reviewService;
}
```

בפרויקט אין אף `@Autowired` על שדות, אין אובייקטים סטטיים גלובליים, ואין גישה ידנית ל־request/session — בדיוק כמו שהתרגיל דורש.

### 5.2 Session ו־@SessionScope — העגלה

HTTP הוא חסר זיכרון (stateless). **Session** = זיכרון שהשרת שומר פר־גולש (מזוהה ע"י cookie בשם JSESSIONID).

הדרך ה"נקייה" של Spring להשתמש ב־session היא bean עם `@SessionScope`:

```java
@Component
@SessionScope
public class CartService implements Serializable { ... }
```

Spring יוצר **מופע נפרד לכל session**, ומזריק ל־controllers (שהם singletons) שגריר (proxy) שמפנה בכל בקשה לעגלה של הגולש הנכון. ככה שני גולשים לא רואים אחד את העגלה של השני, ואנחנו לא נוגעים ב־`HttpSession` ידנית.

בונוס שכדאי להגיד בדמו: אחרי login, ‏Spring Security מחליף את מזהה ה־session (הגנה מפני session fixation) אבל **שומר את התוכן** — ולכן העגלה שמולאה כאורח שורדת את ההתחברות.

### 5.3 Spring Security — התחברות והרשאות

הכול מוגדר ב־`config/SecurityConfig.java`:

- `securityFilterChain` קובע **מי רשאי מה** לפי נתיב: סטטיים/קטלוג/עגלה פתוחים לכולם, `checkout`/`profile`/`orders` רק למחוברים, `/admin/**` רק ל־`ROLE_ADMIN`, וכל השאר — למחוברים.
- `formLogin` נותן לנו מסך התחברות ותהליך שלם בחינם; אנחנו סיפקנו רק את התבנית `auth/login.html`.
- איך Spring יודע מי המשתמשים? `CustomUserDetailsService` מממש `loadUserByUsername` ושולף `User` מהמסד לפי אימייל. `User` מממש `UserDetails`, ולכן Spring יודע לקרוא ממנו סיסמה, הרשאות (`ROLE_USER`/`ROLE_ADMIN`) והאם החשבון מושבת.
- סיסמאות נשמרות כ־**BCrypt hash** (ה־bean של `PasswordEncoder`) — אף פעם לא טקסט גלוי.
- ההרשמה ב־`AuthController` היא שלנו: ולידציה, בדיקת אימייל כפול, הצפנת סיסמה, שמירה.
- `DisabledUserFilter` — **פילטר** משלנו בשרשרת האבטחה: Spring בודק `enabled` רק בזמן ההתחברות, ולכן משתמש שהאדמין השבית היה ממשיך לעבוד עד שהסשן היה פג. הפילטר בודק מחדש בכל בקשה ומסיים את הסשן מיד. (זה גם ממלא את סעיף הבונוס בתרגיל: "Use optionally: Interceptors/Filters".)

## 6. הזרימות המרכזיות צעד־אחר־צעד

### 6.1 חיפוש בקטלוג
`GET /products?q=tv&categoryId=1&sort=price&page=0` → ‏`ProductController.list` מעביר הכול ל־`ProductService.search`, שבוחר שאילתה נגזרת מתאימה ומחזיר `Page<Product>` (עימוד של Spring Data). התבנית מציירת גם את טופס החיפוש (עם הערכים שנבחרו) וגם קישורי עמודים שמשמרים את כל הפרמטרים.

### 6.2 עגלה כאורח → התחברות → העגלה שרדה
1. אורח מוסיף מוצר: ‏`POST /cart/add` → ‏`CartService.addItem` שומר שורה **בזיכרון של ה־session** (מעתיק שם ומחיר כדי שהעגלה תהיה יציבה).
2. אורח לוחץ Checkout: ‏Spring Security חוסם (`/checkout` דורש התחברות), שומר את הבקשה המקורית, ומפנה ל־login.
3. אחרי התחברות מוצלחת — Spring מחזיר אוטומטית ל־checkout (מנגנון saved request), והעגלה עדיין שם (סעיף 5.2).

### 6.3 ביצוע הזמנה — טרנזקציה
`OrderService.placeOrder` מסומן `@Transactional` — הכול או כלום:

1. לכל שורת עגלה מריצים `UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?` — **בדיקת המלאי וההורדה קורות בפקודת SQL אחת**, ולכן שני קונים במקביל לא יכולים לקנות את היחידה האחרונה פעמיים.
2. אם ה־UPDATE לא עדכן שורות ⇒ אין מלאי ⇒ נזרקת חריגה ⇒ **rollback** מלא (גם ההורדות שכבר בוצעו לשורות קודמות מתבטלות).
3. נבנה `Order` עם `OrderItem` לכל שורה (כולל `priceAtPurchase` — המחיר בזמן הקנייה), והסכום מחושב מהפריטים עצמם.
4. שמירה אחת (ה־cascade שומר את הפריטים), ניקוי העגלה, והפניה לדף אישור.

### 6.4 ביקורות
`POST /products/{id}/reviews` פתוח רק למחוברים (גם ברמת ה־security וגם בבדיקה בקוד). האובייקט `Review` עובר ולידציה (`@Min(1) @Max(5)` על הדירוג) עם `@Valid` + `BindingResult`; שגיאה מחזירה את הדף עם הודעה ליד השדה.

### 6.5 עלייה על מסד ריק (דרישה מפורשת בתרגיל)
`application.properties` כולל `createDatabaseIfNotExist=true` (יוצר את הסכמה), `ddl-auto=update` (יוצר טבלאות), ושני seeders שמאזינים ל־`ApplicationReadyEvent`: ‏`AdminSeeder` יוצר חשבון אדמין אם אין, ‏`ProductSeeder` זורע קטלוג אם אין מוצרים. לכן אפשר למחוק את המסד לגמרי — והאתר עולה מוכן לעבודה.

### 6.6 טיפול בשגיאות
`GlobalExceptionHandler` (עם `@ControllerAdvice`) תופס חריגות מכל ה־controllers: כתובת לא קיימת ⇒ דף 404 מעוצב, גישה אסורה ⇒ 403, וכל חריגה לא צפויה ⇒ נרשמת ללוג ומוצג דף 500 ידידותי. הדפים עצמם ב־`templates/error/`.

## 7. application.properties בשורה אחת לכל שורה

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ex4?createDatabaseIfNotExist=true...
                                   # איפה המסד, ושייווצר אם איננו
spring.datasource.username/password=shopex/shopex   # פרטי החיבור (תואם ל־docker-compose)
spring.jpa.hibernate.ddl-auto=update # Hibernate יוצר/מעדכן טבלאות לפי ה־entities
spring.jpa.show-sql=true             # מדפיס את ה־SQL לקונסול — נוח להראות בדמו
spring.thymeleaf.cache=false         # בזמן פיתוח: לרענן תבניות בלי הפעלה מחדש
```

## 8. איך מריצים

```bash
docker compose up -d      # להרים מסד (או MySQL מקומי עם סכמה ex4)
./mvnw clean package      # קומפילציה + טסטים (המסד חייב לרוץ — הטסט מרים את האפליקציה)
./mvnw spring-boot:run    # http://localhost:8080
```

אדמין: `admin@shopex.local` / `Admin123!`. ‏dump לדוגמה: `docs/ex4_dump.sql` (כולל לקוח דמו `demo@shopex.local` / `Demo1234!` והזמנה אחת).

## 9. שאלות שסביר שישאלו — ותשובה בשורה

- **איפה ה־MVC?** Model = entities + הנתונים ב־Model, ‏View = תבניות Thymeleaf, ‏Controller = תיקיית controller.
- **איפה שימוש ב־session?** העגלה — bean בסקופ session, מוזרק דרך proxy (לא `HttpSession` ידני).
- **איפה טרנזקציה?** `OrderService.placeOrder` — ‏`@Transactional`, כולל rollback כשאין מלאי.
- **איך מונעים ששני קונים יקנו את הפריט האחרון?** הורדת מלאי מותנית בפקודת UPDATE אחת (אטומי במסד).
- **מה ההבדל בין `sec:authorize` בתבנית ל־SecurityConfig?** התבנית רק מסתירה ויזואלית; האכיפה האמיתית — בשרשרת הפילטרים.
- **למה הסיסמאות ב־BCrypt?** hash חד־כיווני עם salt — גם דליפת מסד לא חושפת סיסמאות.
- **מה קורה על מסד ריק?** נוצר אוטומטית: סכמה (ddl-auto), אדמין וקטלוג (seeders).
- **איך עובד עימוד?** `Pageable`/`Page` של Spring Data — ה־repository מקבל בקשת עמוד ומחזיר עמוד + מטא־נתונים (סה"כ עמודים וכו').
