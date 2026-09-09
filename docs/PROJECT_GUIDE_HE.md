# ShopEx — מדריך לפרויקט

מדריך שנכתב למי שהנושא טרי אצלו. קראו אותו פעם אחת ברצף, ואז פתחו את הקבצים לצד הסעיפים. הוא משקף את מצב הקוד אחרי הריפקטור המלא.

---

## 1. מה האתר עושה

חנות מקוונת. גולש מדפדף בקטלוג, מחפש ומסנן, מוסיף לעגלה **בלי להתחבר**, נרשם, מתחבר, מבצע הזמנה, כותב ביקורת, ורואה היסטוריית הזמנות בפרופיל. למנהל יש ממשק נפרד ב-`/admin` לניהול מוצרים, הזמנות ומשתמשים.

כל הלוגיקה רצה **בצד השרת** — זו הדרישה המרכזית של התרגיל. השרת מקבל בקשה, מריץ לוגיקה, ומחזיר HTML מוכן. אין React, ואין JavaScript משמעותי.

**בקצרה במספרים:** 2,292 שורות Java ב-39 קבצים, 19 תבניות HTML, ו-73 טסטים אוטומטיים. אפס שורות CSS או JavaScript משלנו — כל העיצוב הוא Bootstrap 5. הקובץ הגדול ביותר הוא כ-150 שורות; אפשר לקרוא את הפרויקט כולו בערב אחד.

---

## 2. התמונה הגדולה — מסלול של בקשה אחת

זה הדבר החשוב ביותר להבין. כל בקשה עוברת את אותו מסלול. לדוגמה `GET /products?q=tv`:

```
דפדפן
  │  GET /products?q=tv
  ▼
Spring Security  ← שרשרת פילטרים: האם המסלול הזה מותר למשתמש הזה?
  ▼
DispatcherServlet ← "המרכזייה" של Spring MVC: לאיזה controller לשלוח?
  ▼
ProductController ← מתרגם HTTP: קורא פרמטרים, מחזיר שם תבנית. בלי לוגיקה עסקית.
  ▼
ProductService    ← הלוגיקה העסקית: איזו שאילתה, אילו כללים, איזו טרנזקציה.
  ▼
ProductRepository ← ממשק בלבד; Spring מייצר ממנו SQL אוטומטית.
  ▼
MySQL (סכמה ex4)
  ▲
  │ התוצאות חוזרות למעלה, ה-controller שם אותן ב-Model
  ▼
Thymeleaf: templates/products/list.html  ← תבנית + נתונים ⇒ HTML
  ▼
דפדפן מקבל דף שלם
```

**אם תזכרו רק דבר אחד — תזכרו את השרשרת הזו:** ‏Controller → Service → Repository → DB, וחזרה דרך תבנית.

וכלל שנובע ממנה, שמתקיים בכל הפרויקט בלי יוצא מן הכלל: **שום controller לא נוגע ב-repository.** אם אתם מחפשים כלל עסקי — הוא ב-service. אם אתם מחפשים תרגום של HTTP — הוא ב-controller.

---

## 3. מבנה התיקיות

```
ex3/
├── pom.xml                    ← הגדרת הפרויקט: אילו ספריות בשימוש
├── mvnw / mvnw.cmd            ← Maven Wrapper: מריץ Maven בלי להתקין אותו
├── docker-compose.yml         ← מרים MariaDB עם סכמה ex4 בפקודה אחת
├── docs/                      ← מסמכים + dump של המסד
├── src/main/java/com/internetprog/shopex/
│   ├── ShopexApplication.java     ← נקודת הכניסה (main)
│   ├── config/                    ← אבטחה, פילטר, עוגיות
│   ├── entity/                    ← מחלקות שממופות לטבלאות (JPA)
│   ├── repository/                ← ממשקי גישה למסד (Spring Data)
│   ├── service/                   ← הלוגיקה העסקית
│   ├── dto/                       ← אובייקטי טפסים
│   └── controller/                ← מקבלים בקשות, מחזירים שם תבנית
│       ├── admin/                 ← ה-controllers של ממשק הניהול
│       └── advice/                ← קוד רוחבי לכל ה-controllers
├── src/main/resources/
│   ├── application.properties     ← חיבור למסד והגדרות
│   ├── static/vendor/bootstrap/   ← Bootstrap 5 (CSS+JS) — אין קוד עיצוב משלנו
│   └── templates/                 ← תבניות Thymeleaf = "הדפים"
└── src/test/                      ← 73 טסטים (סעיף 8)
```

התיקיות מסודרות **לפי שכבה**. היתרון: כשאתם יודעים איזו *שכבה* אתם מחפשים, אתם יודעים לאיזו תיקייה ללכת.

---

## 4. השכבות, אחת-אחת

### 4.1 Entity — טבלה בתחפושת של מחלקה

תיקיית `entity/` מכילה שש מחלקות עם `@Entity`. כל אחת = טבלה, כל שדה = עמודה. Hibernate יוצר את הטבלאות לבד.

