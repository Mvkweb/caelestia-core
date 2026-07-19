package com.example.caelestiaprotection.core

import com.example.caelestiaprotection.claim.ClaimSelection
import com.example.caelestiaprotection.visuals.MessageUtil
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.CommandEvent
import net.minecraft.commands.Commands
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore
import net.minecraft.ChatFormatting
import xaero.pac.common.server.api.OpenPACServerAPI
import net.neoforged.neoforge.items.ItemHandlerHelper

@EventBusSubscriber(modid = CaelestiaProtection.MODID, bus = EventBusSubscriber.Bus.GAME)
object EventHandlers {

    private val lastClaimOwnerMap = mutableMapOf<java.util.UUID, java.util.UUID?>()

    fun isClaimWand(stack: ItemStack): Boolean {
        if (stack.item != Items.STICK) return false
        val customData = stack.get(DataComponents.CUSTOM_DATA)
        return customData != null && customData.contains("IsClaimWand")
    }

    @SubscribeEvent
    fun onCommandEvent(event: CommandEvent) {
        val command = event.parseResults.reader.string
        if (command.startsWith("/openpac claims") || command.startsWith("/opac claims")) {
            val player = event.parseResults.context.source.player
            if (player != null) {
                event.isCanceled = true
                player.sendSystemMessage(MessageUtil.colorHex("Please use the Claim Wand (/caelestiaclaim) to manage your claims!", "#FF5555"))
            }
        }
    }

    @SubscribeEvent
    fun onServerChat(event: net.neoforged.neoforge.event.ServerChatEvent) {
        val player = event.player
        val message = event.message.string
        
        val bridgeMsg = com.example.caelestiaprotection.bridge.BridgeMessage(
            source = com.example.caelestiaprotection.bridge.Source.NEOFORGE,
            type = com.example.caelestiaprotection.bridge.Type.CHAT,
            playerName = player.name.string,
            playerUuid = player.uuid.toString(),
            message = message
        )
        CaelestiaProtection.bridgeClient?.broadcast(bridgeMsg)
    }

    @SubscribeEvent
    fun onPlayerJoin(event: net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity
        val bridgeMsg = com.example.caelestiaprotection.bridge.BridgeMessage(
            source = com.example.caelestiaprotection.bridge.Source.NEOFORGE,
            type = com.example.caelestiaprotection.bridge.Type.JOIN,
            playerName = player.name.string,
            playerUuid = player.uuid.toString(),
            message = ""
        )
        CaelestiaProtection.bridgeClient?.broadcast(bridgeMsg)
    }

    @SubscribeEvent
    fun onPlayerQuit(event: net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent) {
        val player = event.entity
        val bridgeMsg = com.example.caelestiaprotection.bridge.BridgeMessage(
            source = com.example.caelestiaprotection.bridge.Source.NEOFORGE,
            type = com.example.caelestiaprotection.bridge.Type.QUIT,
            playerName = player.name.string,
            playerUuid = player.uuid.toString(),
            message = ""
        )
        CaelestiaProtection.bridgeClient?.broadcast(bridgeMsg)
    }

    @SubscribeEvent
    fun onPlayerDeath(event: net.neoforged.neoforge.event.entity.living.LivingDeathEvent) {
        val entity = event.entity
        if (entity is net.minecraft.world.entity.player.Player) {
            val bridgeMsg = com.example.caelestiaprotection.bridge.BridgeMessage(
                source = com.example.caelestiaprotection.bridge.Source.NEOFORGE,
                type = com.example.caelestiaprotection.bridge.Type.DEATH,
                playerName = entity.name.string,
                playerUuid = entity.uuid.toString(),
                message = event.source.getLocalizedDeathMessage(entity).string
            )
            CaelestiaProtection.bridgeClient?.broadcast(bridgeMsg)
        }
    }

    @SubscribeEvent
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        val dispatcher = event.dispatcher

        // Intercept native OPAC claim commands to guide players to the wand
        val disableMessage = net.minecraft.network.chat.Component.literal("Native claiming is disabled. Please use the /caelestiaclaim command to get the claim wand.")
        
