// supabase/functions/admin-access/index.ts
//
// Mirrors src/lib/admin-access.server.ts from the web app EXACTLY:
//   1. SHA-256 digest + timing-safe compare of the submitted code against a
//      server-only secret (ADMIN_ACCESS_CODE) — never exposed to any client.
//   2. On match: upsert public.user_roles(user_id, role='admin') using the
//      service-role key (never shipped to the app).
//   3. Insert an audit_logs row recording the grant.
//
// This exists because Postgres/PostgREST has no access to server-only secrets,
// so the Android app cannot replicate this check via a plain REST call — it
// MUST go through an Edge Function that runs with Deno's server-side env.
//
// Deploy with:
//   supabase functions deploy admin-access
//   supabase secrets set ADMIN_ACCESS_CODE=your-secret-code
//
// The function already has SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY
// available automatically in the Edge Function runtime — you do not set
// those two yourself.

import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

function timingSafeEqual(a: Uint8Array, b: Uint8Array): boolean {
  if (a.length !== b.length) return false;
  let result = 0;
  for (let i = 0; i < a.length; i++) {
    result |= a[i] ^ b[i];
  }
  return result === 0;
}

async function sha256(value: string): Promise<Uint8Array> {
  const data = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest("SHA-256", data);
  return new Uint8Array(digest);
}

serve(async (req) => {
  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), { status: 405 });
  }

  const expectedCode = Deno.env.get("ADMIN_ACCESS_CODE");
  if (!expectedCode) {
    // Fail closed — misconfiguration should never accidentally grant access.
    return new Response(JSON.stringify({ error: "Server not configured" }), { status: 500 });
  }

  // Identify the calling user from their bearer token (the same one the rest
  // of the app already uses) — this function grants admin to WHOEVER is
  // currently signed in, so the caller must already have a valid session.
  const authHeader = req.headers.get("Authorization");
  if (!authHeader) {
    return new Response(JSON.stringify({ error: "Not authenticated" }), { status: 401 });
  }

  const anonClient = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: authHeader } } }
  );
  const { data: userData, error: userError } = await anonClient.auth.getUser();
  if (userError || !userData?.user) {
    return new Response(JSON.stringify({ error: "Not authenticated" }), { status: 401 });
  }
  const userId = userData.user.id;

  let body: { code?: string };
  try {
    body = await req.json();
  } catch {
    return new Response(JSON.stringify({ error: "Invalid request body" }), { status: 400 });
  }
  if (!body.code) {
    return new Response(JSON.stringify({ error: "Missing code" }), { status: 400 });
  }

  const [submittedDigest, expectedDigest] = await Promise.all([
    sha256(body.code),
    sha256(expectedCode),
  ]);

  if (!timingSafeEqual(submittedDigest, expectedDigest)) {
    // Deliberately generic error — never reveal whether the code was "close".
    return new Response(JSON.stringify({ error: "Invalid code" }), { status: 403 });
  }

  const adminClient = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
  );

  const { error: upsertError } = await adminClient
    .from("user_roles")
    .upsert({ user_id: userId, role: "admin" }, { onConflict: "user_id,role" });
  if (upsertError) {
    return new Response(JSON.stringify({ error: upsertError.message }), { status: 500 });
  }

  await adminClient.from("audit_logs").insert({
    actor_id: userId,
    action: "admin.access_granted",
    entity: "user_roles",
    entity_id: userId,
  });

  return new Response(JSON.stringify({ success: true }), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
});
