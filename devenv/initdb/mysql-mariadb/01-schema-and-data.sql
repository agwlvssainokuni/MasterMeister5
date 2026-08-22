-- MasterMeister5 devenv: 動作確認用のサンプルDDL/初期データ（MySQL 8.4 / MariaDB 11.4共通）
--
-- 対象RDBMS接続の動作確認（Unit 3: スキーマ取込、Unit 4: アクセス権限、
-- Unit 5: データ表示・カスタマイズ、Unit 6: クエリビルダー・大量データ取得監査）
-- のために、顧客・商品・注文・注文明細からなる簡易ECドメインのテーブルを作成し、
-- ページング（デフォルト50件）・大量データ取得閾値（デフォルト100件）・
-- クエリ実行結果の上限（1,000件）を実際に体験できる件数のデータを投入する。
--
-- Docker公式イメージの仕様により、このファイルはデータディレクトリが空の状態で
-- コンテナを起動した場合にのみ自動実行される。既存のコンテナを再利用している場合は
-- `docker compose down`（コンテナごと削除）してから起動し直すこと。

CREATE TABLE customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    registered_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(255)
) ENGINE=InnoDB;

CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    in_stock BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    order_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
) ENGINE=InnoDB;

CREATE TABLE order_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB;

-- 連番生成用の使い捨てヘルパーテーブル（0〜9）。再帰CTEはMySQLの
-- cte_max_recursion_depth（デフォルト1,000）に抵触するため、依存しない
-- 桁の直積方式で連番を作る。
CREATE TABLE digits (d INT PRIMARY KEY);
INSERT INTO digits VALUES (0), (1), (2), (3), (4), (5), (6), (7), (8), (9);

-- customers: 5,000件
INSERT INTO customers (name, email, registered_date, is_active, notes)
SELECT
    CONCAT('Customer ', n),
    CONCAT('customer', n, '@example.com'),
    DATE_ADD('2020-01-01', INTERVAL (n % 2000) DAY),
    (n % 10) <> 0,
    CASE WHEN n % 7 = 0 THEN 'VIP customer' ELSE NULL END
FROM (
    SELECT d1.d + d2.d * 10 + d3.d * 100 + d4.d * 1000 + 1 AS n
    FROM digits d1, digits d2, digits d3, digits d4
) AS nums
WHERE n <= 5000;

-- products: 200件
INSERT INTO products (name, category, unit_price, in_stock)
SELECT
    CONCAT('Product ', n),
    ELT(1 + (n % 5), 'Electronics', 'Books', 'Clothing', 'Home', 'Toys'),
    ROUND(10 + (n % 990) / 10, 2),
    (n % 8) <> 0
FROM (
    SELECT d1.d + d2.d * 10 + d3.d * 100 + 1 AS n
    FROM digits d1, digits d2, digits d3
) AS nums
WHERE n <= 200;

-- orders: 20,000件
INSERT INTO orders (customer_id, order_date, status, total_amount)
SELECT
    1 + (n % 5000),
    DATE_ADD('2023-01-01', INTERVAL (n % 900) DAY),
    ELT(1 + (n % 4), 'PENDING', 'SHIPPED', 'DELIVERED', 'CANCELLED'),
    ROUND(5 + (n % 9500) / 100, 2)
FROM (
    SELECT d1.d + d2.d * 10 + d3.d * 100 + d4.d * 1000 + d5.d * 10000 + 1 AS n
    FROM digits d1, digits d2, digits d3, digits d4, digits d5
) AS nums
WHERE n <= 20000;

-- order_items: 注文1件あたり1〜3件（約40,000件）
INSERT INTO order_items (order_id, product_id, quantity, unit_price)
SELECT
    o.id,
    1 + ((o.id * mult.s + 7) % 200),
    1 + ((o.id + mult.s) % 5),
    ROUND(10 + ((o.id * mult.s) % 990) / 10, 2)
FROM orders o
CROSS JOIN (SELECT 1 AS s UNION ALL SELECT 2 UNION ALL SELECT 3) AS mult
WHERE (o.id + mult.s) % 3 <> 0;

DROP TABLE digits;
