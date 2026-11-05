-- RentEase: EVERYTHING in one file. Create the admin user FIRST (Authentication > Users > Add user), then paste this whole file in SQL Editor and click Run.

-- ======================== SUPABASE_FULL_SCHEMA.sql ========================
-- =====================================================================================
-- RentEase: COMPLETE Supabase database setup (tables, security rules, storage buckets)
-- Run ONCE in Supabase dashboard -> SQL Editor -> New query -> paste everything -> Run.
-- Safe to run again: it will not delete any data.
-- Order: 1) this file  2) SUPABASE_NOTIFY_TRIGGERS.sql  3) (after creating the admin user) SUPABASE_SETUP.sql
-- =====================================================================================

create extension if not exists pgcrypto;

-- ---------- Enums ----------
do $$ begin
  create type public.app_role as enum ('student', 'landlord', 'admin');
exception when duplicate_object then null; end $$;

do $$ begin
  create type public.verification_status as enum ('pending', 'submitted', 'verified', 'rejected', 'documents_required');
exception when duplicate_object then null; end $$;

-- ---------- Tables ----------
create table if not exists public.universities (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  short_name text,
  city text,
  province text,
  students integer,
  average_rent numeric
);

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text,
  email text,
  phone text,
  avatar_url text,
  bio text,
  university_id uuid references public.universities(id) on delete set null,
  student_number text,
  year_of_study integer,
  qualification_duration_years integer,
  company_name text,
  is_verified boolean not null default false,
  is_suspended boolean not null default false,
  preferred_language text not null default 'en',
  notify_booking_updates boolean not null default true,
  notify_new_messages boolean not null default true,
  notify_maintenance_updates boolean not null default true,
  created_at timestamptz not null default now()
);

create table if not exists public.user_roles (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  role public.app_role not null,
  unique (user_id, role)
);

create table if not exists public.student_profiles (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  student_number text,
  university_id uuid references public.universities(id) on delete set null,
  course text,
  faculty text,
  year_of_study integer,
  qualification_duration_years integer,
  budget_max numeric,
  preferred_room_type text,
  preferred_province text,
  preferred_city text,
  preferred_university_id uuid references public.universities(id) on delete set null,
  earliest_move_in date,
  needs_furnished boolean,
  proof_of_registration_url text,
  onboarding_complete boolean not null default false,
  created_at timestamptz not null default now()
);
alter table public.student_profiles add column if not exists qualification_duration_years integer;

