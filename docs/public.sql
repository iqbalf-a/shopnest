-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.

CREATE TABLE public.users (
  id uuid NOT NULL,
  created_at timestamp without time zone NOT NULL,
  updated_at timestamp without time zone,
  email character varying NOT NULL UNIQUE,
  enabled boolean NOT NULL,
  name character varying NOT NULL,
  password character varying NOT NULL,
  role character varying NOT NULL CHECK (role::text = ANY (ARRAY['USER'::character varying, 'ADMIN'::character varying]::text[])),
  CONSTRAINT users_pkey PRIMARY KEY (id)
);
CREATE TABLE public.addresses (
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
  CONSTRAINT fkem30y0v2nmuoopc9v46i6g8mh FOREIGN KEY (profile_id) REFERENCES public.user_profiles(id)
);
CREATE TABLE public.user_profiles (
  id uuid NOT NULL,
  created_at timestamp without time zone NOT NULL,
  updated_at timestamp without time zone,
  birth_date date,
  full_name character varying NOT NULL,
  phone_number character varying,
  user_id uuid NOT NULL UNIQUE,
  CONSTRAINT user_profiles_pkey PRIMARY KEY (id)
);