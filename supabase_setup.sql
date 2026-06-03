-- ============================================================
-- UPSGlam 3.0 — Migraciones SQL para Supabase PostgreSQL
-- Ejecutar en orden en el SQL Editor de Supabase Studio
-- ============================================================


-- ============================================================
-- 001 — EXTENSIONES
-- ============================================================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";


-- ============================================================
-- 002 — TABLA: profiles
-- Vinculada a auth.users mediante trigger automático.
-- No modificar auth.users directamente.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    auth_user_id    UUID UNIQUE NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    username        VARCHAR(50) UNIQUE NOT NULL,
    avatar_url      TEXT,
    bio             TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Trigger: Crea un perfil automáticamente al registrar un usuario en Supabase Auth.
-- Resuelve colisiones de usernames agregando un sufijo secuencial.
-- Maneja registros de autenticación sin correo o sin metadatos.
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    base_username   TEXT;
    final_username  TEXT;
    counter         INT := 0;
BEGIN
    -- 1. Obtener username de metadata, luego del email, y finalmente usar un fallback
    base_username := COALESCE(
        NEW.raw_user_meta_data->>'username', 
        SPLIT_PART(NEW.email, '@', 1),
        'user_' || SUBSTR(gen_random_uuid()::TEXT, 1, 8)
    );
    final_username := base_username;
    
    -- 2. Evitar colisión de usernames agregando sufijos numéricos incrementalmente
    WHILE EXISTS (SELECT 1 FROM public.profiles WHERE username = final_username) LOOP
        counter := counter + 1;
        final_username := base_username || counter::TEXT;
    END LOOP;

    -- 3. Insertar el perfil creado
    INSERT INTO public.profiles (auth_user_id, username)
    VALUES (NEW.id, final_username);
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();


-- ============================================================
-- 003 — TABLA: filters
-- Pre-poblada con los 6 filtros del proyecto.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.filters (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) UNIQUE NOT NULL,
    description TEXT NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Datos iniciales — 3 filtros de prácticas + 3 nuevos
INSERT INTO public.filters (name, description) VALUES
    ('laplaciano',      'Detección de bordes mediante segunda derivada, sin dirección'),
    ('promedio',        'Suavizado lineal que reduce ruido pero difumina bordes'),
    ('nitidez',         'Realce de detalles acentuando altas frecuencias'),
    ('mediana',         'Reducción de ruido impulsivo no lineal, preserva bordes'),
    ('sobel',           'Detección de bordes direccional mediante gradientes'),
    ('marca_agua_ups',  'Superposición de identidad UPS con colores institucionales')
ON CONFLICT (name) DO NOTHING;


