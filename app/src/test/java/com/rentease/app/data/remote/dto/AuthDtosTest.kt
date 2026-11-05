package com.rentease.app.data.remote.dto

import com.rentease.app.data.remote.RetrofitFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthDtosTest {
    private val json = RetrofitFactory.json

    @Test
    fun `a profile with a null full_name still decodes so sign-in does not fail`() {
        val profile = json.decodeFromString<ProfileDto>("""{"id":"u1","full_name":null,"email":"admin@rentease.com"}""")
        assertEquals("", profile.fullName)
        assertEquals("admin@rentease.com", profile.email)
    }

    @Test
    fun `a profile with the name missing entirely decodes`() {
        assertEquals("", json.decodeFromString<ProfileDto>("""{"id":"u1"}""").fullName)
    }

    @Test
    fun `a sign-up answer with email confirmation on has no tokens`() {
        val body = json.decodeFromString<SignUpResponseDto>("""{"id":"u1","email":"t@example.com","role":"authenticated"}""")
        assertNull(body.toSession())
    }

    @Test
    fun `a sign-up answer with a session converts to one`() {
        val body = json.decodeFromString<SignUpResponseDto>(
            """{"access_token":"a","refresh_token":"r","expires_in":3600,"token_type":"bearer","user":{"id":"u1","email":"t@example.com"}}"""
        )
        assertEquals("u1", body.toSession()?.user?.id)
    }
}