| מחלקה | טבלה | קשרים |
|---|---|---|
| `User` | `users` | מממשת גם `UserDetails` של Spring Security (סעיף 5.3) |
| `Category` | `categories` | — |
| `Product` | `products` | `@ManyToOne` ל-Category |
| `Order` | `orders` | `@ManyToOne` ל-User, ‏`@OneToMany` לפריטים |
| `OrderItem` | `order_items` | `@ManyToOne` ל-Order ול-Product |
| `Review` | `reviews` | `@ManyToOne` ל-Product ול-User |

דוגמה מ-`Product.java`:

```java
@ManyToOne
@JoinColumn(name = "category_id")   // עמודת המפתח הזר בטבלת products
private Category category;
```

זה בדיוק "טבלאות עם קשרים" שהתרגיל דורש. שימו לב גם ל-`cascade = CascadeType.ALL` על `Order.items` — שמירת הזמנה שומרת אוטומטית גם את השורות שלה.

### 4.2 Repository — גישה למסד בלי לכתוב SQL

כל ממשק יורש מ-`JpaRepository` ומקבל בחינם `findAll` ,`findById` ,`save` ,`delete`. הקסם: **שאילתות נגזרות** — Spring קורא את *שם המתודה* ובונה ממנו SQL:

```java
Optional<User> findByEmail(String email);                            // WHERE email = ?
Page<Product> findByNameContainingIgnoreCase(String n, Pageable p);  // LIKE %..% + עימוד
long countByStatus(String status);                                   // SELECT COUNT(*)
boolean existsByProductId(Long productId);                           // SELECT EXISTS
```

אנחנו לא כותבים שום מימוש — Spring מייצר אותו בזמן ריצה.

יש גם שאילתה מפורשת אחת, ב-`ProductRepository`, וכדאי להבין אותה כי היא הלב של בטיחות הקנייה:

```java
@Modifying
@Query("update Product p set p.stock = p.stock - :quantity where p.id = :id and p.stock >= :quantity")
int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);
```

**הבדיקה וההורדה קורות בפקודת SQL אחת.** לכן שני קונים במקביל לא יכולים לקנות את היחידה האחרונה פעמיים (סעיף 6.3).

### 4.3 Service — הלוגיקה העסקית

| שירות | אחריות |
|---|---|
| `ProductService` | חיפוש/סינון/מיון/עימוד + CRUD של מוצרים לאדמין |
| `OrderService` | ביצוע הזמנה (טרנזקציוני), קריאת הזמנות, עדכון סטטוס |
| `UserService` | הרשמה (כולל גיבוב סיסמה) וניהול משתמשים |
| `ReviewService` | קריאה והוספה של ביקורות, חישוב ממוצע |
| `CartService` | **עגלת הקניות** — bean בסקופ session (סעיף 5.2) |
| `AdminSeeder`, `ProductSeeder` | זריעת נתונים בעליית השרת (סעיף 6.5) |
| `CustomUserDetailsService` | מגשר בין Spring Security לטבלת המשתמשים |

לכל שירות יש **גבולות טרנזקציה מוצהרים**:

```java
@Service
@Transactional(readOnly = true)   // ברירת מחדל: קריאה בלבד
public class ProductService {
    @Transactional                // כתיבה — מוצהר במפורש
    public Product create(ProductForm form) { ... }
```

כך ברור במבט אחד מה משנה נתונים ומה רק קורא.

### 4.4 DTO — למה טופס לא נקשר לישות

ב-`dto/` יש שלוש מחלקות: `RegistrationForm` ,`ReviewForm` ,`ProductForm`. הן נראות כמו הישויות, אז למה הן קיימות?

**כי ספרינג מזריק לאובייקט הטופס גם את משתני ה-URI מהכתובת.** בקוד המקורי טופס הביקורת נקשר ישירות לישות `Review`, ולכן ה-`{id}` מהכתובת `/products/{id}/reviews` נכנס לשדה `Review.id`. אז `save()` ביצע **עדכון של ביקורת קיימת** במקום הוספה — באג אמיתי שהיה בפרויקט ודרס ביקורות של משתמשים אחרים.

עם DTO הבעיה לא קיימת: ל-`ReviewForm` פשוט אין שדה `id`, והישות נבנית בשרת.

ל-`ProductForm` יש יתרון נוסף: הקטגוריה מגיעה כ-`categoryId` פשוט (מספר), ולכן לא צריך את ה-`@InitBinder` עם `PropertyEditor` שהיה בקוד הישן כדי להמיר ערך לישות `Category`. פחות "קסם" להסביר.

### 4.5 Controller — הדלת של כל דף

Controller = מחלקה עם `@Controller`, שכל מתודה בה ממופה ל-URL. היא מתרגמת HTTP ותו לא:

```java
@GetMapping("/{id}")
public String detail(@PathVariable Long id, Model model) {
    populateDetailModel(model, productService.getById(id));  // הלוגיקה בשירות
    model.addAttribute("reviewForm", new ReviewForm());
    return "products/detail";                                 // ⇒ templates/products/detail.html
}
```

מפת הניתוב המלאה:

