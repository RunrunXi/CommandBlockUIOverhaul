package me.cyanhana.commandblockuioverhaul;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    // 保留 MODERN 的存储名称，已有配置自动使用新的 IDEA Dark 配色，无需重置文件。
    public enum ColorScheme { MODERN, VANILLA }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.EnumValue<ColorScheme> COLOR_SCHEME = BUILDER
            .comment("Command hierarchy palette: MODERN (IDEA Dark) or VANILLA (ChatFormatting).")
            .defineEnum("colorScheme", ColorScheme.VANILLA);
    public static final ModConfigSpec.BooleanValue BREAK_BEFORE_OPEN = BUILDER.define("breakBeforeOpenBracket", true);
    public static final ModConfigSpec.BooleanValue BREAK_AFTER_OPEN = BUILDER.define("breakAfterOpenBracket", true);
    public static final ModConfigSpec.BooleanValue BREAK_BEFORE_CLOSE = BUILDER.define("breakBeforeCloseBracket", true);
    public static final ModConfigSpec.BooleanValue BREAK_AFTER_CLOSE = BUILDER.define("breakAfterCloseBracket", true);
    public static final ModConfigSpec.BooleanValue BREAK_AFTER_COMMA = BUILDER.define("breakAfterComma", true);
    public static final ModConfigSpec.BooleanValue FORMAT_STRINGS = BUILDER.define("formatStrings", false);
    public static final ModConfigSpec.BooleanValue AVOID_EMPTY_LINES = BUILDER.define("avoidEmptyLines", true);
    public static final ModConfigSpec.IntValue INDENTATION = BUILDER.defineInRange("indentation", 2, 0, 16);
    public static final ModConfigSpec.IntValue WRAP_WIDTH = BUILDER
            .comment("Maximum visual line width in pixels; limited by available editor width.")
            .defineInRange("wrapWidth", 300, 10, 6400);
    static final ModConfigSpec SPEC = BUILDER.build();

    public static void save() {
        SPEC.save();
    }

    public static void setColorScheme(ColorScheme scheme) {
        COLOR_SCHEME.set(scheme);
        // 客户端配置由 NeoForge 加载；保存后重启游戏仍使用所选方案。
        SPEC.save();
    }
}
