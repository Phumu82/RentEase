package com.rentease.app.domain.model

/**
 * Pure check for whether the build was given a real Supabase project. The Gradle script falls back to
 * `https://not-configured.invalid` and a blank key when `local.properties` has no SUPABASE_URL /
 * SUPABASE_ANON_KEY; every request then fails offline, so the app says so instead of showing "HTTP 503".
 */
object SupabaseConfig {
    const val PLACEHOLDER_HOST = "not-configured.invalid"

    fun isConfigured(url: String, anonKey: String): Boolean =
        url.isNotBlank() && anonKey.isNotBlank() &&
            !url.contains(PLACEHOLDER_HOST, ignoreCase = true) &&
            (url.startsWith("https://") || url.startsWith("http://"))

    /**
     * A base URL Retrofit will accept: trimmed, quotes removed, https:// added when the scheme was left off,
     * no trailing slash. Anything unusable becomes the placeholder host, so a typo in local.properties shows
     * the "not set up" banner instead of crashing the app while it starts.
     */
    fun normalizeUrl(raw: String): String {
        var url = raw.trim().removeSurrounding("\"").removeSurrounding("'").trim()
        if (url.isEmpty()) return "https://$PLACEHOLDER_HOST"
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
        val authority = url.substringAfter("://").substringBefore('/')
        val host = authority.substringBefore(':')
        if (host.isBlank() || authority.any { it.isWhitespace() }) return "https://$PLACEHOLDER_HOST"
        url = url.trimEnd('/')
        return url
    }
}
