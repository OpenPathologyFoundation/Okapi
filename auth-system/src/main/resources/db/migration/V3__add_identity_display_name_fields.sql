-- Add additional identity name representations used in clinical UI.
-- This keeps the core normalization stable while allowing accurate display in different contexts.

ALTER TABLE public.identities
    ADD COLUMN IF NOT EXISTS display_short VARCHAR(100),
    ADD COLUMN IF NOT EXISTS middle_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS middle_initial VARCHAR(10),
    ADD COLUMN IF NOT EXISTS nickname VARCHAR(60),
    ADD COLUMN IF NOT EXISTS prefix VARCHAR(20),
    ADD COLUMN IF NOT EXISTS suffix VARCHAR(20);