| URL | Controller | תבנית |
|---|---|---|
| `/` | `HomeController` | `index.html` |
| `/products`, `/products/{id}` | `ProductController` | `products/list.html`, `products/detail.html` |
| `POST /products/{id}/reviews` | `ProductController` | redirect |
| `/cart`, `/cart/add|update|remove` | `CartController` | `cart/view.html` |
| `/checkout`, `/checkout/place` | `CheckoutController` | `checkout/checkout.html` |
| `/orders/{id}/confirmation` | `CheckoutController` | `checkout/confirmation.html` |
| `/login`, `/register` | `AuthController` | `auth/login.html`, `auth/register.html` |
| `/profile` | `ProfileController` | `profile/profile.html` |
| `/admin` | `AdminDashboardController` | `admin/dashboard.html` |
| `/admin/products/**` | `AdminProductController` | `admin/products.html`, `admin/product-form.html` |
| `/admin/orders/**` | `AdminOrderController` | `admin/orders.html` |
| `/admin/users/**` | `AdminUserController` | `admin/users.html` |

ב-`controller/advice/` יש קוד שרץ עבור **כל** ה-controllers:
- `CartModelAdvice` — מוסיף את מונה העגלה לכל דף, כדי שה-badge בכותרת יהיה נכון בכל מקום.
- `GlobalExceptionHandler` — תופס חריגות וממפה אותן לדפי שגיאה (סעיף 6.6).

### 4.6 Templates — Thymeleaf

תבנית Thymeleaf היא HTML רגיל עם אטריביוטים של `th:`:

```html
<div class="card" th:each="product : ${productPage.content}">   <!-- לולאה -->
    <h3 th:text="${product.name}">שם לדוגמה</h3>                <!-- הצבת טקסט -->
    <a th:href="@{/products/{id}(id=${product.id})}">פרטים</a>   <!-- בניית קישור -->
</div>
```

- `fragments/layout.html` — ה-header, ה-footer, תג ה-`<head>` ותגית ה-script המשותפים; כל דף מושך אותם עם `th:replace`. גם ארבעת דפי השגיאה בנויים מפרגמנט משותף אחד, ולכן כל אחד מהם הוא 12 שורות.
- `sec:authorize="hasRole('ADMIN')"` — מציג חלקים רק למורשים. **זו הסתרה ויזואלית בלבד** — האכיפה האמיתית ב-`SecurityConfig`.
- טפסים עם `th:action` מקבלים אוטומטית שדה CSRF נסתר; ‏`th:field` + `th:errors` קושרים שדות ומציגים שגיאות ולידציה.

### 4.7 העיצוב — Bootstrap 5 בלבד

**בפרויקט אין אף שורת CSS או JavaScript שנכתבה בידיים.** לא קובץ עיצוב משלנו, לא בלוק `<style>`, לא אטריביוט `style=`, ולא `onclick`. תיקיית `static/` מכילה בדיוק שני קבצים:

```
static/vendor/bootstrap/css/bootstrap.min.css
static/vendor/bootstrap/js/bootstrap.bundle.min.js
```

**למה מקומית ולא מ-CDN?** כדי שהאתר ייראה זהה גם בלי אינטרנט, וגם ברשת שחוסמת מארחים חיצוניים. אם ה-CDN חסום, אתר שנשען עליו נראה שבור לגמרי.

**התאמה לשלושה גדלי מסך.** הדפוס חוזר בכל הדפים:

```html
<div class="row row-cols-1 row-cols-md-2 row-cols-lg-4 g-4">
```

| מסך | רוחב | עמודות מוצרים | תפריט |
|---|---|---|---|
| מובייל | < 768px | 1 | המבורגר |
| טאבלט | ≥ 768px (`md`) | 2 | המבורגר |
| דסקטופ | ≥ 992px (`lg`) | 4 | מלא |

התפריט מתקפל עם `navbar-expand-lg`, וה-JS של Bootstrap הוא זה שפותח אותו.

**תג שאסור לשכוח:** ב-`<head>` יש `<meta name="viewport" content="width=device-width, initial-scale=1">`. בלעדיו טלפון אמיתי מרנדר את הדף ברוחב וירטואלי של 980px ומקטין — כלומר כל הכללים הרספונסיביים פשוט לא נכנסים לפעולה. זה היה חסר בפרויקט.

**שלושה כללי מבנה של Bootstrap שקל להחמיץ:**

1. **עמודה חייבת להיות ילד ישיר של `.row`.** ‏`col-12 col-lg-8` בתוך `div` רגיל נותן רוחב אבל לא את הריווח — הן נשענות על משתני ה-gutter שה-row מגדיר.
2. **`.invalid-feedback` חייב לבוא מיד אחרי אלמנט עם `.is-invalid`.** ה-CSS מציג אותו בעזרת סלקטור אחים (`~`), ולכן סדר לא נכון = הודעת שגיאה שלעולם לא תיראה.
3. **מודל לא יכול לשבת בתוך `.table-responsive`.** ל-container הזה יש `overflow-x: auto`, שגוזר כל דבר עם `position: fixed` בתוכו. לכן המודלים בדפי הניהול מרונדרים **אחרי** הטבלה, והכפתור בשורה רק מצביע אליהם.

**דיאלוגים ללא JavaScript משלנו.** אישור מחיקת מוצר והשבתת משתמש הם מודלים של Bootstrap שנפתחים הצהרתית:

