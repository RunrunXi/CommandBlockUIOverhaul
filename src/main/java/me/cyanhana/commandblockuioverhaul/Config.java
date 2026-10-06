package me.cyanhana.commandblockuioverhaul;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    // 保留 MODERN 的存储名称，已有配置自动使用新的 IDEA Dark 配色，无需重置文件。
    public enum ColorScheme { MODERN, VANILLA }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.EnumValue<ColorScheme> COLOR_SCHEME = BUILDER
            .comment("Command hierarchy palette: MODERN (IDEA Dark) or VANILLA (ChatFormatting).")
            .defineEnum("colorScheme", ColorScheme.MODERN);
    static final ModConfigSpec SPEC = BUILDER.build();

    public static void setColorScheme(ColorScheme scheme) {
        COLOR_SCHEME.set(scheme);
        // 客户端配置由 NeoForge 加载；保存后重启游戏仍使用所选方案。
        SPEC.save();
    }
}
