package me.cyanhana.commandblockuioverhaul;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    // 保留 MODERN 的存储名称，已有配置自动使用新的 IDEA Dark 配色，无需重置文件。
    public enum ColorScheme { MODERN, VANILLA }

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.EnumValue<ColorScheme> COLOR_SCHEME = BUILDER
            .comment("Command hierarchy palette: MODERN (IDEA Dark) or VANILLA (ChatFormatting).")
            .defineEnum("colorScheme", ColorScheme.VANILLA);
    public static final ForgeConfigSpec.BooleanValue BREAK_BEFORE_OPEN = BUILDER.define("breakBeforeOpenBracket", true);
    public static final ForgeConfigSpec.BooleanValue BREAK_AFTER_OPEN = BUILDER.define("breakAfterOpenBracket", true);
    public static final ForgeConfigSpec.BooleanValue BREAK_BEFORE_CLOSE = BUILDER.define("breakBeforeCloseBracket", true);
    public static final ForgeConfigSpec.BooleanValue BREAK_AFTER_CLOSE = BUILDER.define("breakAfterCloseBracket", true);
    public static final ForgeConfigSpec.BooleanValue BREAK_AFTER_COMMA = BUILDER.define("breakAfterComma", true);
    public static final ForgeConfigSpec.BooleanValue FORMAT_STRINGS = BUILDER.define("formatStrings", false);
    public static final ForgeConfigSpec.BooleanValue AVOID_EMPTY_LINES = BUILDER.define("avoidEmptyLines", true);
    public static final ForgeConfigSpec.IntValue INDENTATION = BUILDER.defineInRange("indentation", 2, 0, 16);
    public static final ForgeConfigSpec.IntValue WRAP_WIDTH = BUILDER
            .comment("Maximum visual line width in pixels; limited by available editor width.")
            .defineInRange("wrapWidth", 300, 10, 6400);
    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static void save() {
        SPEC.save();
    }

    public static void setColorScheme(ColorScheme scheme) {
        COLOR_SCHEME.set(scheme);
        // 客户端配置由 Forge 加载；保存后重启游戏仍使用所选方案。
        SPEC.save();
    }
}