```html
<button data-bs-toggle="modal" th:attr="data-bs-target=|#deleteProduct${product.id}|">Delete</button>
```

לכל שורה מודל משלה. היתרון: אפס JavaScript משלנו. המחיר: עם 10 מוצרים יש 10 מודלים ב-DOM — זניח בקנה המידה הזה, אבל אם היו מאות מוצרים בעמוד עדיף מודל משותף אחד שמתמלא מה-`data` של הכפתור, וזה כן דורש כמה שורות JS.

### 4.8 תמונות — למה הן נטענות מהר

בגרסה מוקדמת תמונות המוצרים הגיעו מ-`picsum.photos`, שירות תמונות חיצוני. זה עבד, אבל שילם על כל תמונה חיפוש DNS, לחיצת יד TLS ונסיעה לשרת של מישהו אחר — ובלי אינטרנט האתר נראה שבור. עכשיו הן חלק מהפרויקט, ב-`static/images/products/`.

חמישה דברים עושים את ההבדל, וכולם best practice מקובל:

**1. פורמט WebP במידה הנכונה.** כל תמונה 800×600 (כפול מהמשבצת של 400×300, כדי להישאר חדה במסכי retina) ושוקלת בערך 7KB — 69KB לכל עשר. שליחת JPEG בגודל 2000px למשבצת של 400px היא הטעות הנפוצה ביותר בנושא.

התמונות עצמן הן הרכבה: רקע בגוון הקטגוריה שצויר לפרויקט, ועליו איור מתוך **Noto Emoji** של Google. בחרנו את המקור הזה בגלל הרישיון — ‏**Apache 2.0**, שמתיר שימוש, שינוי והפצה תמורת שימור הודעת הרישיון בלבד, בלי חובת ייחוס בכל דף. הקרדיטים המלאים ב-`static/images/products/ATTRIBUTION.md`. אלה ממלאי-מקום להדגמה ולא צילומי מוצר אמיתיים; להחלפה — פשוט דורסים את קבצי ה-`.webp` באותם שמות.

**2. ‏`width` ו-`height` על כל `<img>`.** זה **לא** קובע את הגודל על המסך — Bootstrap עושה את זה. זה נותן לדפדפן את **יחס הצדדים** מראש, כדי שישמור את המקום לתמונה לפני שהיא מגיעה. בלי זה הטקסט "קופץ" למטה כשכל תמונה נטענת, וזו התזוזה שנמדדת ב-CLS. הפרויקט מודד **CLS = 0** בכל הדפים.

**3. ‏`loading="lazy"` בקטלוג בלבד.** בדף הקטלוג רוב התמונות מתחת לקו הקיפול, ולכן אין טעם להוריד אותן לפני שגוללים. במובייל, שם הדף בגובה 5391 פיקסלים, זה חוסך 6 מתוך 10 בקשות בטעינה הראשונה. בדסקטופ הדף קצר (1702px) והדפדפן טוען מראש הכול — וזה בסדר.

> חשוב: בדף הבית **אין** ‏`lazy`. יש שם רק 4 מוצרים והם בדיוק בקו הקיפול, וטעינה עצלה של התמונה הגדולה שהמשתמש רואה ראשונה **מאטה** את ה-LCP במקום לזרז. `lazy` על תמונה שמעל קו הקיפול היא טעות מוכרת.

**4. ‏`fetchpriority="high"` בדף המוצר.** שם התמונה היא האלמנט הגדול בעמוד, כלומר ה-LCP עצמו — אז אומרים לדפדפן להביא אותה ראשונה. `decoding="async"` בכל המקומות מונע מפענוח התמונה לחסום את הרינדור.

**5. כתובות עם חתימת תוכן ומטמון לשנה.** ב-`application.properties`:

```properties
spring.web.resources.chain.strategy.content.enabled=true
spring.web.resources.cache.cachecontrol.max-age=365d
```

הכתובת שנשלחת לדפדפן היא `/images/products/tv-5f850dbf...webp` — ה-hash מחושב מתוכן הקובץ. לכן אפשר לבקש מהדפדפן לשמור אותה **שנה** בלי סיכון: אם נשנה את התמונה, ה-hash ישתנה, הכתובת תהיה אחרת, והדפדפן יוריד מחדש. זה מה שמאפשר לביקור חוזר להגיע ל-**10 מתוך 10 תמונות מהמטמון, אפס בתים מהרשת**.

כדי שזה יעבוד התבנית חייבת לעטוף את הכתובת ב-`@{...}`:

```html
<img th:src="@{${product.imageUrl}}" th:alt="${product.name}"
     width="400" height="300" loading="lazy" decoding="async"/>
```

ביטוי הקישור `@{...}` הוא זה שמזריק את ה-hash. אם מנהל מזין בטופס כתובת חיצונית מלאה, Thymeleaf מזהה שהיא אבסולוטית ומעביר אותה כמו שהיא — כך שגם זה עדיין עובד.

ולבסוף `th:alt="${product.name}"` במקום `alt=""`: שם המוצר במקום כלום, בשביל קורא מסך ובשביל המקרה שהתמונה לא נטענת.

---

## 5. שלושת המושגים שחייבים לדעת להסביר

