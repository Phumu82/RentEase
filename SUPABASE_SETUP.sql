-- RentEase: one-off Supabase setup the Android app depends on.
-- Run in Supabase dashboard -> SQL Editor. Nothing here contains a password or key.

-- 1) Make property photos viewable by visitors who are not signed in.
--    (The web schema only lets signed-in users read this bucket, so without this
--     the landing page, guest browsing and listing galleries show "Sample photo".)
update storage.buckets set public = true where id = 'property-images';

-- 2) Admin account: first create the user in Authentication -> Users -> Add user
--    (email admin@rentease.com, choose your own password, tick auto-confirm), then:
insert into public.user_roles (user_id, role)
select id, 'admin'::public.app_role from auth.users where email = 'admin@rentease.com'
on conflict (user_id, role) do nothing;

-- ---------------------------------------------------------------------------------------------
-- Forgot password (dashboard setting, not SQL):
-- Supabase dashboard -> Authentication -> URL Configuration -> Redirect URLs -> add:
--     rentease://reset-password
-- Without this, the reset email's link will not open the app. Keep the default
-- "Reset password" email template ({{ .ConfirmationURL }}). The user must open the email on the
-- phone that has RentEase installed.
-- ---------------------------------------------------------------------------------------------
