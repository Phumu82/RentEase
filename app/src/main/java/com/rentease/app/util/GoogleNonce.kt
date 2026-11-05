package com.rentease.app.util

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Nonce handling for Google sign-in with Supabase (native ID-token flow).
 *
 * Google embeds whatever nonce string it is given into the ID token's `nonce`
 * claim. Supabase (GoTrue) verifies `sha256_hex(nonce sent in the request)`
 * against that claim, so the app must:
 *   1. generate a random RAW nonce,
 *   2. give Credential Manager the SHA-256 hex of the raw nonce ([Nonce.hashed]),
 *   3. send the RAW nonce to Supabase ([Nonce.raw]).
 */
object GoogleNonce {

    data class Nonce(val raw: String, val hashed: String)

    fun generate(random: SecureRandom = SecureRandom()): Nonce {
        val bytes = ByteArray(16).also { random.nextBytes(it) }
        val raw = bytes.toHex()
        return Nonce(raw = raw, hashed = sha256Hex(raw))
    }

    fun sha256Hex(input: String): String =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8)).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