### 5.1 Bean והזרקת תלויות

**Bean** = אובייקט ש-Spring יוצר ומנהל. כל מה שמסומן `@Component`/`@Service`/`@Controller`/`@Configuration` (וכל repository) הופך ל-bean.

**הזרקת תלויות**: במקום `new ProductService()`, ה-controller מצהיר על התלות בקונסטרקטור ו-Spring מספק אותה:

```java
public ProductController(ProductService productService, ReviewService reviewService) {
    this.productService = productService;   // Spring הזריק
    this.reviewService = reviewService;
}
```

בפרויקט אין אף `@Autowired` על שדה, אין אובייקטים סטטיים גלובליים, ואין גישה ידנית ל-request/session — בדיוק כפי שהתרגיל דורש.

### 5.2 Session ו-`@SessionScope` — העגלה

HTTP הוא חסר זיכרון. **Session** = זיכרון שהשרת שומר פר-גולש, מזוהה בעוגיית `JSESSIONID`.

הדרך הנקייה של Spring היא bean בסקופ session:

```java
@Component
@SessionScope
public class CartService implements Serializable { ... }
```

Spring יוצר **מופע נפרד לכל גולש**, ומזריק ל-controllers (שהם singletons) שגריר שמפנה בכל בקשה לעגלה הנכונה. שני גולשים לא רואים זה את עגלתו של זה, ואנחנו לא נוגעים ב-`HttpSession` ידנית.

בונוס להסבר: אחרי login, ‏Spring Security מחליף את מזהה ה-session (הגנה מפני session fixation) אבל **שומר את התוכן** — ולכן העגלה שמולאה כאורח שורדת את ההתחברות.

### 5.3 Spring Security — הזדהות והרשאות

הכול ב-`config/SecurityConfig.java`, וזה **המקום היחיד** שמחליט מי נכנס לאן:

```java
.requestMatchers("/", "/login", "/register").permitAll()
.requestMatchers("/cart", "/cart/**").permitAll()        // קניות בלי התחברות
.requestMatchers("/checkout", "/checkout/**").authenticated()
.requestMatchers("/admin/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

- **איך משיגים את המשתמש המחובר?** דרך אחת בלבד, בכל הקונטרולרים:
  ```java
  public String profile(@AuthenticationPrincipal User currentUser, Model model) { ... }
  ```
  זו הדרך שספרינג ממליץ עליה. שליפה ידנית דרך `SecurityContextHolder`, או ערבוב `Principal` עם `Authentication`, נחשבים דפוסים ישנים. זה עובד כי `User` מממשת `UserDetails` — מה שיושב בסשן אחרי ההתחברות זו הישות שלנו עצמה.
- **אף controller לא בודק בעצמו אם המשתמש מחובר.** אם מסלול דורש התחברות, ה-controller מניח שיש משתמש.
- סיסמאות נשמרות כ-**BCrypt hash** — אף פעם לא טקסט גלוי.
- `DisabledUserFilter` — **פילטר** משלנו: ספרינג בודק `enabled` רק בזמן ההתחברות, ולכן משתמש שהאדמין השבית היה ממשיך לעבוד עד שהסשן פג. הפילטר בודק מחדש בכל בקשה ומסיים את הסשן מיד. (זה גם ממלא את סעיף הבונוס בתרגיל: *"Use optionally: Interceptors/Filters"*.)

### 5.4 מה נבדק בפועל מבחינת אבטחה

הרצנו על האתר החי סוללת בדיקות תקיפה — לא רק "יש Spring Security אז זה בטוח". **23 בדיקות עברו.** מה שנבדק ולמה:

| התקפה | איך נבדקה | התוצאה |
|---|---|---|
| **CSRF** | POST בלי טוקן, ועם טוקן מזויף | 403 בשניהם |
| **CSRF על התנתקות** | ‏`GET /logout` | לא מנתק — התנתקות דורשת POST |
| **קיבוע סשן** (session fixation) | השוואת מזהה הסשן לפני ואחרי התחברות | המזהה מוחלף |
| **הסלמת הרשאות** | שליחת `role=ADMIN` בטופס ההרשמה | נשמר `USER` — הטופס נקשר ל-DTO |
| **הסלמת הרשאות** | משתמש רגיל פונה ל-4 דפי ניהול | 403 בכולם |
| **IDOR** | משתמש א׳ מבקש `/orders/{id}` של משתמש ב׳ | 403; הזמנה לא קיימת 404 |
| **XSS מאוחסן** | ‏`<script>` ו-`onerror=` בביקורת ובשם מוצר | נשמר, ומוצג מקודד (`&lt;script&gt;`) |
| **SQL Injection** | ‏`' OR 1=1--` בשדה החיפוש | נחשב כטקסט; הטבלה שלמה |
| **מכירת יתר** | הזמנת 99 יחידות ממלאי 2 | נדחתה; המלאי נשאר 2, לא שלילי |
| **מניפולציית מחיר** | מחיר בעגלה מול מחיר במסד | ההזמנה משתמשת ב-`product.getPrice()` מהמסד |
| **מניית משתמשים** | אימייל לא קיים מול סיסמה שגויה | אותה הודעה בדיוק |
| **דליפת מידע** | דפי 400/404/405 | אפס stack traces, אפס שמות מחלקות |
| **גניבת סיסמאות** | קריאת עמודת `password` | ‏BCrypt, ולא מוצגת בשום דף |

