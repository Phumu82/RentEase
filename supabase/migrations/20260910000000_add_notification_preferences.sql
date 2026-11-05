-- Adds per-category notification preference flags to profiles.
-- No such columns exist in the current schema (confirmed via ProfileDto and
-- the Phase 1 audit) — the Settings screen's notification toggles need
-- somewhere real to persist to. Defaults to true (opt-out model) so existing
-- users keep receiving notifications they'd expect unless they turn them off.

ALTER TABLE public.profiles
  ADD COLUMN IF NOT EXISTS notify_booking_updates boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS notify_new_messages boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS notify_maintenance_updates boolean NOT NULL DEFAULT true;
