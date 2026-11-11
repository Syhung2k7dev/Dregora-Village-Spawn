package com.syhung2k7.dregoravillagespawn;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class VillageSpawnHandler {
    private static final String DONE_TAG = "DregoraVillageSpawnDone";
    private static final int SEARCH_RADIUS = 64;

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;

        if (player.world.isRemote || player.dimension != 0) {
            return;
        }

        NBTTagCompound persistentData = player.getEntityData();
        if (persistentData.getBoolean(DONE_TAG)) {
            return;
        }

        WorldServer world = (WorldServer) player.world;
        BlockPos worldSpawn = world.getSpawnPoint();

        try {
            // Uses the world's chunk generator, so custom generators can decide
            // whether they expose a structure named "Village".
            BlockPos villageCenter = world.findNearestStructure("Village", worldSpawn, false);

            if (villageCenter == null) {
                DregoraVillageSpawn.LOGGER.warn(
                    "No Village structure was returned by the world generator. " +
                    "The player will keep their current spawn; the search can retry on next login."
                );
                return;
            }

            BlockPos safeSpawn = findSafePosition(world, villageCenter, SEARCH_RADIUS);
            if (safeSpawn == null) {
                DregoraVillageSpawn.LOGGER.warn(
                    "A Village was found at {}, but no safe surface position was found nearby.",
                    villageCenter
                );
                return;
            }

            world.setSpawnPoint(safeSpawn);
            player.setSpawnPoint(safeSpawn, true);
            player.setPositionAndUpdate(
                safeSpawn.getX() + 0.5D,
                safeSpawn.getY(),
                safeSpawn.getZ() + 0.5D
            );

            persistentData.setBoolean(DONE_TAG, true);

            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                "§aDregora Village Spawn: §fYou have been placed in the nearest village."
            ));

            DregoraVillageSpawn.LOGGER.info(
                "Placed first-time player {} at village spawn {}.",
                player.getName(),
                safeSpawn
            );
        } catch (Exception exception) {
            DregoraVillageSpawn.LOGGER.error(
                "Could not place first-time player near a village. Keeping the normal spawn.",
                exception
            );
        }
    }

    private BlockPos findSafePosition(WorldServer world, BlockPos center, int radius) {
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                BlockPos found = checkColumn(world, center.getX() + dx, center.getZ() - r);
                if (found != null) return found;

                if (r > 0) {
                    found = checkColumn(world, center.getX() + dx, center.getZ() + r);
                    if (found != null) return found;
                }
            }

            for (int dz = -r + 1; dz < r; dz++) {
                BlockPos found = checkColumn(world, center.getX() - r, center.getZ() + dz);
                if (found != null) return found;

                if (r > 0) {
                    found = checkColumn(world, center.getX() + r, center.getZ() + dz);
                    if (found != null) return found;
                }
            }
        }

        return null;
    }

    private BlockPos checkColumn(WorldServer world, int x, int z) {
        BlockPos surface = world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z));

        if (surface.getY() < 1 || surface.getY() >= 255) {
            return null;
        }

        BlockPos floorPos = surface.down();
        IBlockState floor = world.getBlockState(floorPos);
        IBlockState feet = world.getBlockState(surface);
        IBlockState head = world.getBlockState(surface.up());

        if (floor.getMaterial().isLiquid() || !floor.isSideSolid(world, floorPos, EnumFacing.UP)) {
            return null;
        }

        if (!feet.getBlock().isAir(feet, world, surface) ||
            !head.getBlock().isAir(head, world, surface.up())) {
            return null;
        }

        return surface;
    }
}