**שלושה ליקויים אמיתיים נמצאו ותוקנו:**

**1. מזהה הסשן דלף לתוך כתובות URL.** כשדפדפן עדיין לא החזיר עוגייה, שרת הסרוולטים נופל אחורה ומדביק `;jsessionid=...` לכתובת. ראינו את זה בשטח:

```
Location: http://localhost:8080/login;jsessionid=F4AAFD6C23583FC5FC70C7B770703204
```

זה מזהה סשן חי, והוא נכנס להיסטוריית הדפדפן, ללוגים של פרוקסי, ולכותרת `Referer` שנשלחת לאתרים אחרים. מי שמשיג אותו מתחזה למשתמש. התיקון, שורה אחת:

```properties
server.servlet.session.tracking-modes=cookie
```

**2. שיטת HTTP שגויה החזירה 500.** פנייה ב-GET לנתיב שמוגדר `@PostMapping` (קישור שמור, סורק אוטומטי) נפלה ל-handler הכללי, החזירה **500** וכתבה stack trace מלא ברמת ERROR. זו טעות של הלקוח, לא תקלת שרת — ומעבר להטעיה, כל סורק אוטומטי היה מנפח את הלוג. הוספנו handler ל-`HttpRequestMethodNotSupportedException` ודף `error/405`.

**3. סיסמת האדמין נכתבה ללוג** — פעמיים, גם דרך ה-logger וגם ב-`System.out.println` שנשאר מגרסה קודמת. קבצי לוג מועתקים ונשלחים; הסיסמה מתועדת ב-README וזה מספיק.

**ובנוסף — הוספנו Content-Security-Policy.** זו ההגנה השנייה מפני XSS: גם אם סקריפט זר איכשהו נכנס לדף, הדפדפן יסרב להריץ אותו.

```
default-src 'self'; script-src 'self'; style-src 'self';
img-src 'self' https: data:; form-action 'self';
base-uri 'self'; object-src 'none'; frame-ancestors 'none'
```

שימו לב למה **שאין** כאן: אין `'unsafe-inline'`. רוב האתרים נאלצים להוסיף אותו — ואז המדיניות כמעט חסרת ערך, כי בדיוק סקריפט מוזרק הוא inline. אנחנו יכולנו לוותר עליו **בגלל ההחלטה מסעיף 4.7**: אין בפרויקט אף `<script>` פנימי, אף `<style>`, ואף `style="..."` — הכול מגיע מקבצי Bootstrap חיצוניים. בדקנו בדפדפן אמיתי שהמדיניות לא שוברת כלום: **אפס הפרות CSP, אפס שגיאות JS**, והמודלים ותפריט ההמבורגר עובדים.

זו דוגמה יפה לכך שהחלטת עיצוב אחת (בלי CSS/JS משלנו) שילמה דיבידנד באבטחה.

---

## 6. הזרימות המרכזיות

### 6.1 חיפוש בקטלוג
`GET /products?q=tv&categoryId=1&sort=price&page=0` → ‏`ProductController.list` מעביר הכול ל-`ProductService.search`, שבוחר שאילתה נגזרת מתאימה ומחזיר `Page<Product>`. התבנית מציירת את טופס החיפוש עם הערכים שנבחרו, וקישורי עמודים ששומרים את כל הפרמטרים.

### 6.2 עגלה כאורח → התחברות → העגלה שרדה
1. אורח מוסיף מוצר: `CartService.addItem` שומר שורה **בזיכרון הסשן** (מעתיק שם ומחיר, כדי שהעגלה תהיה יציבה גם אם המחיר ישתנה).
2. אורח לוחץ Checkout: ‏Spring Security חוסם, שומר את הבקשה, ומפנה ל-login.
3. אחרי התחברות — Spring מחזיר אוטומטית ל-checkout, והעגלה עדיין שם.

### 6.3 ביצוע הזמנה — הטרנזקציה
`OrderService.placeOrder` מסומן `@Transactional` — הכול או כלום:

1. לכל שורה: `UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?` — בדיקה והורדה בפקודה אחת.
2. אם ה-UPDATE לא עדכן שורות ⇒ אין מלאי ⇒ חריגה ⇒ **rollback מלא**, כולל הורדות שכבר בוצעו לשורות קודמות.
3. נבנה `Order` עם `OrderItem` לכל שורה, כולל `priceAtPurchase` — המחיר בזמן הקנייה.
4. הסכום מחושב **מהשורות עצמן**, ולכן תמיד תואם להן.
5. שמירה אחת (cascade שומר את השורות), ניקוי העגלה, והפניה לדף אישור.

זה עומד במבחן אמיתי: 8 קניות במקביל על היחידה האחרונה ⇒ בדיוק הזמנה אחת מצליחה, המלאי מגיע ל-0 ואף פעם לא לשלילי.

### 6.4 ביקורות
פתוח רק למחוברים (נאכף ב-`SecurityConfig`). ה-DTO עובר ולידציה (`@Min(1) @Max(5)`), ושגיאה מחזירה את הדף עם הודעה ליד השדה.