        val disableAction = com.mojang.brigadier.Command<net.minecraft.commands.CommandSourceStack> { context ->
            context.source.sendFailure(disableMessage)
            0
        }

        // Intercept /opac claims claim and /opac claims unclaim
        dispatcher.register(
            Commands.literal("opac")
                .then(Commands.literal("claims")
                    .then(Commands.literal("claim").executes(disableAction))
                    .then(Commands.literal("unclaim").executes(disableAction))
                )
        )

        // Intercept /opac-claims claim and /opac-claims unclaim
        dispatcher.register(
            Commands.literal("opac-claims")
                .then(Commands.literal("claim").executes(disableAction))
                .then(Commands.literal("unclaim").executes(disableAction))
        )

        // Intercept /openpac-claims claim and /openpac-claims unclaim
        dispatcher.register(
            Commands.literal("openpac-claims")
                .then(Commands.literal("claim").executes(disableAction))
                .then(Commands.literal("unclaim").executes(disableAction))
        )

        dispatcher.register(
            Commands.literal("caelestiaclaim").executes { context ->
                val player = context.source.playerOrException
                
                val wand = ItemStack(Items.STICK)
                
                val tag = CompoundTag()
                tag.putBoolean("IsClaimWand", true)
                wand.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))
                wand.set(DataComponents.CUSTOM_NAME, MessageUtil.colorHex("Caelestia Claim Wand", "#55FF55").withStyle(ChatFormatting.BOLD))
                
                val loreLines = listOf(
                    Component.empty(),
                    MessageUtil.colorHex("ʟᴇꜰᴛ ᴄʟɪᴄᴋ ᴛᴏ ꜱᴇᴛ ᴘᴏꜱɪᴛɪᴏɴ 1", "#AAAAAA"),
                    MessageUtil.colorHex("ʀɪɢʜᴛ ᴄʟɪᴄᴋ ᴛᴏ ꜱᴇᴛ ᴘᴏꜱɪᴛɪᴏɴ 2", "#AAAAAA"),
                    MessageUtil.colorHex("ꜱʜɪꜰᴛ + ʀɪɢʜᴛ ᴄʟɪᴄᴋ ᴛᴏ ᴄʟᴀɪᴍ", "#FFAA00")
                )
                wand.set(DataComponents.LORE, ItemLore(loreLines))
                
                ItemHandlerHelper.giveItemToPlayer(player, wand)
                
