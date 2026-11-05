package com.rentease.app.data.remote

import android.content.Context
import com.rentease.app.BuildConfig
import com.rentease.app.R
import com.rentease.app.domain.model.SupabaseConfig

/**
 * Suffix appended to "could not load X" messages. Instead of a bare "(HTTP 503)" it says what is actually
 * wrong: the build has no Supabase project configured, or the server cannot be reached.
 */
fun Context.httpHint(code: Int): String = when {
    !SupabaseConfig.isConfigured(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) ->
        " – ${getString(R.string.error_not_configured)}"
    code == 503 -> " – ${getString(R.string.error_no_connection)}"
    else -> " (HTTP $code)"
}
