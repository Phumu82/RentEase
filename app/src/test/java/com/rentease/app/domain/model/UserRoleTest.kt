package com.rentease.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UserRoleTest {

    @Test
    fun `fromDb maps lowercase db values to the correct enum`() {
        assertEquals(UserRole.STUDENT, UserRole.fromDb("student"))
        assertEquals(UserRole.LANDLORD, UserRole.fromDb("landlord"))
        assertEquals(UserRole.ADMIN, UserRole.fromDb("admin"))
    }

    @Test
    fun `fromDb is case-insensitive since Postgres enum values are always lowercase but defend anyway`() {
        assertEquals(UserRole.STUDENT, UserRole.fromDb("STUDENT"))
        assertEquals(UserRole.LANDLORD, UserRole.fromDb("Landlord"))
    }

    @Test
    fun `fromDb throws on an unknown role string rather than silently defaulting`() {
        assertThrows(IllegalArgumentException::class.java) {
            UserRole.fromDb("super-admin")
        }
    }

    @Test
    fun `toDb round-trips back to the exact Postgres enum literal`() {
        assertEquals("student", UserRole.STUDENT.toDb())
        assertEquals("landlord", UserRole.LANDLORD.toDb())
        assertEquals("admin", UserRole.ADMIN.toDb())
    }
}