                context.source.sendSuccess({ Component.literal("§aClaim wand received! §7Left click Pos1, Right click Pos2, Shift+Right click to confirm.") }, false)
                1
            }
        )

        event.dispatcher.register(
            Commands.literal("unclaim")
                .executes { context ->
                    val player = context.source.playerOrException
                    val chunkPos = net.minecraft.world.level.ChunkPos(player.blockPosition())
                    val dimension = player.level().dimension().location()
                    
                    val claimsAPI = OpenPACServerAPI.get(player.server).serverClaimsManager
                    val result = claimsAPI.tryToUnclaim(dimension, player.uuid, chunkPos.x, chunkPos.z, chunkPos.x, chunkPos.z, true)
                    
                    if (result.resultType.success) {
                        player.sendSystemMessage(MessageUtil.colorHex("Chunk unclaimed successfully!", "#55FF55"))
                    } else {
                        player.sendSystemMessage(MessageUtil.colorHex("Failed to unclaim. ${result.resultType.message.string}", "#FF5555"))
                    }
                    1
                }
                .then(Commands.literal("area").executes { context ->
                    val player = context.source.playerOrException
                    val selection = ClaimSelection.getSelection(player)
                    if (selection != null && selection.isComplete()) {
                        val chunk1 = net.minecraft.world.level.ChunkPos(selection.pos1!!)
                        val chunk2 = net.minecraft.world.level.ChunkPos(selection.pos2!!)
                        val dimension = player.level().dimension().location()
                        
                        val minX = minOf(chunk1.x, chunk2.x)
                        val maxX = maxOf(chunk1.x, chunk2.x)
                        val minZ = minOf(chunk1.z, chunk2.z)
                        val maxZ = maxOf(chunk1.z, chunk2.z)

                        val serverPlayer = player as? net.minecraft.server.level.ServerPlayer
                        if (serverPlayer != null) {
                            val claimsAPI = OpenPACServerAPI.get(serverPlayer.server).serverClaimsManager
                            val playerChunkPos = net.minecraft.world.level.ChunkPos(player.blockPosition())
                            
                            val result = claimsAPI.tryToUnclaimArea(dimension, player.uuid, playerChunkPos.x, playerChunkPos.z, minX, minZ, maxX, maxZ, true)
                            
                            val success = result.resultTypesStream.anyMatch { it.success }
                            if (success) {
                                player.sendSystemMessage(MessageUtil.colorHex("Unclaimed area successfully!", "#55FF55"))
                                ClaimSelection.clearSelection(player)
                            } else {
                                val failMsg = result.resultTypesStream.filter { it.fail }.findFirst().map { it.message.string }.orElse("Do you own these chunks?")
                                player.sendSystemMessage(MessageUtil.colorHex("Failed to unclaim area. $failMsg", "#FF5555"))
                            }
                        }
                    } else {
                        player.sendSystemMessage(MessageUtil.colorHex("You must select Pos1 and Pos2 with your wand first!", "#FF5555"))
                    }
                    1
                })
                .then(Commands.literal("all").executes { context ->
                    val player = context.source.playerOrException
                    val serverPlayer = player as? net.minecraft.server.level.ServerPlayer ?: return@executes 0
                    
                    val dimension = player.level().dimension().location()
                    val startChunk = net.minecraft.world.level.ChunkPos(player.blockPosition())
                    
                    val claimsManager = OpenPACServerAPI.get(serverPlayer.server).serverClaimsManager
                    val partyAPI = OpenPACServerAPI.get(serverPlayer.server).partyManager.getPartyByMember(player.uuid)
                    val ownerId = partyAPI?.id ?: player.uuid
                    
                    val claim = claimsManager.get(dimension, startChunk.x, startChunk.z)
                    if (claim == null || claim.playerId != ownerId) {
                        player.sendSystemMessage(MessageUtil.colorHex("You do not own the claim you are standing in!", "#FF5555"))
                        return@executes 1
                    }
                    
                    val visited = mutableSetOf<net.minecraft.world.level.ChunkPos>()
                    val queue = java.util.ArrayDeque<net.minecraft.world.level.ChunkPos>()
                    queue.add(startChunk)
                    visited.add(startChunk)
                    
                    var unclaimedCount = 0
                    
                    while (queue.isNotEmpty() && unclaimedCount < 500) { // Limit to 500 chunks just in case
                        val current = queue.poll()
                        val currentClaim = claimsManager.get(dimension, current.x, current.z)
                        
                        if (currentClaim != null && currentClaim.playerId == ownerId) {
                            claimsManager.tryToUnclaim(dimension, player.uuid, player.chunkPosition().x, player.chunkPosition().z, current.x, current.z, true)
                            unclaimedCount++
                            
                            val neighbors = listOf(
                                net.minecraft.world.level.ChunkPos(current.x + 1, current.z),
                                net.minecraft.world.level.ChunkPos(current.x - 1, current.z),
                                net.minecraft.world.level.ChunkPos(current.x, current.z + 1),
                                net.minecraft.world.level.ChunkPos(current.x, current.z - 1)
                            )
                            
                            for (n in neighbors) {
                                if (visited.add(n)) {
                                    queue.add(n)
                                }
                            }
                        }
                    }
                    
                    player.sendSystemMessage(MessageUtil.colorHex("Unclaimed $unclaimedCount connected chunks!", "#55FF55"))
                    1
                })
        )

        // Caelestia Friends (Whitelist) Command via Alliances
        dispatcher.register(
            Commands.literal("caelestia-friends")
                .then(Commands.literal("add")
                    .then(Commands.argument("playerName", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes { context ->
                            val player = context.source.playerOrException
                            val serverPlayer = player as? net.minecraft.server.level.ServerPlayer ?: return@executes 0
                            val targetName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "playerName")
                            
                            val server = serverPlayer.server
                            server.profileCache?.get(targetName)?.ifPresentOrElse({ profile ->
                                val targetUuid = profile.id
                                val partyManager = OpenPACServerAPI.get(server).partyManager
                                
                                var executorParty = partyManager.getPartyByOwner(player.uuid)
                                if (executorParty == null) {
                                    executorParty = partyManager.createPartyForOwner(player)
                                }
                                
                                if (executorParty == null) {
                                    player.sendSystemMessage(MessageUtil.colorHex("Failed to create a friends list for you!", "#FF5555"))
                                    return@ifPresentOrElse
                                }
                                
                                var targetParty = partyManager.getPartyByOwner(targetUuid)
                                if (targetParty == null) {
                                    val onlineTarget = server.playerList.getPlayer(targetUuid)
                                    if (onlineTarget != null) {
                                        targetParty = partyManager.createPartyForOwner(onlineTarget)
                                    }
                                }
                                
                                if (targetParty == null) {
                                    player.sendSystemMessage(MessageUtil.colorHex("Player $targetName must join the server first so their profile is created!", "#FF5555"))
                                    return@ifPresentOrElse
                                }
                                
                                if (executorParty.id == targetParty.id) {
                                    player.sendSystemMessage(MessageUtil.colorHex("You cannot add yourself!", "#FF5555"))
                                    return@ifPresentOrElse
                                }
                                
                                if (executorParty.isAlly(targetParty.id)) {
                                    player.sendSystemMessage(MessageUtil.colorHex("$targetName is already in your friends list!", "#FF5555"))
                                    return@ifPresentOrElse
                                }
                                
                                executorParty.addAllyParty(targetParty.id)
                                player.sendSystemMessage(MessageUtil.colorHex("Added $targetName to your friends list!", "#55FF55"))
                            }, {
                                player.sendSystemMessage(MessageUtil.colorHex("Player $targetName not found!", "#FF5555"))
                            })
                            1
                        }
                    )
                )
                .then(Commands.literal("remove")
                    .then(Commands.argument("playerName", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes { context ->
                            val player = context.source.playerOrException
                            val serverPlayer = player as? net.minecraft.server.level.ServerPlayer ?: return@executes 0
                            val targetName = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "playerName")
                            
                            val partyManager = OpenPACServerAPI.get(serverPlayer.server).partyManager
                            val executorParty = partyManager.getPartyByOwner(player.uuid)
                            
                            if (executorParty == null) {
                                player.sendSystemMessage(MessageUtil.colorHex("You don't have a friends list!", "#FF5555"))
                                return@executes 1
                            }
                            
                            val allyToRemove = executorParty.allyPartiesStream.filter { ally -> 
                                val allyParty = partyManager.getPartyById(ally.partyId)
                                allyParty?.owner?.username?.equals(targetName, ignoreCase = true) == true
                            }.findFirst().orElse(null)
                            
                            if (allyToRemove != null) {
                                executorParty.removeAllyParty(allyToRemove.partyId)
                                player.sendSystemMessage(MessageUtil.colorHex("Removed $targetName from your friends list!", "#55FF55"))
                            } else {
                                player.sendSystemMessage(MessageUtil.colorHex("$targetName is not in your friends list!", "#FF5555"))
                            }
                            1
                        }
                    )
                )
                .then(Commands.literal("list")
                    .executes { context ->
                        val player = context.source.playerOrException
                        val serverPlayer = player as? net.minecraft.server.level.ServerPlayer ?: return@executes 0
                        
                        val partyManager = OpenPACServerAPI.get(serverPlayer.server).partyManager
                        val executorParty = partyManager.getPartyByOwner(player.uuid)
                        
                        if (executorParty == null || executorParty.allyCount == 0) {
                            player.sendSystemMessage(MessageUtil.colorHex("Your friends list is empty!", "#FFAA00"))
                            return@executes 1
                        }
                        
                        val friends = executorParty.allyPartiesStream.map { ally -> 
                            partyManager.getPartyById(ally.partyId)?.owner?.username ?: "Unknown"
                        }.toList().joinToString(", ")
                        
                        player.sendSystemMessage(MessageUtil.colorHex("Your Whitelist: $friends", "#55FF55"))
                        1
                    }
                )
        )
    }

    @SubscribeEvent
    fun onLeftClickBlock(event: PlayerInteractEvent.LeftClickBlock) {
        val player = event.entity
        val item = player.mainHandItem
        val level = event.level
        val pos = event.pos

        if (event.hand != net.minecraft.world.InteractionHand.MAIN_HAND) return

        if (isClaimWand(item)) {
            event.isCanceled = true

            if (!level.isClientSide) {
                ClaimSelection.setPos1(player, pos)
                player.sendSystemMessage(MessageUtil.colorHex("Position 1 set to ${pos.x}, ${pos.y}, ${pos.z}", "#55FF55"))
            } else {
                ClaimSelection.setPos1(player, pos)
            }
        }
    }

    @SubscribeEvent
    fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
        val player = event.entity
        val item = player.mainHandItem
        val level = event.level
        val pos = event.pos

        if (event.hand != net.minecraft.world.InteractionHand.MAIN_HAND) return

        if (isClaimWand(item)) {
            event.isCanceled = true
            
            if (player.isShiftKeyDown) {
                if (!level.isClientSide) processClaimConfirmation(player)
            } else {
                ClaimSelection.setPos2(player, pos)
                if (!level.isClientSide) {
                    player.sendSystemMessage(MessageUtil.colorHex("Position 2 set to ${pos.x}, ${pos.y}, ${pos.z}", "#55FF55"))
                }
            }
        }
    }

    @SubscribeEvent
    fun onRightClickItem(event: PlayerInteractEvent.RightClickItem) {
        val player = event.entity
        val item = player.mainHandItem
        val level = event.level

        if (event.hand != net.minecraft.world.InteractionHand.MAIN_HAND) return

        if (isClaimWand(item)) {
            if (player.isShiftKeyDown) {
                if (!level.isClientSide) processClaimConfirmation(player)
            }
        }
    }

    private fun processClaimConfirmation(player: net.minecraft.world.entity.player.Player) {
        val selection = ClaimSelection.getSelection(player)
        if (selection != null && selection.isComplete()) {
            val chunk1 = net.minecraft.world.level.ChunkPos(selection.pos1!!)
            val chunk2 = net.minecraft.world.level.ChunkPos(selection.pos2!!)
            val dimension = player.level().dimension().location()
            
            val minX = minOf(chunk1.x, chunk2.x)
            val maxX = maxOf(chunk1.x, chunk2.x)
            val minZ = minOf(chunk1.z, chunk2.z)
            val maxZ = maxOf(chunk1.z, chunk2.z)

            val serverPlayer = player as? net.minecraft.server.level.ServerPlayer ?: return
            val claimsAPI = OpenPACServerAPI.get(serverPlayer.server).serverClaimsManager
            
            val playerChunkPos = net.minecraft.world.level.ChunkPos(player.blockPosition())
            
            val partyAPI = OpenPACServerAPI.get(serverPlayer.server).partyManager.getPartyByMember(player.uuid)
            val ownerId = partyAPI?.id ?: player.uuid
            
            // Check for overlaps with other claims
            var overlaps = false
            for (x in minX..maxX) {
                for (z in minZ..maxZ) {
                    val existingClaim = claimsAPI.get(dimension, x, z)
                    if (existingClaim != null && existingClaim.playerId != ownerId) {
                        overlaps = true
                        break
                    }
                }
                if (overlaps) break
            }
            
            if (overlaps) {
                player.sendSystemMessage(MessageUtil.colorHex("Failed! Your selection overlaps with an existing claim owned by someone else.", "#FF5555"))
                return
            }
            
            val result = claimsAPI.tryToClaimArea(dimension, ownerId, 0, playerChunkPos.x, playerChunkPos.z, minX, minZ, maxX, maxZ, false)
            
            val success = result.resultTypesStream.anyMatch { it.success }
            if (success) {
                player.sendSystemMessage(MessageUtil.colorHex("Claimed area successfully!", "#55FF55"))
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY)
                ClaimSelection.clearSelection(player)
            } else {
                val failMsg = result.resultTypesStream.filter { it.fail }.findFirst().map { it.message.string }.orElse("Perhaps it is already claimed or too large.")
                player.sendSystemMessage(MessageUtil.colorHex("Failed to claim area. $failMsg", "#FF5555"))
            }
        } else {
            player.sendSystemMessage(MessageUtil.colorHex("You must select Pos1 and Pos2 first!", "#FF5555"))
        }
    }

    @SubscribeEvent
    fun onServerTick(event: net.neoforged.neoforge.event.tick.ServerTickEvent.Post) {
        val server = event.server
        if (server.tickCount % 5 != 0) return

        for (player in server.playerList.players) {
            val item = player.mainHandItem
            if (!isClaimWand(item)) continue
            
            val selection = ClaimSelection.getSelection(player) ?: continue
            val level = player.level() as? net.minecraft.server.level.ServerLevel ?: continue

            selection.pos1?.let { pos ->
                for (y in 0..20 step 2) {
                    level.sendParticles(player, net.minecraft.core.particles.ParticleTypes.END_ROD, false, pos.x + 0.5, pos.y + y.toDouble(), pos.z + 0.5, 1, 0.0, 0.0, 0.0, 0.0)
                }
            }

            selection.pos2?.let { pos ->
                for (y in 0..20 step 2) {
                    level.sendParticles(player, net.minecraft.core.particles.ParticleTypes.END_ROD, false, pos.x + 0.5, pos.y + y.toDouble(), pos.z + 0.5, 1, 0.0, 0.0, 0.0, 0.0)
                }
            }
            
            if (selection.isComplete()) {
                val chunk1 = net.minecraft.world.level.ChunkPos(selection.pos1!!)
                val chunk2 = net.minecraft.world.level.ChunkPos(selection.pos2!!)
                
                val minX = minOf(chunk1.minBlockX, chunk2.minBlockX)
                val maxX = maxOf(chunk1.maxBlockX, chunk2.maxBlockX)
                val minZ = minOf(chunk1.minBlockZ, chunk2.minBlockZ)
                val maxZ = maxOf(chunk1.maxBlockZ, chunk2.maxBlockZ)
                
                val y = player.y + 1.0
                
                for (x in minX..maxX step 2) {
                    level.sendParticles(player, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, false, x + 0.5, y, minZ.toDouble(), 1, 0.0, 0.0, 0.0, 0.0)
                    level.sendParticles(player, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, false, x + 0.5, y, maxZ + 1.0, 1, 0.0, 0.0, 0.0, 0.0)
                }
                for (z in minZ..maxZ step 2) {
                    level.sendParticles(player, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, false, minX.toDouble(), y, z + 0.5, 1, 0.0, 0.0, 0.0, 0.0)
                    level.sendParticles(player, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, false, maxX + 1.0, y, z + 0.5, 1, 0.0, 0.0, 0.0, 0.0)
                }
            }
        }
        
        // Custom Action Bar messages
        if (server.tickCount % 10 == 0) {
            val claimsManager = OpenPACServerAPI.get(server).serverClaimsManager
            for (player in server.playerList.players) {
                val chunkPos = net.minecraft.world.level.ChunkPos(player.blockPosition())
                val dimension = player.level().dimension().location()
                
                val claim = claimsManager.get(dimension, chunkPos.x, chunkPos.z)
                val currentOwner = claim?.playerId
                val lastOwner = lastClaimOwnerMap[player.uuid]

                if (currentOwner != lastOwner) {
                    lastClaimOwnerMap[player.uuid] = currentOwner
                    
                    if (currentOwner != null) {
                        try {
                            val ownerInfo = claimsManager.getPlayerInfo(currentOwner)
                            val ownerName = ownerInfo.playerUsername
                            player.displayClientMessage(Component.literal("§6Entered §e$ownerName's §6claim!"), true)
                        } catch (e: Exception) {
                            player.displayClientMessage(Component.literal("§6Entered a claimed area!"), true)
                        }
                    } else if (lastOwner != null) {
                        player.displayClientMessage(Component.literal("§aEntered Wilderness!"), true)
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onItemToss(event: net.neoforged.neoforge.event.entity.item.ItemTossEvent) {
        val itemEntity = event.entity
        if (isClaimWand(itemEntity.item)) {
            event.isCanceled = true
            itemEntity.discard()
            ClaimSelection.clearSelection(event.player)
            event.player.sendSystemMessage(MessageUtil.colorHex("Claim wand destroyed.", "#FF5555"))
        }
    }
}