create table if not exists public.landlord_profiles (
  user_id uuid primary key references public.profiles(id) on delete cascade,
  legal_name text,
  id_number text,
  business_name text,
  business_registration_number text,
  business_address text,
  city text,
  province text,
  postal_code text,
  contact_phone text,
  id_document_url text,
  proof_of_ownership_url text,
  proof_of_address_url text,
  business_registration_doc_url text,
  verification_status public.verification_status not null default 'pending',
  verification_notes text,
  onboarding_complete boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists public.properties (
  id uuid primary key default gen_random_uuid(),
  slug text unique not null default ('p-' || substr(replace(gen_random_uuid()::text, '-', ''), 1, 10)),
  title text not null,
  description text,
  address text,
  city text,
  province text,
  latitude double precision,
  longitude double precision,
  distance_km numeric,
  price_per_month numeric not null default 0,
  deposit numeric,
  bedrooms integer,
  bathrooms integer,
  rooms_available integer,
  furnished boolean not null default false,
  verified boolean not null default false,
  amenities text[] not null default '{}',
  cover_image text,
  rating numeric not null default 0,
  review_count integer not null default 0,
  landlord_name text,
  landlord_id uuid references public.profiles(id) on delete set null,
  university_id uuid references public.universities(id) on delete set null,
  status text not null default 'draft'
    check (status in ('published', 'draft', 'pending review', 'rejected', 'archived')),
  created_at timestamptz not null default now()
);
create index if not exists properties_landlord_idx on public.properties(landlord_id);
create index if not exists properties_status_idx on public.properties(status);

create table if not exists public.rooms (
  id uuid primary key default gen_random_uuid(),
  property_id uuid not null references public.properties(id) on delete cascade,
  name text not null,
  room_type text,
  price_per_month numeric not null default 0,
  capacity integer not null default 1,
  status text not null default 'available'
    check (status in ('available', 'reserved', 'occupied', 'maintenance')),
  created_at timestamptz not null default now()
);
create index if not exists rooms_property_idx on public.rooms(property_id);

create table if not exists public.property_images (
  id uuid primary key default gen_random_uuid(),
  property_id uuid not null references public.properties(id) on delete cascade,
  url text not null,
  position integer not null default 0,
  created_at timestamptz not null default now()
);
create index if not exists property_images_property_idx on public.property_images(property_id);

create table if not exists public.favorites (
  id uuid primary key default gen_random_uuid(),
  student_id uuid not null references public.profiles(id) on delete cascade,
  property_id uuid not null references public.properties(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (student_id, property_id)
);

create table if not exists public.bookings (
  id uuid primary key default gen_random_uuid(),
  reference text unique not null default ('BK-' || upper(substr(md5(random()::text || clock_timestamp()::text), 1, 8))),
  student_id uuid not null references public.profiles(id) on delete cascade,
  property_id uuid not null references public.properties(id) on delete cascade,
  room_id uuid references public.rooms(id) on delete set null,
  landlord_id uuid references public.profiles(id) on delete set null,
  move_in_date date,
  lease_months integer,
  monthly_rent numeric,
  deposit numeric,
  status text not null default 'pending',
  message text,
  id_document_url text,
  proof_of_registration_url text,
  decided_at timestamptz,
  created_at timestamptz not null default now()
);
create index if not exists bookings_student_idx on public.bookings(student_id);
create index if not exists bookings_landlord_idx on public.bookings(landlord_id);

create table if not exists public.reviews (
  id uuid primary key default gen_random_uuid(),
  property_id uuid not null references public.properties(id) on delete cascade,
  booking_id uuid references public.bookings(id) on delete set null,
  student_id uuid not null references public.profiles(id) on delete cascade,
  author_name text,
  rating integer not null check (rating between 1 and 5),
  title text,
  body text,
  created_at timestamptz not null default now()
);

create table if not exists public.payments (
  id uuid primary key default gen_random_uuid(),
  reference text unique not null default ('PAY-' || upper(substr(md5(random()::text || clock_timestamp()::text), 1, 8))),
  booking_id uuid not null references public.bookings(id) on delete cascade,
  student_id uuid not null references public.profiles(id) on delete cascade,
  amount numeric not null,
  kind text not null default 'deposit' check (kind in ('deposit', 'rent', 'fee')),
  status text not null default 'pending' check (status in ('pending', 'paid', 'failed', 'refunded')),
  due_date date,
  paid_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists public.maintenance_requests (
  id uuid primary key default gen_random_uuid(),
  property_id uuid not null references public.properties(id) on delete cascade,
  student_id uuid not null references public.profiles(id) on delete cascade,
  booking_id uuid references public.bookings(id) on delete set null,
  title text not null,
  description text,
  category text,
  priority text,
  status text not null default 'open' check (status in ('open', 'in_progress', 'resolved', 'closed')),
  photo_url text,
  created_at timestamptz not null default now()
);

create table if not exists public.conversations (
  id uuid primary key default gen_random_uuid(),
  student_id uuid not null references public.profiles(id) on delete cascade,
  landlord_id uuid not null references public.profiles(id) on delete cascade,
  property_id uuid references public.properties(id) on delete set null,
  subject text,
  last_message_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create table if not exists public.messages (
  id uuid primary key default gen_random_uuid(),
  conversation_id uuid not null references public.conversations(id) on delete cascade,
  sender_id uuid not null references public.profiles(id) on delete cascade,
  body text not null,
  created_at timestamptz not null default now(),
  read_at timestamptz
);
create index if not exists messages_conv_idx on public.messages(conversation_id);

create table if not exists public.notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  title text not null,
  body text,
  category text,
  link text,
  read_at timestamptz,
  created_at timestamptz not null default now()
);
create index if not exists notifications_user_idx on public.notifications(user_id);

create table if not exists public.audit_logs (
  id uuid primary key default gen_random_uuid(),
  actor_id uuid,
  action text not null,
  entity text,
  entity_id uuid,
  metadata jsonb,
  created_at timestamptz not null default now()
);

create table if not exists public.complaints (
  id uuid primary key default gen_random_uuid(),
  reporter_id uuid not null references public.profiles(id) on delete cascade,
  subject text not null,
  category text,
  description text,
  status text not null default 'open' check (status in ('open', 'in_progress', 'resolved', 'closed')),
  admin_response text,
  resolved_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists public.device_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  fcm_token text not null,
  platform text not null default 'android',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, fcm_token)
);
create index if not exists device_tokens_user_idx on public.device_tokens(user_id);

create or replace function public.touch_device_token_updated_at()
returns trigger language plpgsql as $$
begin new.updated_at = now(); return new; end; $$;
drop trigger if exists device_tokens_touch_updated_at on public.device_tokens;
create trigger device_tokens_touch_updated_at before update on public.device_tokens
  for each row execute function public.touch_device_token_updated_at();

-- ---------- Helper: does a user hold a role? (security definer avoids RLS recursion) ----------
create or replace function public.has_role(_user_id uuid, _role public.app_role)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.user_roles where user_id = _user_id and role = _role);
$$;
grant execute on function public.has_role(uuid, public.app_role) to anon, authenticated;

-- ---------- Sign-up trigger: creates profile + role from the sign-up form's metadata ----------
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

-- Stop normal users granting themselves "verified" / un-suspending themselves.
create or replace function public.protect_profile_flags()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is not null and coalesce(auth.role(), '') <> 'service_role'
     and not public.has_role(auth.uid(), 'admin') then
    new.is_verified := old.is_verified;
    new.is_suspended := old.is_suspended;
  end if;
  return new;
end; $$;
drop trigger if exists protect_profile_flags on public.profiles;
create trigger protect_profile_flags before update on public.profiles
  for each row execute function public.protect_profile_flags();

-- ---------- Row Level Security ----------
alter table public.universities          enable row level security;
alter table public.profiles              enable row level security;
alter table public.user_roles            enable row level security;
alter table public.student_profiles      enable row level security;
alter table public.landlord_profiles     enable row level security;
alter table public.properties            enable row level security;
alter table public.rooms                 enable row level security;
alter table public.property_images       enable row level security;
alter table public.favorites             enable row level security;
alter table public.bookings              enable row level security;
alter table public.reviews               enable row level security;
alter table public.payments              enable row level security;
alter table public.maintenance_requests  enable row level security;
alter table public.conversations         enable row level security;
alter table public.messages              enable row level security;
alter table public.notifications         enable row level security;
alter table public.audit_logs            enable row level security;
alter table public.complaints            enable row level security;
alter table public.device_tokens         enable row level security;

grant usage on schema public to anon, authenticated;
grant select on public.universities, public.properties, public.rooms, public.property_images, public.reviews to anon;
grant select, insert, update, delete on all tables in schema public to authenticated;

-- universities: everyone reads, admin writes
drop policy if exists "universities read" on public.universities;
create policy "universities read" on public.universities for select using (true);
drop policy if exists "universities admin" on public.universities;
create policy "universities admin" on public.universities for all to authenticated
  using (public.has_role(auth.uid(), 'admin')) with check (public.has_role(auth.uid(), 'admin'));

-- profiles: any signed-in user can read (names on bookings/messages); edit only your own
drop policy if exists "profiles read" on public.profiles;
create policy "profiles read" on public.profiles for select to authenticated using (true);
drop policy if exists "profiles insert own" on public.profiles;
create policy "profiles insert own" on public.profiles for insert to authenticated with check (id = auth.uid());
drop policy if exists "profiles update own" on public.profiles;
create policy "profiles update own" on public.profiles for update to authenticated
  using (id = auth.uid() or public.has_role(auth.uid(), 'admin'))
  with check (id = auth.uid() or public.has_role(auth.uid(), 'admin'));

-- user_roles: read your own (admin reads all). Writes only via trigger / edge function.
drop policy if exists "roles read" on public.user_roles;
create policy "roles read" on public.user_roles for select to authenticated
  using (user_id = auth.uid() or public.has_role(auth.uid(), 'admin'));

-- student_profiles
drop policy if exists "student_profiles own" on public.student_profiles;
create policy "student_profiles own" on public.student_profiles for all to authenticated
  using (user_id = auth.uid() or public.has_role(auth.uid(), 'admin'))
  with check (user_id = auth.uid() or public.has_role(auth.uid(), 'admin'));
drop policy if exists "student_profiles landlord read" on public.student_profiles;
create policy "student_profiles landlord read" on public.student_profiles for select to authenticated
  using (exists (select 1 from public.bookings b where b.student_id = student_profiles.user_id and b.landlord_id = auth.uid()));

-- landlord_profiles
drop policy if exists "landlord_profiles own" on public.landlord_profiles;
create policy "landlord_profiles own" on public.landlord_profiles for all to authenticated
  using (user_id = auth.uid() or public.has_role(auth.uid(), 'admin'))
  with check (user_id = auth.uid() or public.has_role(auth.uid(), 'admin'));

-- properties: public sees published; owner sees own; admin sees all
drop policy if exists "properties read" on public.properties;
create policy "properties read" on public.properties for select
  using (status = 'published' or landlord_id = auth.uid() or public.has_role(auth.uid(), 'admin'));
drop policy if exists "properties insert" on public.properties;
create policy "properties insert" on public.properties for insert to authenticated
  with check (landlord_id = auth.uid() or public.has_role(auth.uid(), 'admin'));
drop policy if exists "properties update" on public.properties;
create policy "properties update" on public.properties for update to authenticated
  using (landlord_id = auth.uid() or public.has_role(auth.uid(), 'admin'))
  with check (landlord_id = auth.uid() or public.has_role(auth.uid(), 'admin'));
drop policy if exists "properties delete" on public.properties;
create policy "properties delete" on public.properties for delete to authenticated
  using (landlord_id = auth.uid() or public.has_role(auth.uid(), 'admin'));

-- rooms and property_images follow their property
drop policy if exists "rooms read" on public.rooms;
create policy "rooms read" on public.rooms for select using (
  exists (select 1 from public.properties p where p.id = rooms.property_id
          and (p.status = 'published' or p.landlord_id = auth.uid() or public.has_role(auth.uid(), 'admin'))));
drop policy if exists "rooms write" on public.rooms;
create policy "rooms write" on public.rooms for all to authenticated
  using (exists (select 1 from public.properties p where p.id = rooms.property_id
                 and (p.landlord_id = auth.uid() or public.has_role(auth.uid(), 'admin'))))
  with check (exists (select 1 from public.properties p where p.id = rooms.property_id
                 and (p.landlord_id = auth.uid() or public.has_role(auth.uid(), 'admin'))));

drop policy if exists "images read" on public.property_images;
create policy "images read" on public.property_images for select using (
  exists (select 1 from public.properties p where p.id = property_images.property_id
          and (p.status = 'published' or p.landlord_id = auth.uid() or public.has_role(auth.uid(), 'admin'))));
drop policy if exists "images write" on public.property_images;
create policy "images write" on public.property_images for all to authenticated
  using (exists (select 1 from public.properties p where p.id = property_images.property_id
                 and (p.landlord_id = auth.uid() or public.has_role(auth.uid(), 'admin'))))
  with check (exists (select 1 from public.properties p where p.id = property_images.property_id
                 and (p.landlord_id = auth.uid() or public.has_role(auth.uid(), 'admin'))));

-- favorites: own
drop policy if exists "favorites own" on public.favorites;
create policy "favorites own" on public.favorites for all to authenticated
  using (student_id = auth.uid()) with check (student_id = auth.uid());

-- reviews: everyone reads; students write their own
drop policy if exists "reviews read" on public.reviews;
create policy "reviews read" on public.reviews for select using (true);
drop policy if exists "reviews insert" on public.reviews;
create policy "reviews insert" on public.reviews for insert to authenticated with check (student_id = auth.uid());
drop policy if exists "reviews admin" on public.reviews;
create policy "reviews admin" on public.reviews for delete to authenticated
  using (student_id = auth.uid() or public.has_role(auth.uid(), 'admin'));

-- bookings
drop policy if exists "bookings read" on public.bookings;
create policy "bookings read" on public.bookings for select to authenticated
  using (student_id = auth.uid() or landlord_id = auth.uid() or public.has_role(auth.uid(), 'admin'));
drop policy if exists "bookings insert" on public.bookings;
create policy "bookings insert" on public.bookings for insert to authenticated with check (student_id = auth.uid());
drop policy if exists "bookings update" on public.bookings;
create policy "bookings update" on public.bookings for update to authenticated
  using (student_id = auth.uid() or landlord_id = auth.uid() or public.has_role(auth.uid(), 'admin'));

-- payments
drop policy if exists "payments read" on public.payments;
create policy "payments read" on public.payments for select to authenticated using (
  student_id = auth.uid() or public.has_role(auth.uid(), 'admin')
  or exists (select 1 from public.bookings b where b.id = payments.booking_id and b.landlord_id = auth.uid()));
drop policy if exists "payments insert" on public.payments;
create policy "payments insert" on public.payments for insert to authenticated with check (
  student_id = auth.uid() or public.has_role(auth.uid(), 'admin')
  or exists (select 1 from public.bookings b where b.id = payments.booking_id and b.landlord_id = auth.uid()));
drop policy if exists "payments update" on public.payments;
create policy "payments update" on public.payments for update to authenticated using (
  student_id = auth.uid() or public.has_role(auth.uid(), 'admin')
  or exists (select 1 from public.bookings b where b.id = payments.booking_id and b.landlord_id = auth.uid()));

-- maintenance requests
drop policy if exists "maintenance read" on public.maintenance_requests;
create policy "maintenance read" on public.maintenance_requests for select to authenticated using (
  student_id = auth.uid() or public.has_role(auth.uid(), 'admin')
  or exists (select 1 from public.properties p where p.id = maintenance_requests.property_id and p.landlord_id = auth.uid()));
drop policy if exists "maintenance insert" on public.maintenance_requests;
create policy "maintenance insert" on public.maintenance_requests for insert to authenticated with check (student_id = auth.uid());
drop policy if exists "maintenance update" on public.maintenance_requests;
create policy "maintenance update" on public.maintenance_requests for update to authenticated using (
  student_id = auth.uid() or public.has_role(auth.uid(), 'admin')
  or exists (select 1 from public.properties p where p.id = maintenance_requests.property_id and p.landlord_id = auth.uid()));

-- conversations + messages
drop policy if exists "conversations read" on public.conversations;
create policy "conversations read" on public.conversations for select to authenticated
  using (student_id = auth.uid() or landlord_id = auth.uid() or public.has_role(auth.uid(), 'admin'));
drop policy if exists "conversations insert" on public.conversations;
create policy "conversations insert" on public.conversations for insert to authenticated
  with check (student_id = auth.uid() or landlord_id = auth.uid());
drop policy if exists "conversations update" on public.conversations;
create policy "conversations update" on public.conversations for update to authenticated
  using (student_id = auth.uid() or landlord_id = auth.uid());

drop policy if exists "messages read" on public.messages;
create policy "messages read" on public.messages for select to authenticated using (
  exists (select 1 from public.conversations c where c.id = messages.conversation_id
          and (c.student_id = auth.uid() or c.landlord_id = auth.uid())));
drop policy if exists "messages insert" on public.messages;
create policy "messages insert" on public.messages for insert to authenticated with check (
  sender_id = auth.uid() and exists (select 1 from public.conversations c where c.id = messages.conversation_id
          and (c.student_id = auth.uid() or c.landlord_id = auth.uid())));
drop policy if exists "messages update" on public.messages;
create policy "messages update" on public.messages for update to authenticated using (
  exists (select 1 from public.conversations c where c.id = messages.conversation_id
          and (c.student_id = auth.uid() or c.landlord_id = auth.uid())));

-- notifications: your own; admin may create for anyone
drop policy if exists "notifications read" on public.notifications;
create policy "notifications read" on public.notifications for select to authenticated using (user_id = auth.uid());
drop policy if exists "notifications insert" on public.notifications;
create policy "notifications insert" on public.notifications for insert to authenticated
  with check (user_id = auth.uid() or public.has_role(auth.uid(), 'admin'));
drop policy if exists "notifications update" on public.notifications;
create policy "notifications update" on public.notifications for update to authenticated using (user_id = auth.uid());

-- audit log: admin reads; signed-in users may add rows about themselves
drop policy if exists "audit read" on public.audit_logs;
create policy "audit read" on public.audit_logs for select to authenticated using (public.has_role(auth.uid(), 'admin'));
drop policy if exists "audit insert" on public.audit_logs;
create policy "audit insert" on public.audit_logs for insert to authenticated
  with check (actor_id = auth.uid() or public.has_role(auth.uid(), 'admin'));

-- complaints
drop policy if exists "complaints read" on public.complaints;
create policy "complaints read" on public.complaints for select to authenticated
  using (reporter_id = auth.uid() or public.has_role(auth.uid(), 'admin'));
drop policy if exists "complaints insert" on public.complaints;
create policy "complaints insert" on public.complaints for insert to authenticated with check (reporter_id = auth.uid());
drop policy if exists "complaints admin update" on public.complaints;
create policy "complaints admin update" on public.complaints for update to authenticated
  using (public.has_role(auth.uid(), 'admin'));

-- device tokens
drop policy if exists "Users manage own device tokens" on public.device_tokens;
create policy "Users manage own device tokens" on public.device_tokens for all to authenticated
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- ---------- Storage buckets ----------
-- property-images: public (photos visible to everyone). booking-documents: private (IDs, proofs, photos).
insert into storage.buckets (id, name, public) values ('property-images', 'property-images', true)
  on conflict (id) do update set public = true;
insert into storage.buckets (id, name, public) values ('booking-documents', 'booking-documents', false)
  on conflict (id) do nothing;

-- Files are stored under "<user id>/<folder>/<file>", so the first folder name = the owner.
drop policy if exists "property images public read" on storage.objects;
create policy "property images public read" on storage.objects for select using (bucket_id = 'property-images');
drop policy if exists "property images upload own" on storage.objects;
create policy "property images upload own" on storage.objects for insert to authenticated
  with check (bucket_id = 'property-images' and (storage.foldername(name))[1] = auth.uid()::text);
drop policy if exists "property images update own" on storage.objects;
create policy "property images update own" on storage.objects for update to authenticated
  using (bucket_id = 'property-images' and (storage.foldername(name))[1] = auth.uid()::text);
drop policy if exists "property images delete own" on storage.objects;
create policy "property images delete own" on storage.objects for delete to authenticated
  using (bucket_id = 'property-images' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists "documents upload own" on storage.objects;
create policy "documents upload own" on storage.objects for insert to authenticated
  with check (bucket_id = 'booking-documents' and (storage.foldername(name))[1] = auth.uid()::text);
drop policy if exists "documents update own" on storage.objects;
create policy "documents update own" on storage.objects for update to authenticated
  using (bucket_id = 'booking-documents' and (storage.foldername(name))[1] = auth.uid()::text);
-- Owner, admins, and landlords (to review a booking applicant's documents) can open them.
drop policy if exists "documents read" on storage.objects;
create policy "documents read" on storage.objects for select to authenticated using (
  bucket_id = 'booking-documents' and (
    (storage.foldername(name))[1] = auth.uid()::text
    or public.has_role(auth.uid(), 'admin')
    or public.has_role(auth.uid(), 'landlord')));

-- ---------- Starter data: a few South African universities ----------
insert into public.universities (name, short_name, city, province, students, average_rent)
select * from (values
  ('University of Pretoria', 'UP', 'Pretoria', 'Gauteng', 56000, 3800),
  ('University of the Witwatersrand', 'Wits', 'Johannesburg', 'Gauteng', 40000, 4500),
  ('University of Johannesburg', 'UJ', 'Johannesburg', 'Gauteng', 50000, 4000),
  ('Tshwane University of Technology', 'TUT', 'Pretoria', 'Gauteng', 60000, 3200),
  ('University of Cape Town', 'UCT', 'Cape Town', 'Western Cape', 29000, 5200),
  ('Stellenbosch University', 'SU', 'Stellenbosch', 'Western Cape', 32000, 4600),
  ('University of KwaZulu-Natal', 'UKZN', 'Durban', 'KwaZulu-Natal', 47000, 3500),
  ('Nelson Mandela University', 'NMU', 'Gqeberha', 'Eastern Cape', 28000, 3300),
  ('North-West University', 'NWU', 'Potchefstroom', 'North West', 64000, 3000),
  ('University of the Free State', 'UFS', 'Bloemfontein', 'Free State', 37000, 3100)
) as v(name, short_name, city, province, students, average_rent)
where not exists (select 1 from public.universities);

-- ======================== SUPABASE_NOTIFY_TRIGGERS.sql ========================
-- RentEase: landlord verification notifications + audit trail for decisions made from the Android app.
-- Run once in Supabase -> SQL Editor (after the web migrations). Safe to re-run.
--
-- Why this exists: the notifications table only lets a user insert rows for THEMSELVES, and the web app
-- works around that with a server function that holds the service-role key. The Android app must never
-- contain that key, so the same job is done inside the database instead:
--   * admin approves/rejects/asks for documents  -> the landlord gets a notification
--   * landlord submits documents                 -> every admin gets a notification
--   * every decision                              -> a row in audit_logs
-- Calls made with the service-role key (the website) are skipped, because the website already does all of
-- this itself; you will not get duplicate notifications.

create or replace function public.landlord_verification_changed()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_title text;
  v_body text;
  v_who text;
begin
  if new.verification_status is not distinct from old.verification_status then
    return new;
  end if;
  -- The website's server function (service role) already notifies and audits.
  if coalesce(auth.role(), '') = 'service_role' then
    return new;
  end if;

  if new.verification_status::text in ('verified', 'rejected', 'documents_required', 'pending') then
    v_title := case new.verification_status::text
      when 'verified' then 'Your landlord account is verified'
      when 'rejected' then 'Landlord verification rejected'
      when 'documents_required' then 'Additional documents required'
      else 'Landlord verification reset to pending'
    end;
    v_body := case new.verification_status::text
      when 'verified' then 'An administrator approved your verification documents. You can now submit properties for review and publish approved listings.'
      when 'rejected' then 'An administrator could not verify your account with the documents supplied. Review the notes on your onboarding page and resubmit.'
      when 'documents_required' then 'An administrator needs more paperwork before your landlord account can be verified. Please upload the requested documents on your onboarding page.'
      else 'Your landlord verification is being reviewed again by an administrator.'
    end;
    if coalesce(new.verification_notes, '') <> '' then
      v_body := v_body || E'\n\nAdministrator notes: ' || new.verification_notes;
    end if;

    insert into public.notifications (user_id, title, body, category, link)
    values (new.user_id, v_title, v_body, 'verification', '/landlord/onboarding');

    insert into public.audit_logs (actor_id, action, entity, entity_id, metadata)
    values (auth.uid(), 'landlord.' || new.verification_status::text, 'landlord_profiles', new.user_id,
            jsonb_build_object('notes', new.verification_notes, 'source', 'android'));

  elsif new.verification_status::text = 'submitted' then
    select coalesce(nullif(p.company_name, ''), nullif(p.full_name, ''), p.email, 'A landlord')
      into v_who
      from public.profiles p where p.id = new.user_id;

    insert into public.notifications (user_id, title, body, category, link)
    select ur.user_id,
           'Landlord verification submitted',
           coalesce(v_who, 'A landlord') || ' submitted verification documents and is awaiting review.',
           'verification',
           '/admin/verifications'
      from public.user_roles ur
     where ur.role = 'admin';
  end if;

  return new;
end;
$$;

drop trigger if exists landlord_verification_changed on public.landlord_profiles;
create trigger landlord_verification_changed
  after update of verification_status on public.landlord_profiles
  for each row execute function public.landlord_verification_changed();

-- ======================== SUPABASE_SIGNUP_FIX.sql ========================
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

-- ======================== SUPABASE_SETUP.sql ========================
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