### 6.5 עלייה על מסד ריק — דרישה מפורשת בתרגיל
`createDatabaseIfNotExist=true` יוצר את הסכמה, `ddl-auto=update` יוצר טבלאות, ושני seeders מאזינים ל-`ApplicationReadyEvent`: ‏`AdminSeeder` יוצר חשבון אדמין אם אין, ‏`ProductSeeder` זורע קטלוג אם אין מוצרים. אפשר למחוק את המסד לגמרי — והאתר עולה מוכן.

### 6.6 טיפול בשגיאות
`GlobalExceptionHandler` מחזיר את **הסטטוס הנכון** עם דף מעוצב:

| מצב | דוגמה | תוצאה |
|---|---|---|
| משאב לא קיים | `/products/9999` | 404 |
| ערך שלא ניתן להמיר | `/products/abc`, `?page=abc` | 400 |
| גישה אסורה | משתמש רגיל ב-`/admin` | 403 |
| חריגה לא צפויה | — | נרשם ללוג, מוצג 500 ידידותי |

הסדר חשוב: מטפל כללי אחד ל-`Exception` היה "בולע" גם חריגות שנושאות סטטוס משלהן והופך הכול ל-500, ולכן יש מטפלים ייעודיים **לפני** הכללי.

---

## 7. application.properties — שורה-שורה

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ex4?createDatabaseIfNotExist=true...
                                     # איפה המסד, וייווצר אם איננו
spring.datasource.username/password=shopex/shopex   # תואם ל-docker-compose
spring.jpa.hibernate.ddl-auto=update # Hibernate יוצר/מעדכן טבלאות לפי הישויות
spring.jpa.show-sql=true             # מדפיס SQL לקונסול — נוח להדגמה
spring.jpa.open-in-view=false        # ראו הסבר למטה
spring.web.resources.chain.strategy.content.enabled=true
                                     # חתימת תוכן בכתובת של כל נכס סטטי
spring.web.resources.cache.cachecontrol.max-age=365d
                                     # ולכן מותר לבקש מטמון לשנה — סעיף 4.8
server.servlet.session.tracking-modes=cookie
                                     # שלא ידלוף ;jsessionid= לכתובות — סעיף 5.4
