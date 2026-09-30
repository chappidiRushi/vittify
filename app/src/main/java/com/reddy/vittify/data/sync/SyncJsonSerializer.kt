package com.reddy.vittify.data.sync

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.reddy.vittify.data.backup.BigDecimalTypeAdapter
import com.reddy.vittify.data.backup.FlexibleStringTypeAdapter
import com.reddy.vittify.data.backup.LocalDateTypeAdapter
import com.reddy.vittify.data.backup.LocalDateTimeTypeAdapter
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncJsonSerializer @Inject constructor() {

    val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter())
        .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
        .registerTypeAdapter(BigDecimal::class.java, BigDecimalTypeAdapter())
        .registerTypeAdapter(String::class.java, FlexibleStringTypeAdapter())
        .create()

    fun toJson(any: Any): String = gson.toJson(any)

    fun <T> fromJson(json: String, clazz: Class<T>): T = gson.fromJson(json, clazz)
}

