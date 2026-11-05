package com.rentease.app.data.remote

import com.rentease.app.BuildConfig
import com.rentease.app.domain.model.SupabaseConfig

/** The one place the Supabase base URL is read, already cleaned up (see [SupabaseConfig.normalizeUrl]). */
object SupabaseEndpoint {
    val baseUrl: String = SupabaseConfig.normalizeUrl(BuildConfig.SUPABASE_URL)
}
