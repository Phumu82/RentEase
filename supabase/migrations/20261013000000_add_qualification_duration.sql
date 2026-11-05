-- Adds the qualification length (in years) captured on student sign-up.
alter table public.student_profiles
  add column if not exists qualification_duration_years integer;
