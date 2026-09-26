package com.anacatavc.shoppinglist.data

import org.json.JSONArray
import org.json.JSONObject

/** Serializes the whole database to the backup format described in docs/architecture.md. */
fun backupJson(categories: List<Category>, items: List<Item>, exportedAt: Long): String {
    val categoriesJson = JSONArray()
    categories.forEach {
        categoriesJson.put(JSONObject().put("id", it.id).put("name", it.name).put("emoji", it.emoji))
    }
    val itemsJson = JSONArray()
    items.forEach {
        itemsJson.put(
            JSONObject()
                .put("id", it.id)
                .put("name", it.name)
                .put("quantity", it.quantity)
                .put("note", it.note)
                .put("categoryId", it.categoryId)
                .put("urgency", it.urgency.name)
                .put("recurrenceDays", it.recurrenceDays ?: JSONObject.NULL)
                .put("done", it.done)
                .put("lastBoughtAt", it.lastBoughtAt ?: JSONObject.NULL)
                .put("nextDueAt", it.nextDueAt ?: JSONObject.NULL),
        )
    }
    return JSONObject()
        .put("version", 1)
        .put("exportedAt", exportedAt)
        .put("categories", categoriesJson)
        .put("items", itemsJson)
        .toString(2)
}
