package me.cyanhana.commandblockuioverhaul;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(Commandblockuioverhaul.MODID)
public class Commandblockuioverhaul {
    public static final String MODID = "commandblockuioverhaul";

    public Commandblockuioverhaul(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }
}
