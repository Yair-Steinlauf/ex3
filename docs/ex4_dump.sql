-- ShopEx (ex3) — sample SQL dump for MySQL/MariaDB database "ex4"
-- Matches the JPA entities under com.internetprog.shopex.entity exactly.
-- The app also auto-seeds this same admin account + catalog on first boot against
-- an empty database (see AdminSeeder / ProductSeeder) — this dump is provided so the
-- data can be inspected directly in a DB tool without running the app first.
--
-- Import with e.g.:
--   mysql -u root -p ex4 < ex4_dump.sql

CREATE DATABASE IF NOT EXISTS ex4;
USE ex4;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name  VARCHAR(255) NOT NULL,
    last_name   VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(255) NOT NULL DEFAULT 'USER',
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categories (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE products (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    description  VARCHAR(2000),
    price        DECIMAL(19,2) NOT NULL,
    stock        INT NOT NULL DEFAULT 0,
    image_url    VARCHAR(255),
    category_id  BIGINT,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE orders (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    order_date    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status        VARCHAR(255) NOT NULL DEFAULT 'PENDING',
    total_amount  DECIMAL(19,2) NOT NULL,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE order_items (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id           BIGINT NOT NULL,
    product_id         BIGINT NOT NULL,
    quantity           INT NOT NULL,
    price_at_purchase  DECIMAL(19,2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE reviews (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    rating      INT NOT NULL,
    comment     VARCHAR(1000),
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Seed admin account: admin@shopex.local / Admin123!  (real BCrypt hash, verified to match)
INSERT INTO users (first_name, last_name, email, password, role, enabled) VALUES
('Shop', 'Admin', 'admin@shopex.local', '$2a$10$oHO29aAwW6KGXQQQ27Ox6.fvsZa/OF/ywLG238LGohQJFRqyZNim.', 'ADMIN', TRUE);

-- Sample demo customer: demo@shopex.local / Demo1234!  (real BCrypt hash, verified to match)
INSERT INTO users (first_name, last_name, email, password, role, enabled) VALUES
('Demo', 'Customer', 'demo@shopex.local', '$2a$10$9vsgowK8yw14ApKpDsXuQOtpYnBcbd5XEm/5YsGRiGPK3Y88Z8/Ae', 'USER', TRUE);

INSERT INTO categories (name) VALUES
('Electronics'), ('Home & Kitchen'), ('Books');

INSERT INTO products (name, description, price, stock, image_url, category_id) VALUES
('Wireless Noise-Cancelling Headphones', 'Over-ear Bluetooth headphones with active noise cancellation and 30-hour battery life.', 199.99, 45, 'https://picsum.photos/seed/headphones/400/300', 1),
('4K Ultra HD Smart TV - 55 inch', '55-inch 4K smart television with HDR support and built-in streaming apps.', 549.00, 20, 'https://picsum.photos/seed/tv/400/300', 1),
('Mechanical Keyboard RGB', 'Compact mechanical keyboard with hot-swappable switches and per-key RGB lighting.', 89.90, 60, 'https://picsum.photos/seed/keyboard/400/300', 1),
('Wireless Ergonomic Mouse', 'Ergonomic wireless mouse with adjustable DPI and silent clicks.', 34.50, 100, 'https://picsum.photos/seed/mouse/400/300', 1),
('Stainless Steel Cookware Set', '10-piece stainless steel cookware set, dishwasher safe and induction compatible.', 129.99, 25, 'https://picsum.photos/seed/cookware/400/300', 2),
('Programmable Coffee Maker', '12-cup programmable drip coffee maker with reusable filter and auto shut-off.', 54.99, 40, 'https://picsum.photos/seed/coffee/400/300', 2),
('Robot Vacuum Cleaner', 'Smart robot vacuum with mapping navigation and app control.', 249.00, 15, 'https://picsum.photos/seed/vacuum/400/300', 2),
('Non-Stick Frying Pan Set', '3-piece non-stick frying pan set in multiple sizes, PFOA-free coating.', 39.99, 80, 'https://picsum.photos/seed/pan/400/300', 2),
('Clean Code: A Handbook of Agile Software Craftsmanship', 'Classic software engineering book on writing maintainable, readable code.', 42.00, 50, 'https://picsum.photos/seed/cleancode/400/300', 3),
('Designing Data-Intensive Applications', 'In-depth guide to the architecture of modern data systems.', 48.50, 35, 'https://picsum.photos/seed/ddia/400/300', 3);

-- Sample review data so the review UI isn't empty
INSERT INTO reviews (product_id, user_id, rating, comment) VALUES
(1, 2, 5, 'Excellent sound quality and the battery really does last all day.'),
(3, 2, 4, 'Great feel, a bit loud for an office setting.');