-- ============================================================
-- 004 — TABLA: posts
-- ============================================================
CREATE TABLE IF NOT EXISTS public.posts (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    filter_id             UUID NOT NULL REFERENCES public.filters(id),
    caption               TEXT,
    original_image_url    TEXT NOT NULL,
    processed_image_url   TEXT NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 005 — TABLA: comments
-- ============================================================
CREATE TABLE IF NOT EXISTS public.comments (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     UUID NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    content     TEXT NOT NULL CHECK (CHAR_LENGTH(content) BETWEEN 1 AND 500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 006 — TABLA: likes
-- UNIQUE(post_id, user_id) impide likes duplicados a nivel BD.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.likes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     UUID NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_like_per_user_post UNIQUE (post_id, user_id)
);


-- ============================================================
-- 007 — TABLA: processing_history
-- Registra cada procesamiento GPU solicitado por un usuario.
-- post_id es nullable: un usuario puede procesar sin publicar.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.processing_history (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    post_id               UUID REFERENCES public.posts(id) ON DELETE SET NULL,
    filter_id             UUID NOT NULL REFERENCES public.filters(id),
    original_image_url    TEXT NOT NULL,
    processed_image_url   TEXT,
    status                VARCHAR(20) NOT NULL DEFAULT 'pending'
                              CHECK (status IN ('pending', 'success', 'error')),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 008 — TABLA: gpu_metrics
-- Métricas técnicas CUDA de cada procesamiento.
-- Relación 1:1 con processing_history.
-- ============================================================
CREATE TABLE IF NOT EXISTS public.gpu_metrics (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    processing_id   UUID UNIQUE NOT NULL
                        REFERENCES public.processing_history(id) ON DELETE CASCADE,
    block_dim       VARCHAR(20) NOT NULL,   -- ej. "256x1x1"
    grid_dim        VARCHAR(30) NOT NULL,   -- ej. "7650x1x1"
    total_threads   INTEGER NOT NULL CHECK (total_threads > 0),
    kernel_time_ms  NUMERIC(10, 4) NOT NULL CHECK (kernel_time_ms >= 0),
    image_width     INTEGER NOT NULL CHECK (image_width > 0),
    image_height    INTEGER NOT NULL CHECK (image_height > 0),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- ============================================================
-- 009 — ÍNDICES
-- Aceleran las consultas más frecuentes de la app.
-- ============================================================

-- Feed: posts ordenados por fecha
CREATE INDEX IF NOT EXISTS idx_posts_created_at
    ON public.posts (created_at DESC);

-- Posts de un usuario (perfil)
CREATE INDEX IF NOT EXISTS idx_posts_user_id
    ON public.posts (user_id);

-- Comentarios de un post
CREATE INDEX IF NOT EXISTS idx_comments_post_id
    ON public.comments (post_id);

-- Likes de un post (conteo rápido)
CREATE INDEX IF NOT EXISTS idx_likes_post_id
    ON public.likes (post_id);

-- Historial de procesamiento por usuario
CREATE INDEX IF NOT EXISTS idx_processing_history_user_id
    ON public.processing_history (user_id);

-- Métricas por procesamiento
CREATE INDEX IF NOT EXISTS idx_gpu_metrics_processing_id
    ON public.gpu_metrics (processing_id);


-- ============================================================
-- 010 — ROW LEVEL SECURITY (RLS)
-- Activa RLS en tablas con datos de usuario.
-- filters es pública (solo lectura para todos).
-- ============================================================

ALTER TABLE public.profiles            ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.posts               ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.comments            ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.likes               ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.processing_history  ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.gpu_metrics         ENABLE ROW LEVEL SECURITY;

-- ── profiles ──────────────────────────────────────────
-- Cualquiera puede ver perfiles (feed público)
DROP POLICY IF EXISTS "profiles_select_public" ON public.profiles;
CREATE POLICY "profiles_select_public"
    ON public.profiles FOR SELECT USING (TRUE);

-- Solo el dueño puede actualizar su perfil
DROP POLICY IF EXISTS "profiles_update_own" ON public.profiles;
CREATE POLICY "profiles_update_own"
    ON public.profiles FOR UPDATE
    USING (auth_user_id = auth.uid());

-- ── posts ─────────────────────────────────────────────
-- Todos pueden ver publicaciones
DROP POLICY IF EXISTS "posts_select_public" ON public.posts;
CREATE POLICY "posts_select_public"
    ON public.posts FOR SELECT USING (TRUE);

-- Solo el dueño puede crear y borrar sus posts
DROP POLICY IF EXISTS "posts_insert_own" ON public.posts;
CREATE POLICY "posts_insert_own"
    ON public.posts FOR INSERT
    WITH CHECK (user_id = (SELECT id FROM public.profiles WHERE auth_user_id = auth.uid()));

DROP POLICY IF EXISTS "posts_delete_own" ON public.posts;
CREATE POLICY "posts_delete_own"
    ON public.posts FOR DELETE
    USING (user_id = (SELECT id FROM public.profiles WHERE auth_user_id = auth.uid()));

-- ── comments ──────────────────────────────────────────
DROP POLICY IF EXISTS "comments_select_public" ON public.comments;
CREATE POLICY "comments_select_public"
    ON public.comments FOR SELECT USING (TRUE);

DROP POLICY IF EXISTS "comments_insert_auth" ON public.comments;
CREATE POLICY "comments_insert_auth"
    ON public.comments FOR INSERT
    WITH CHECK (user_id = (SELECT id FROM public.profiles WHERE auth_user_id = auth.uid()));

DROP POLICY IF EXISTS "comments_delete_own" ON public.comments;
CREATE POLICY "comments_delete_own"
    ON public.comments FOR DELETE
    USING (user_id = (SELECT id FROM public.profiles WHERE auth_user_id = auth.uid()));

-- ── likes ─────────────────────────────────────────────
DROP POLICY IF EXISTS "likes_select_public" ON public.likes;
CREATE POLICY "likes_select_public"
    ON public.likes FOR SELECT USING (TRUE);

DROP POLICY IF EXISTS "likes_insert_auth" ON public.likes;
CREATE POLICY "likes_insert_auth"
    ON public.likes FOR INSERT
    WITH CHECK (user_id = (SELECT id FROM public.profiles WHERE auth_user_id = auth.uid()));

DROP POLICY IF EXISTS "likes_delete_own" ON public.likes;
CREATE POLICY "likes_delete_own"
    ON public.likes FOR DELETE
    USING (user_id = (SELECT id FROM public.profiles WHERE auth_user_id = auth.uid()));

-- ── processing_history ────────────────────────────────
-- Cada usuario solo ve su propio historial
DROP POLICY IF EXISTS "history_select_own" ON public.processing_history;
CREATE POLICY "history_select_own"
    ON public.processing_history FOR SELECT
    USING (user_id = (SELECT id FROM public.profiles WHERE auth_user_id = auth.uid()));

DROP POLICY IF EXISTS "history_insert_own" ON public.processing_history;
CREATE POLICY "history_insert_own"
    ON public.processing_history FOR INSERT
    WITH CHECK (user_id = (SELECT id FROM public.profiles WHERE auth_user_id = auth.uid()));

-- ── gpu_metrics ───────────────────────────────────────
-- Solo el dueño del procesamiento puede ver sus métricas
DROP POLICY IF EXISTS "gpu_metrics_select_own" ON public.gpu_metrics;
CREATE POLICY "gpu_metrics_select_own"
    ON public.gpu_metrics FOR SELECT
    USING (
        processing_id IN (
            SELECT ph.id FROM public.processing_history ph
            JOIN public.profiles p ON p.id = ph.user_id
            WHERE p.auth_user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "gpu_metrics_insert_own" ON public.gpu_metrics;
CREATE POLICY "gpu_metrics_insert_own"
    ON public.gpu_metrics FOR INSERT
    WITH CHECK (
        processing_id IN (
            SELECT ph.id FROM public.processing_history ph
            JOIN public.profiles p ON p.id = ph.user_id
            WHERE p.auth_user_id = auth.uid()
        )
    );


-- ============================================================
-- 011 — BUCKETS DE ALMACENAMIENTO (Supabase Storage)
-- Inicializa los buckets requeridos por el backend
-- ============================================================
INSERT INTO storage.buckets (id, name, public)
VALUES 
    ('originals', 'originals', TRUE),
    ('processed', 'processed', TRUE)
ON CONFLICT (id) DO NOTHING;


-- ============================================================
-- 012 — TABLA: follows
-- ============================================================
CREATE TABLE IF NOT EXISTS public.follows (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    follower_id   UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    following_id  UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_follow_per_user UNIQUE (follower_id, following_id),
    CONSTRAINT chk_no_self_follow CHECK (follower_id <> following_id)
);

ALTER TABLE public.follows ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "follows_select_public" ON public.follows;
CREATE POLICY "follows_select_public"
    ON public.follows FOR SELECT USING (TRUE);

DROP POLICY IF EXISTS "follows_insert_own" ON public.follows;
CREATE POLICY "follows_insert_own"
    ON public.follows FOR INSERT
    WITH CHECK (follower_id = (SELECT id FROM public.profiles WHERE auth_user_id = auth.uid()));

DROP POLICY IF EXISTS "follows_delete_own" ON public.follows;
CREATE POLICY "follows_delete_own"
    ON public.follows FOR DELETE
    USING (follower_id = (SELECT id FROM public.profiles WHERE auth_user_id = auth.uid()));


-- ============================================================
-- 013 — TABLA: reposts
-- ============================================================
CREATE TABLE IF NOT EXISTS public.reposts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id       UUID NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_repost_per_user_post UNIQUE (post_id, user_id)
);

ALTER TABLE public.reposts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "reposts_select_public" ON public.reposts;
CREATE POLICY "reposts_select_public"
    ON public.reposts FOR SELECT USING (TRUE);

DROP POLICY IF EXISTS "reposts_insert_own" ON public.reposts;
CREATE POLICY "reposts_insert_own"
    ON public.reposts FOR INSERT
    WITH CHECK (user_id = (SELECT id FROM public.profiles WHERE auth_user_id = auth.uid()));

DROP POLICY IF EXISTS "reposts_delete_own" ON public.reposts;
CREATE POLICY "reposts_delete_own"
    ON public.reposts FOR DELETE
    USING (user_id = (SELECT id FROM public.profiles WHERE auth_user_id = auth.uid()));


-- ============================================================
-- FIN DEL SCRIPT
-- ============================================================

