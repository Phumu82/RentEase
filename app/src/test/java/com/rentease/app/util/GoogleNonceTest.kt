package com.rentease.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleNonceTest {

    @Test
    fun `sha256Hex matches the known SHA-256 test vector for abc`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            GoogleNonce.sha256Hex("abc")
        )
    }

    @Test
    fun `generate returns a 32-char hex raw nonce whose hash is the hashed nonce`() {
        val nonce = GoogleNonce.generate()
        assertEquals(32, nonce.raw.length)
        assertTrue(nonce.raw.all { it in '0'..'9' || it in 'a'..'f' })
        assertEquals(GoogleNonce.sha256Hex(nonce.raw), nonce.hashed)
        assertEquals(64, nonce.hashed.length)
    }

    @Test
    fun `each generated nonce is different`() {
        assertNotEquals(GoogleNonce.generate().raw, GoogleNonce.generate().raw)
    }
}
