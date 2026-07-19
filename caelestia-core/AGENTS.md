# Caelestia Protection - OpenCode Agent Instructions

This repository contains a NeoForge 1.21.1 Kotlin mod that serves as a custom claiming addon/wrapper around **Open Parties and Claims (OPAC)**.

## Commands & Build Workflow
- **Build**: Always run `sh gradlew clean build`. 
  - *Gotcha*: If you delete a Kotlin/Java file without running `clean`, NeoForge may still load the orphaned `.class` file from the `build/` directory, leading to dangling entrypoint crashes (e.g., `fml.modloadingissue.javafml.dangling_entrypoint`).
- **Artifact**: The compiled mod is located at `build/libs/caelestiaprotection-1.0.0.jar`.

## Architecture & OPAC API Gotchas
This mod disables native OPAC claim commands and implements a WorldEdit-style Claim Wand (`/caelestiaclaim`). The OPAC decompiled source is kept in `/open-parties-and-claims/` strictly for API reference.

- **Claim Ownership (Critical)**: Modpacks like BMC5 enforce "Party Owned Claims" in OPAC. When claiming land programmatically, **do not hardcode `player.uuid` as the claim owner**. If you do, OPAC will treat the player as a trespasser in their own claim (preventing block breaking). You must resolve the Party UUID:
  ```kotlin
  val partyAPI = OpenPACServerAPI.get(server).partyManager.getPartyByMember(player.uuid)
  val ownerId = partyAPI?.id ?: player.uuid
  ```
- **Action Bar Messages**: OPAC's native action bar entrance/exit messages are often disabled by modpack configurations. We manage custom action bar messages manually inside `ServerTickEvent` using `claimsManager.get(dimension, x, z)`.
- **Flood Fill Unclaiming**: `/unclaim all` uses a custom BFS flood-fill algorithm up to 500 chunks to unclaim contiguous land, since OPAC natively only provides absolute-coordinate area unclaiming (`tryToUnclaimArea`).

## Modding Quirks (NeoForge 1.21.1)
- **Item Syncing**: When giving the claim wand to a player via a command, use `net.neoforged.neoforge.items.ItemHandlerHelper.giveItemToPlayer(player, wand)`. Standard `player.inventory.add()` does not reliably sync custom DataComponents to the client, causing "ghost item" desyncs where the server cancels block-break events but the client thinks the hand is empty.
- **Removing Items**: When consuming the wand upon a successful claim, use `player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)` instead of `shrink(1)` to ensure immediate client-side visual removal.
- **Event Interception**: The wand intercepts `PlayerInteractEvent.LeftClickBlock` and `RightClickBlock`. Always call `event.isCanceled = true` when setting positions to prevent native block breaking or placement.
- **Overlapping Claims**: OPAC's `tryToClaimArea` skips over existing claims and claims the free chunks around them. If you want to prevent players from selecting areas that overlap another player's claim, you must manually loop through the chunks and verify `claimsAPI.get(dimension, x, z)` is null (or owned by the player) *before* executing the claim.
- **Friend Whitelists / Alliances**: Because of `PartyOwnedClaims`, if you use `addMember()` to add Player B to Player A's party, Player B loses the ability to have their own separate claims. To create a true many-to-many "whitelist" system, ensure both players have their own individual parties and use `executorParty.addAllyParty(targetParty.id)`. This natively grants them access to each other's claims without restricting their independence.
