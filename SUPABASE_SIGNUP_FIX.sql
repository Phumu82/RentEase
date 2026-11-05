-- RentEase: run this once in the Supabase SQL editor on an existing project.
-- Replaces the sign-up trigger so the student/landlord profile row is created for every new account,
-- even when "Confirm email" is on and the app has no session at sign-up time.
-- Safe to run more than once. Requires the tables from SUPABASE_FULL_SCHEMA.sql / SUPABASE_SETUP.sql.

create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
declare
  v_meta jsonb := coalesce(new.raw_user_meta_data, '{}'::jsonb);
  v_role text := lower(coalesce(v_meta->>'role', 'student'));
  v_uni uuid;
begin
  if v_role not in ('student', 'landlord') then v_role := 'student'; end if;  -- never 'admin' from a form

  -- A malformed university id must never block sign-up.
  begin
    v_uni := nullif(v_meta->>'university_id', '')::uuid;
  exception when others then
    v_uni := null;
  end;

  insert into public.profiles (id, full_name, email, phone, company_name, university_id, student_number)
  values (
    new.id,
    nullif(v_meta->>'full_name', ''),
    new.email,
    nullif(v_meta->>'phone', ''),
    nullif(v_meta->>'company_name', ''),
    case when v_role = 'student' then v_uni end,
    case when v_role = 'student' then nullif(v_meta->>'student_number', '') end
  )
  on conflict (id) do nothing;

  insert into public.user_roles (user_id, role) values (new.id, v_role::public.app_role)
  on conflict (user_id, role) do nothing;

  -- Child rows are created here, not by the app: with "Confirm email" on, sign-up returns no session,
  -- so the app cannot write them (row-level security) until the person has confirmed and signed in.
  if v_role = 'student' then
    insert into public.student_profiles (user_id, student_number, university_id)
    values (new.id, nullif(v_meta->>'student_number', ''), v_uni)
    on conflict (user_id) do nothing;
  else
    insert into public.landlord_profiles (user_id, legal_name, business_name, contact_phone)
    values (
      new.id,
      coalesce(nullif(v_meta->>'legal_name', ''), nullif(v_meta->>'full_name', '')),
      nullif(v_meta->>'company_name', ''),
      nullif(v_meta->>'phone', '')
    )
    on conflict (user_id) do nothing;
  end if;

  return new;
end; $$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users
  for each row execute function public.handle_new_user();

-- Back-fill 1: any auth user without a profile / role row (e.g. created before the trigger existed,
-- such as an admin added in the dashboard). Without a profile row sign-in ends in "could not load profile".
insert into public.profiles (id, full_name, email)
select u.id, nullif(u.raw_user_meta_data->>'full_name', ''), u.email
from auth.users u
where not exists (select 1 from public.profiles p where p.id = u.id)
on conflict (id) do nothing;

insert into public.user_roles (user_id, role)
select u.id, 'student'::public.app_role
from auth.users u
where not exists (select 1 from public.user_roles r where r.user_id = u.id)
on conflict (user_id, role) do nothing;

-- Back-fill 2: accounts created before this fix that are missing their child row.
insert into public.student_profiles (user_id)
select ur.user_id from public.user_roles ur
join public.profiles p on p.id = ur.user_id
where ur.role = 'student'
on conflict (user_id) do nothing;

insert into public.landlord_profiles (user_id, legal_name, business_name, contact_phone)
select ur.user_id, p.full_name, p.company_name, p.phone from public.user_roles ur
join public.profiles p on p.id = ur.user_id
where ur.role = 'landlord'
on conflict (user_id) do nothing;
