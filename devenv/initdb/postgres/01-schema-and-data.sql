-- MasterMeister5 devenv: 動作確認用のサンプルDDL/初期データ（PostgreSQL 17）
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
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    registered_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(255)
);

CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    in_stock BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    customer_id INTEGER NOT NULL REFERENCES customers (id),
    order_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL
);

CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES orders (id),
    product_id INTEGER NOT NULL REFERENCES products (id),
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL
);

-- customers: 5,000件
INSERT INTO customers (name, email, registered_date, is_active, notes)
SELECT
    'Customer ' || n,
    'customer' || n || '@example.com',
    DATE '2020-01-01' + (n % 2000),
    (n % 10) <> 0,
    CASE WHEN n % 7 = 0 THEN 'VIP customer' ELSE NULL END
FROM generate_series(1, 5000) AS n
ORDER BY n;

-- products: 200件
INSERT INTO products (name, category, unit_price, in_stock)
SELECT
    'Product ' || n,
    (ARRAY['Electronics', 'Books', 'Clothing', 'Home', 'Toys'])[1 + (n % 5)],
    ROUND((10 + (n % 990) / 10.0)::numeric, 2),
    (n % 8) <> 0
FROM generate_series(1, 200) AS n
ORDER BY n;

-- orders: 20,000件
INSERT INTO orders (customer_id, order_date, status, total_amount)
SELECT
    1 + (n % 5000),
    DATE '2023-01-01' + (n % 900),
    (ARRAY['PENDING', 'SHIPPED', 'DELIVERED', 'CANCELLED'])[1 + (n % 4)],
    ROUND((5 + (n % 9500) / 100.0)::numeric, 2)
FROM generate_series(1, 20000) AS n
ORDER BY n;

-- order_items: 注文1件あたり1〜3件（約40,000件）
INSERT INTO order_items (order_id, product_id, quantity, unit_price)
SELECT
    o.id,
    1 + ((o.id * s + 7) % 200),
    1 + ((o.id + s) % 5),
    ROUND((10 + ((o.id * s) % 990) / 10.0)::numeric, 2)
FROM orders o
CROSS JOIN generate_series(1, 3) AS s
WHERE (o.id + s) % 3 <> 0
ORDER BY o.id, s;
