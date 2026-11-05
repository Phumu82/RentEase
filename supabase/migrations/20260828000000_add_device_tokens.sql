-- Adds FCM device token storage — required for Part 1 §3.8 "the Android app
-- registers an FCM device token with the API on login (POST /devices)".
-- No such table exists in the current schema; this migration adds it without
-- touching any existing table.

CREATE TABLE public.device_tokens (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  fcm_token text NOT NULL,
  platform text NOT NULL DEFAULT 'android',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (user_id, fcm_token)
);

CREATE INDEX device_tokens_user_idx ON public.device_tokens(user_id);

ALTER TABLE public.device_tokens ENABLE ROW LEVEL SECURITY;

-- A user may only see/manage their own device tokens.
CREATE POLICY "Users manage own device tokens"
  ON public.device_tokens
  FOR ALL
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.device_tokens TO authenticated;

-- Keep updated_at current on re-registration (token refresh) without
-- requiring the client to know to set it manually.
CREATE OR REPLACE FUNCTION public.touch_device_token_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER device_tokens_touch_updated_at
  BEFORE UPDATE ON public.device_tokens
  FOR EACH ROW
  EXECUTE FUNCTION public.touch_device_token_updated_at();
