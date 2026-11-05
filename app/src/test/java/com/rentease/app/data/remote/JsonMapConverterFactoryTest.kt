package com.rentease.app.data.remote

import com.rentease.app.domain.model.MaintenancePriority
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonMapConverterFactoryTest {

    private fun render(map: Map<String, Any?>): String =
        Json.encodeToString(JsonObject.serializer(), map.toJsonObject())

    @Test
    fun `primitives, null, lists and nested maps convert to the matching JSON`() {
        val json = render(
            linkedMapOf(
                "name" to "Room 1",
                "capacity" to 2,
                "price" to 1500.5,
                "furnished" to true,
                "photo" to null,
                "amenities" to listOf("WiFi", "Gym"),
                "meta" to mapOf("floor" to 3)
            )
        )
        assertEquals(
            """{"name":"Room 1","capacity":2,"price":1500.5,"furnished":true,"photo":null,"amenities":["WiFi","Gym"],"meta":{"floor":3}}""",
            json
        )
    }

    @Test
    fun `strings are escaped so user text cannot break the JSON`() {
        val json = render(mapOf("description" to "5\" screen, \"quoted\" \\ back"))
        val parsed = Json.parseToJsonElement(json) as JsonObject
        assertEquals("5\" screen, \"quoted\" \\ back", (parsed["description"] as JsonPrimitive).content)
    }

    @Test
    fun `null becomes JsonNull and enums use their name`() {
        assertTrue(null.toJsonElement() is JsonNull)
        assertEquals(JsonPrimitive("URGENT"), MaintenancePriority.URGENT.toJsonElement())
    }
}
