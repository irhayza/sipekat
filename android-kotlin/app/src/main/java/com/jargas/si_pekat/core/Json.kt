package com.jargas.si_pekat.core

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.ToNumberPolicy

typealias JsonMap = Map<String, Any?>

/**
 * Pengganti dart:convert (jsonEncode/jsonDecode) untuk payload dinamis dari GAS.
 * Bilangan bulat dibaca sebagai Long (bukan Double) supaya "5" tidak menjadi "5.0",
 * persis seperti perilaku Dart.
 */
object Json {
    private val gson = GsonBuilder()
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .serializeNulls()
        .create()

    fun encode(value: Any?): String = gson.toJson(value)

    /** Mengembalikan Map / List / primitif, atau melempar JsonSyntaxException bila bukan JSON valid. */
    fun decode(text: String): Any? {
        if (text.isBlank()) throw JsonSyntaxException("kosong")
        return gson.fromJson(text, Any::class.java)
    }
}

@Suppress("UNCHECKED_CAST")
fun Any?.asJsonMap(): JsonMap? = this as? Map<String, Any?>

fun Any?.asList(): List<*>? = this as? List<*>

/** Setara `x?.toString()` di Dart. */
fun Any?.str(): String? = this?.toString()
