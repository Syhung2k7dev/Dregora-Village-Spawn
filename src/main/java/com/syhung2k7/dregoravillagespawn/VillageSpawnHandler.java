package com.syhung2k7.dregoravillagespawn;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class VillageSpawnHandler {
    private static final String DONE_TAG = "DregoraVillageSpawnDone";
    private static final int SAFE_SEARCH_RADIUS = 64;
    private static final int BIOME_SEARCH_RADIUS = 500;
    private static final int CLOSEST_BIOME_LIMIT = 128;
    private static final int BIOME_SAMPLE_STEP = 4;

    private static final int DESERT_INDEX = 0;
    private static final int OCEAN_INDEX = 1;
    private static final int ICE_PLAINS_INDEX = 2;

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
            // The generator locates a Village structure. Biome distances are
            // measured from that village, not from the world's original spawn.
            BlockPos villageCenter = world.findNearestStructure("Village", worldSpawn, false);

            if (villageCenter == null) {
                DregoraVillageSpawn.LOGGER.warn(
                    "No Village structure was returned by the world generator. " +
                    "The player will keep their current spawn; the search can retry on next login."
                );
                return;
            }

            if (!hasValidBiomeProximity(world, villageCenter)) {
                DregoraVillageSpawn.LOGGER.warn(
                    "Village at {} does not meet the Desert/Ocean/Ice Plains proximity rules. " +
                    "The player will keep their current spawn.",
                    villageCenter
                );
                return;
            }

            BlockPos safeSpawn = findSafePosition(world, villageCenter, SAFE_SEARCH_RADIUS);
            if (safeSpawn == null) {
                DregoraVillageSpawn.LOGGER.warn(
                    "A qualifying Village was found at {}, but no safe surface position was found nearby.",
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
                "§aDregora Village Spawn: §fYou have been placed in a village near the target biomes."
            ));

            DregoraVillageSpawn.LOGGER.info(
                "Placed first-time player {} at qualifying village spawn {}.",
                player.getName(),
                safeSpawn
            );
        } catch (Exception exception) {
            DregoraVillageSpawn.LOGGER.error(
                "Could not place first-time player near a qualifying village. Keeping the normal spawn.",
                exception
            );
        }
    }

    /**
     * Checks the biome distances from the village center:
     * - If all three target biome types are within 500 blocks, the nearest
     *   must be within 128 blocks.
     * - The same rule naturally applies when only two or one target types
     *   exist within 500 blocks: at least the nearest must be within 128.
     * All biome measurements are horizontal (X/Z) distances from the village.
     */
    private boolean hasValidBiomeProximity(WorldServer world, BlockPos villageCenter) {
        long[] nearestSquaredDistances = {
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE
        };

        int radiusSquared = BIOME_SEARCH_RADIUS * BIOME_SEARCH_RADIUS;
        int centerX = villageCenter.getX();
        int centerZ = villageCenter.getZ();

        // Sample at 4-block intervals to keep login-time biome checks bounded.
        // The biome provider is queried directly, without deliberately loading chunks.
        for (int dx = -BIOME_SEARCH_RADIUS; dx <= BIOME_SEARCH_RADIUS; dx += BIOME_SAMPLE_STEP) {
            for (int dz = -BIOME_SEARCH_RADIUS; dz <= BIOME_SEARCH_RADIUS; dz += BIOME_SAMPLE_STEP) {
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared > radiusSquared) {
                    continue;
                }

                Biome biome = world.getBiomeProvider().getBiome(
                    new BlockPos(centerX + dx, 0, centerZ + dz)
                );
                int targetIndex = getTargetBiomeIndex(biome);

                if (targetIndex >= 0 && distanceSquared < nearestSquaredDistances[targetIndex]) {
                    nearestSquaredDistances[targetIndex] = distanceSquared;
                }
            }
        }

        int foundBiomeTypes = 0;
        long closestSquaredDistance = Long.MAX_VALUE;
        StringBuilder distanceSummary = new StringBuilder();

        String[] biomeNames = {"Desert", "Ocean", "Ice Plains"};
        for (int i = 0; i < nearestSquaredDistances.length; i++) {
            long distanceSquared = nearestSquaredDistances[i];
            if (distanceSquared != Long.MAX_VALUE) {
                foundBiomeTypes++;
                if (distanceSquared < closestSquaredDistance) {
                    closestSquaredDistance = distanceSquared;
                }
                if (distanceSummary.length() > 0) {
                    distanceSummary.append(", ");
                }
                distanceSummary.append(biomeNames[i])
                    .append("=")
                    .append((int) Math.round(Math.sqrt(distanceSquared)))
                    .append(" blocks");
            }
        }

        if (foundBiomeTypes == 0) {
            DregoraVillageSpawn.LOGGER.info(
                "No target biome found within {} blocks of village {}.",
                BIOME_SEARCH_RADIUS,
                villageCenter
            );
            return false;
        }

        int closestDistance = (int) Math.round(Math.sqrt(closestSquaredDistance));
        boolean valid = closestSquaredDistance <= (long) CLOSEST_BIOME_LIMIT * CLOSEST_BIOME_LIMIT;

        DregoraVillageSpawn.LOGGER.info(
            "Biome proximity for village {}: {}. Found {} of 3 target biome types; " +
            "nearest is {} blocks (required: <= {}).",
            villageCenter,
            distanceSummary.toString(),
            foundBiomeTypes,
            closestDistance,
            CLOSEST_BIOME_LIMIT
        );

        // Any additional target biome types found are necessarily within 500 blocks
        // because that is the boundary of the scan.
        return valid;
    }

    private int getTargetBiomeIndex(Biome biome) {
        if (biome == Biomes.DESERT || biome == Biomes.DESERT_HILLS) {
            return DESERT_INDEX;
        }

        if (biome == Biomes.ICE_PLAINS) {
            return ICE_PLAINS_INDEX;
        }

        if (biome == Biomes.OCEAN ||
            biome == Biomes.DEEP_OCEAN ||
            biome == Biomes.FROZEN_OCEAN ||
            BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)) {
            return OCEAN_INDEX;
        }

        return -1;
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
