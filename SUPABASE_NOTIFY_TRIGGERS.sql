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
