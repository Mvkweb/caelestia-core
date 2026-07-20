package com.caelestia.paper.listeners

import org.bukkit.entity.ExperienceOrb
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntitySpawnEvent
import com.caelestia.paper.CaelestiaPlugin

class XpClumpsListener(private val plugin: CaelestiaPlugin) : Listener {

    @EventHandler
    fun onEntitySpawn(event: EntitySpawnEvent) {
        if (!plugin.caelestiaConfig.featXpClumps) return

        val maxOrbValue = plugin.caelestiaConfig.xpClumpsMaxValue
        val mergeRadius = plugin.caelestiaConfig.xpClumpsMergeRadius

        val entity = event.entity
        if (entity is ExperienceOrb) {
            val nearbyOrbs = entity.getNearbyEntities(mergeRadius, mergeRadius, mergeRadius)
                .filterIsInstance<ExperienceOrb>()
                .filter { it.experience < maxOrbValue && it.uniqueId != entity.uniqueId }

            if (nearbyOrbs.isNotEmpty()) {
                val targetOrb = nearbyOrbs.first()
                val spaceLeft = maxOrbValue - targetOrb.experience

                if (entity.experience <= spaceLeft) {
                    targetOrb.experience += entity.experience
                    event.isCancelled = true // Cancel this spawn
                } else {
                    targetOrb.experience = maxOrbValue
                    entity.experience -= spaceLeft
                    // The entity continues to spawn with the remaining value
                }
            }
        }
    }
}
