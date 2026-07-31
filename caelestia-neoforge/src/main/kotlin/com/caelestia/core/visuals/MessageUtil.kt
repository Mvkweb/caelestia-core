package com.caelestia.core.visuals

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.TextColor

object MessageUtil {
    fun colorHex(text: String, hex: String): MutableComponent {
        return Component.literal(text).withStyle { style ->
            style.withColor(TextColor.parseColor(hex).getOrThrow())
        }
    }
}
