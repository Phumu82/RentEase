package com.rentease.app.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

/**
 * Retrofit request-body converter for `Map<String, Any?>` bodies.
 *
 * Several PostgREST PATCH/POST endpoints (profile updates, property/room
 * create/update) send a partial row as a `Map<String, Any?>` because the set of
 * columns changes per call. `converter-kotlinx-serialization` cannot serialise
 * `Any` ("Serializer for class 'Any' is not found"), so without this factory
 * Retrofit throws as soon as one of those calls is made. This factory converts
 * the map to a [JsonObject] by value type (String, Boolean, Number, null,
 * collections, nested maps) and must be registered BEFORE the kotlinx converter.
 * It returns null for every other body type so the normal converter still
 * handles typed DTOs and `Map<String, String>`.
 */
class JsonMapConverterFactory(private val json: Json) : Converter.Factory() {

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? {
        if (!isStringToAnyMap(type)) return null
        return Converter<Map<String, Any?>, RequestBody> { value ->
            json.encodeToString(JsonObject.serializer(), value.toJsonObject())
                .toRequestBody(JSON_MEDIA_TYPE)
        }
    }

    private fun isStringToAnyMap(type: Type): Boolean {
        if (type !is ParameterizedType) return false
        if (type.rawType != Map::class.java) return false
        val args = type.actualTypeArguments
        if (args.size != 2) return false
        return upperBound(args[0]) == String::class.java && upperBound(args[1]) == Any::class.java
    }

    private fun upperBound(type: Type): Type =
        if (type is WildcardType) type.upperBounds.firstOrNull() ?: Any::class.java else type

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

/** Converts an arbitrary-valued map into a JSON object (see [JsonMapConverterFactory]). */
fun Map<String, Any?>.toJsonObject(): JsonObject =
    JsonObject(mapValues { (_, value) -> value.toJsonElement() })

/** Converts a plain Kotlin value into the matching [JsonElement]. */
fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Enum<*> -> JsonPrimitive(name)
    is Map<*, *> -> JsonObject(entries.associate { (k, v) -> k.toString() to v.toJsonElement() })
    is Iterable<*> -> JsonArray(map { it.toJsonElement() })
    is Array<*> -> JsonArray(map { it.toJsonElement() })
    else -> JsonPrimitive(toString())
}
