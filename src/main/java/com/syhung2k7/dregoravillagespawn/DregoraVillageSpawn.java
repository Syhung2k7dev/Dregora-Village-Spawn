package com.syhung2k7.dregoravillagespawn;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = DregoraVillageSpawn.MODID,
    name = DregoraVillageSpawn.NAME,
    version = DregoraVillageSpawn.VERSION,
    acceptedMinecraftVersions = "[1.12.2]"
)
public class DregoraVillageSpawn {
    public static final String MODID = "dregoravillagespawn";
    public static final String NAME = "Dregora Village Spawn";
    public static final String VERSION = "1.0.0";

    public static Logger LOGGER;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        MinecraftForge.EVENT_BUS.register(new VillageSpawnHandler());
        LOGGER.info("Dregora Village Spawn initialized.");
    }
}
