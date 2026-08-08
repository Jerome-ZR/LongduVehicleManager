package com.longdu.vehicle.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 维修地点管理器 — 持久化维修厂/地点列表，支持增删改
 */
class ShopManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("shops", Context.MODE_PRIVATE)

    private val _shops = MutableStateFlow<List<String>>(emptyList())
    val shops: StateFlow<List<String>> = _shops.asStateFlow()

    init { load() }

    private fun load() {
        val json = prefs.getString("shop_list", null)
        _shops.value = if (json.isNullOrBlank()) defaultShops
        else json.split("||").filter { it.isNotBlank() }
    }

    private fun save() {
        prefs.edit().putString("shop_list", _shops.value.joinToString("||")).apply()
    }

    fun add(name: String) {
        if (name.isBlank() || _shops.value.contains(name)) return
        _shops.value = _shops.value + name
        save()
    }

    fun rename(old: String, new: String) {
        if (new.isBlank()) return
        _shops.value = _shops.value.map { if (it == old) new else it }
        save()
    }

    fun delete(name: String) {
        _shops.value = _shops.value.filter { it != name }
        save()
    }

    companion object {
        val defaultShops = listOf("铁马机车生活馆", "洪亮机车", "现奔宝", "4S店", "其他")
    }
}
