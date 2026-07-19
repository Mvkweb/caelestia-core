package com.example.caelestiaprotection.claim

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import java.util.UUID

object ClaimSelection {
    private val selections = mutableMapOf<UUID, Selection>()

    fun setPos1(player: Player, pos: BlockPos) {
        val sel = selections.getOrPut(player.uuid) { Selection() }
        sel.pos1 = pos
    }

    fun setPos2(player: Player, pos: BlockPos) {
        val sel = selections.getOrPut(player.uuid) { Selection() }
        sel.pos2 = pos
    }

    fun getSelection(player: Player): Selection? {
        return selections[player.uuid]
    }

    fun clearSelection(player: Player) {
        selections.remove(player.uuid)
    }

    class Selection {
        var pos1: BlockPos? = null
        var pos2: BlockPos? = null
        
        fun isComplete(): Boolean {
            return pos1 != null && pos2 != null
        }
    }
}