```

**איפה `spring.thymeleaf.cache`?** בכוונה לא נמצא כאן. בגרסה קודמת היה `=false`, וזה נוח בפיתוח אבל נשאר כך גם ב-jar המוכן — כלומר בייצור ספרינג פירסר מחדש כל תבנית בכל בקשה. מחקנו את השורה: `devtools` נמצא ב-classpath רק בפיתוח ומכבה את המטמון שם לבד, והוא **לא** נכנס ל-jar — כך שבפיתוח התבניות מתרעננות ובייצור הן במטמון, בלי להגיד כלום.

**מה זה `open-in-view`?** כברירת מחדל ספרינג משאיר את חיבור ה-JPA פתוח גם בזמן ציור התבנית, כדי שאפשר יהיה "לשלוף עוד" באמצע ה-HTML. זה נוח אבל נחשב anti-pattern: החיבור מוחזק זמן ארוך ונוצרות שאילתות נסתרות (בעיית N+1). כיבינו אותו, ולכן מה שהתבנית צריכה נשלף מראש — למשל `findWithItemsById` מביא הזמנה יחד עם השורות והמוצרים בשאילתה אחת (`@EntityGraph`).

בנוסף, `WebCookieConfig` מסמן את עוגיית הסשן ב-`SameSite=Lax`, לפי המלצת OWASP.

---

## 8. הטסטים

73 טסטים תחת `src/test/`, רצים על **H2 בזיכרון** — כלומר `./mvnw clean package` עובד גם כשה-MySQL כבוי. ההפרדה בפרופיל: `@ActiveProfiles("test")` טוען את `application-test.properties`.

**א. טסטי "פרוסה" (slice) — מרימים רק שכבה אחת, ולכן מהירים:**

| קובץ | מה נבדק |
|---|---|
| `ProductRepositoryTest` | שאילתות נגזרות, עימוד, ו-`decrementStock` שמוריד מלאי רק כשיש מספיק |
| `ProductServiceTest` | CRUD, ולידציה, וסירוב למחוק מוצר שיש לו הזמנות |
| `OrderServiceTest` | הזמנה, מלאי, סכום=שורות, rollback, בעלות על הזמנה, עדכון סטטוס |
| `UserServiceTest` | הרשמה עם גיבוב סיסמה, אימייל תפוס, ואיסור על אדמין להשבית את עצמו |

**ב. טסטי שכבת ווב עם `MockMvc`** — בקשות HTTP מדומות בלי שרת אמיתי:

| קובץ | מה נבדק |
|---|---|
| `SecurityAccessControlTest` | מטריצת הרשאות מלאה, אכיפת CSRF, כותרות אבטחה, ו-CSP ללא `unsafe-inline` |
| `CartCheckoutFlowTest` | המסע המלא: עגלת אורח → בידוד סשנים → checkout → דף אישור → בעלות |
| `ReviewSubmissionTest` | שביקורת חדשה מתווספת ולא דורסת קיימת |
| `AdminProductFormTest` | שהטופס נפתח, ושולידציה מציגה שגיאות בלי לשמור זבל |
| `ErrorHandlingTest` | 404 / 400 / 403 / 405 מחזירים סטטוס ודף נכונים |

שני כלים ששווה להכיר: `.with(user(someUser))` מריץ בקשה בתור משתמש מסוים, ו-`.with(csrf())` מצרף token תקין — בלעדיו הבקשה נדחית, וזה בדיוק מה שרוצים לבדוק.

הרצה: `./mvnw test`

---

## 9. איך מריצים

```bash
docker compose up -d      # מסד (או MySQL מקומי עם סכמה ex4)
./mvnw clean package      # קומפילציה + טסטים (לא צריך מסד — הטסטים על H2)
./mvnw spring-boot:run    # http://localhost:8080
```

דרוש **JDK 21 ומעלה**. אדמין: `admin@shopex.local` / `Admin123!`. ‏dump לדוגמה: `docs/ex4_dump.sql`, כולל לקוח דמו `demo@shopex.local` / `Demo1234!` והזמנה אחת.

---

## 10. שאלות שסביר שישאלו — תשובה בשורה

- **איפה ה-MVC?** Model = ישויות + הנתונים ב-`Model`, ‏View = תבניות Thymeleaf, ‏Controller = תיקיית `controller`.
- **איפה שימוש ב-session?** העגלה — bean בסקופ session, מוזרק דרך proxy.
- **איפה טרנזקציה?** `OrderService.placeOrder`, כולל rollback כשאין מלאי.
- **איך מונעים ששני קונים יקנו את הפריט האחרון?** הורדת מלאי מותנית בפקודת UPDATE אחת — אטומי במסד.
- **למה controller לא ניגש ל-repository?** כדי שלכל כלל עסקי יהיה מקום אחד ויחיד. אין חוץ מזה.
- **למה טופס נקשר ל-DTO ולא לישות?** כי ספרינג מזריק לטופס גם משתני URI; קשירה לישות `Review` גרמה ל-`save()` לעדכן ביקורת קיימת במקום להוסיף.
- **איך מקבלים את המשתמש המחובר?** `@AuthenticationPrincipal User currentUser` — דרך אחת בכל מקום.
- **מה ההבדל בין `sec:authorize` ל-SecurityConfig?** התבנית מסתירה ויזואלית; האכיפה בשרשרת הפילטרים.
- **למה BCrypt?** hash חד-כיווני עם salt — גם דליפת מסד לא חושפת סיסמאות.
- **למה כיביתם `open-in-view`?** כדי שלא ייווצרו שאילתות נסתרות בזמן ציור ה-HTML.
- **מה קורה על מסד ריק?** נוצר אוטומטית: סכמה, אדמין וקטלוג.
- **איך עובד עימוד?** `Pageable`/`Page` של Spring Data — בקשת עמוד פנימה, עמוד + מטא-נתונים החוצה.
- **איך יודעים שהכול עובד?** 73 טסטים אוטומטיים (`./mvnw test`) — סעיף 8, ובנוסף סוויטת בדיקות בדפדפן אמיתי.
- **איפה קובץ ה-CSS שלכם?** אין כזה. כל העיצוב מגיע ממחלקות Bootstrap 5, וגם הדיאלוגים הם מודלים של Bootstrap — אפס JavaScript משלנו.
- **למה Bootstrap מקומי ולא CDN?** כדי שהאתר ייראה זהה בלי אינטרנט או ברשת שחוסמת מארחים חיצוניים. מאותה סיבה גם תמונות המוצרים מקומיות ולא משירות תמונות חיצוני.
- **איך דאגתם שהתמונות ייטענו מהר?** WebP במידה הנכונה (‏69KB לכל עשר), ‏`width`/`height` שמונעים תזוזת פריסה (‏CLS=0), ‏`loading="lazy"` בקטלוג, ‏`fetchpriority="high"` בדף המוצר, וכתובות עם חתימת תוכן שמאפשרות מטמון לשנה — סעיף 4.8.
- **איך האתר מתאים לשלושה גדלי מסך?** ‏`row-cols-1 row-cols-md-2 row-cols-lg-4` לרשת המוצרים, ו-`navbar-expand-lg` לתפריט שמתקפל להמבורגר — סעיף 4.7.
- **בדקתם אבטחה, או רק הנחתם ש-Spring Security מספיק?** בדקנו על האתר החי: 23 בדיקות תקיפה — CSRF, קיבוע סשן, IDOR, הסלמת הרשאות, XSS מאוחסן, SQL injection ומכירת יתר. שלושה ליקויים אמיתיים נמצאו ותוקנו, והוספנו CSP. הכול בסעיף 5.4.
- **מה זה CSP ולמה שלכם חזק?** כותרת שאומרת לדפדפן מאילו מקורות מותר לטעון קוד. שלנו בלי `'unsafe-inline'` — מה שרוב האתרים נאלצים להוסיף ובכך מרוקנים אותה מתוכן — כי אין בפרויקט אף שורת JS או CSS פנימית.
