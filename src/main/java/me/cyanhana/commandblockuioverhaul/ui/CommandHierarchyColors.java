package me.cyanhana.commandblockuioverhaul.ui;

import me.cyanhana.commandblockuioverhaul.Config;
import net.minecraft.ChatFormatting;
import java.util.ArrayDeque;
import java.util.Deque;

/** Structural highlighting in original UTF-16 coordinates, independent of visual wrapping.
 * 在原始UTF-16坐标中进行结构高亮显示，与视觉换行无关。 */
public final class CommandHierarchyColors {
    // IDEA 新界面 Dark 风格：基础文本、关键字、数字、字符串、字段、方法、标签。
    // 将语法颜色用于命令层级区分，并非复刻 IDEA 按语法类别着色的规则。
    // 深灰背景与边框参考 JetBrains 的 themes/expUI/expUI_dark.theme.json。
    // 使用 RGB 色值，不包含透明度；修改此数组即可调整整个编辑器的层级配色。
    private static final int[] PALETTE = {
            0xBCBEC4, 0xCF8E6D, 0x2AACB8, 0x6AAB73, 0xC77DBB, 0x56A8F5, 0xD5B778
    };
    private static final int[] VANILLA_PALETTE = {
            ChatFormatting.GRAY.getColor(), ChatFormatting.AQUA.getColor(),
            ChatFormatting.YELLOW.getColor(), ChatFormatting.GREEN.getColor(),
            ChatFormatting.LIGHT_PURPLE.getColor(), ChatFormatting.GOLD.getColor(),
            ChatFormatting.BLUE.getColor()
    };

    public static int color(int level) {
        int[] palette = Config.COLOR_SCHEME.get() == Config.ColorScheme.VANILLA ? VANILLA_PALETTE : PALETTE;
        // 深层只循环彩色部分，避免第 7 层重新变成基础灰色。
        return palette[level == 0 ? 0 : 1 + Math.floorMod(level - 1, palette.length - 1)];
    }

    public static boolean isIdeaDark() {
        return Config.COLOR_SCHEME.get() == Config.ColorScheme.MODERN;
    }

    // 采用不透明 ARGB 背景；建议列表不能透出下方的命令文字。
    public static int editorBackground() {
        return isIdeaDark() ? 0xFF1E1F22 : 0xFF000000;
    }

    public static int popupBackground() {
        return isIdeaDark() ? 0xFF2B2D30 : 0xFF202020;
    }

    public static int editorBorder(boolean focused) {
        return isIdeaDark() ? (focused ? 0xFFA0A0A0 : 0xFF43454A)
                : (focused ? 0xFFFFFFFF : 0xFFA0A0A0);
    }

    static int[] levels(String text, int[] baseLevels) {
        // baseLevels 来自命令解析器；此处再叠加 JSON/SNBT、选择器等括号嵌套深度。
        // 数组下标与原始字符串的 UTF-16 下标一致，显示换行不会重新计算层级。
        int[] result = new int[text.length()];
        Deque<Character> brackets = new ArrayDeque<>();
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                // 引号内的括号只是字符串内容。逐个处理反斜杠，正确区分 \" 与 \\"。
                result[i] = baseLevels[i] + brackets.size();
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == quote) quote = 0;
                continue;
            }
            if (c == '\'' || c == '"') quote = c;
            // 左括号先入栈再着色；匹配的右括号先着色再出栈，使一对括号颜色相同。
            if (c == '{' || c == '[') brackets.push(c);
            result[i] = baseLevels[i] + brackets.size();
            // 输入尚未完成或括号不匹配时不强制出栈，避免下溢和错误地结束外层。
            if (!brackets.isEmpty() && ((c == '}' && brackets.peek() == '{')
                    || (c == ']' && brackets.peek() == '['))) brackets.pop();
        }
        return result;
    }
}
