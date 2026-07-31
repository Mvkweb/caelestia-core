package com.caelestia.paper.util

import java.util.UUID

object SkinUtil {
    fun getAvatarUrl(apiUrl: String, identifier: String, renderType: String = "head", scale: Int = 100): String {
        val baseUrl = apiUrl.trimEnd('/')
        return "$baseUrl/$renderType/$identifier/$scale"
    }
}
