-- Snapshot skema database ShopNest (Supabase) — untuk dokumentasi, bukan untuk dijalankan.
-- Tabel dibuat otomatis oleh Hibernate (ddl-auto=update) saat tiap service startup.
-- Arsitektur: schema-per-service — tiap service hanya menyentuh schema miliknya sendiri.
-- Tidak ada FK lintas schema: relasi antar service dipegang lewat API (Feign), bukan database.

-- ============ auth-service (schema: auth_service) ============
CREATE TABLE auth_service.users (
  id uuid NOT NULL,
  created_at timestamp without time zone NOT NULL,
  updated_at timestamp without time zone,
  email character varying NOT NULL UNIQUE,
  enabled boolean NOT NULL,
  name character varying NOT NULL,
  password character varying NOT NULL,
  role character varying NOT NULL CHECK (role IN ('USER', 'ADMIN')),
  CONSTRAINT users_pkey PRIMARY KEY (id)
);

-- ============ user-service (schema: user_service) ============
CREATE TABLE user_service.user_profiles (
  id uuid NOT NULL,
  created_at timestamp without time zone NOT NULL,
  updated_at timestamp without time zone,
  birth_date date,
  full_name character varying NOT NULL,
  phone_number character varying,
  user_id uuid NOT NULL UNIQUE, -- ID dari auth_service.users (bukan FK - beda service)
  CONSTRAINT user_profiles_pkey PRIMARY KEY (id)
);

CREATE TABLE user_service.addresses (
  id uuid NOT NULL,
  created_at timestamp without time zone NOT NULL,
  updated_at timestamp without time zone,
  city character varying NOT NULL,
  is_default boolean NOT NULL,
  label character varying NOT NULL,
  postal_code character varying NOT NULL,
  street character varying NOT NULL,
  profile_id uuid NOT NULL,
  CONSTRAINT addresses_pkey PRIMARY KEY (id),
  CONSTRAINT fk_addresses_profile FOREIGN KEY (profile_id) REFERENCES user_service.user_profiles(id)
);

-- ============ product-service (schema: product_service) ============
CREATE TABLE product_service.products (
  id uuid NOT NULL,
  created_at timestamp without time zone NOT NULL,
  updated_at timestamp without time zone,
  category character varying NOT NULL,
  description text,
  name character varying NOT NULL,
  price numeric NOT NULL,
  stock integer NOT NULL,
  CONSTRAINT products_pkey PRIMARY KEY (id)
);

-- ============ order-service (schema: order_service) ============
CREATE TABLE order_service.orders (
  id uuid NOT NULL,
  created_at timestamp without time zone NOT NULL,
  updated_at timestamp without time zone,
  status character varying NOT NULL CHECK (status IN ('PENDING', 'PAID', 'CANCELLED')),
  total_amount numeric NOT NULL,
  user_id uuid NOT NULL, -- ID dari auth_service.users (bukan FK - beda service)
  CONSTRAINT orders_pkey PRIMARY KEY (id)
);

CREATE TABLE order_service.order_items (
  id uuid NOT NULL,
  created_at timestamp without time zone NOT NULL,
  updated_at timestamp without time zone,
  price numeric NOT NULL, -- harga saat order dibuat (snapshot, bukan referensi live)
  product_id uuid NOT NULL, -- ID dari product_service.products (bukan FK - beda service)
  product_name character varying NOT NULL,
  quantity integer NOT NULL,
  subtotal numeric NOT NULL,
  order_id uuid NOT NULL,
  CONSTRAINT order_items_pkey PRIMARY KEY (id),
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES order_service.orders(id)
);
