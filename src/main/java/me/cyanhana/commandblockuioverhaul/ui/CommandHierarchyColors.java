package me.cyanhana.commandblockuioverhaul.ui;

import java.util.ArrayDeque;
import java.util.Deque;

/** Structural highlighting in original UTF-16 coordinates, independent of visual wrapping.
 * 在原始UTF-16坐标中进行结构高亮显示，与视觉换行无关。 */
final class CommandHierarchyColors {
    // 第 0 层使用浅灰；更深层依次使用青蓝、柔黄、薄荷绿、淡紫、珊瑚橙、淡蓝。
    // 使用 RGB 色值，不包含透明度；修改此数组即可调整整个编辑器的层级配色。
    private static final int[] PALETTE = {
            0xCDD6E4, 0x89DCEB, 0xF9E2AF, 0xA6E3A1, 0xCBA6F7, 0xFAB387, 0x89B4FA
    };

    static int color(int level) {
        // 深层只循环彩色部分，避免第 7 层重新变成基础灰色。
        return PALETTE[level == 0 ? 0 : 1 + Math.floorMod(level - 1, PALETTE.length - 1)];
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
