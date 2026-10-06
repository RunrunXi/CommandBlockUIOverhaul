package me.cyanhana.commandblockuioverhaul.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** 只生成显示行，不插入换行或删除原始命令中的空格，保证光标与补全索引不变。 */
final class CommandTextLayout {
    record Options(boolean beforeOpen, boolean afterOpen, boolean beforeClose, boolean afterClose,
                   boolean afterComma, boolean formatStrings, boolean avoidEmpty, int indentation, int wrapWidth) {}
    record Row(String text, int indent) {}

    static List<Row> layout(String text, Options options, int availableWidth, ToIntFunction<String> measure) {
        List<Row> rows = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        int depth = 0;
        int indent = 0;
        char quote = 0;
        boolean escaped = false;
        boolean pendingBreak = false;
        int width = Math.max(1, Math.min(options.wrapWidth(), availableWidth));
        int spaceWidth = measure.applyAsInt(" ");

        for (int index = 0; index < text.length();) {
            int cp = text.codePointAt(index);
            index += Character.charCount(cp);
            // 括号后的空格留在括号所在行，直到下一个非空格字符才执行待处理断行。
            boolean afterSpaces = pendingBreak && cp != ' ';
            if (afterSpaces) {
                emit(rows, line, indent, options.avoidEmpty());
                indent = depth;
                pendingBreak = false;
            }
            boolean structural = options.formatStrings() || quote == 0;
            boolean open = structural && (cp == '{' || cp == '[');
            boolean close = structural && (cp == '}' || cp == ']');
            boolean before = (open && options.beforeOpen()) || (close && options.beforeClose());
            // 同一个位置既有“上一括号后”又有“当前括号前”断行时，可选择合并空行。
            if (before) emit(rows, line, indent, options.avoidEmpty());
            if (close) depth = Math.max(0, depth - 1);
            if (line.isEmpty()) indent = depth;
            line.appendCodePoint(cp);
            if (open) ++depth;
            if ((open && options.afterOpen()) || (close && options.afterClose())
                    || (structural && cp == ',' && options.afterComma())) pendingBreak = true;

            // 奇偶反斜杠逐个处理，转义引号不结束字符串；软换行仍可拆分长字符串。
            if (quote != 0) {
                if (escaped) escaped = false;
                else if (cp == '\\') escaped = true;
                else if (cp == quote) quote = 0;
            } else if (cp == '\'' || cp == '"') quote = (char) cp;

            // 空格跟随待断行括号时不单独触发宽度断行。
            if (!(pendingBreak && cp == ' ')) {
                int limit = Math.max(1, width - indent * options.indentation() * spaceWidth);
                while (line.codePointCount(0, line.length()) > 1 && measure.applyAsInt(line.toString()) > limit) {
                    int split = line.offsetByCodePoints(line.length(), -1);
                    // 优先在单词之间断行，空格保留在上一行；不拆开 Unicode 代理对。
                    for (int i = split - 1; i >= 0; --i) {
                        if (line.charAt(i) == ' ') { split = i + 1; break; }
                    }
                    if (options.avoidEmpty() && line.substring(0, split).isBlank()) {
                        if (rows.isEmpty()) break; // 保留前导空格，不单独生成空白行。
                        Row previous = rows.remove(rows.size() - 1);
                        rows.add(new Row(previous.text() + line.substring(0, split), previous.indent()));
                        line.delete(0, split);
                        continue;
                    }
                    rows.add(new Row(line.substring(0, split), indent));
                    line.delete(0, split);
                }
            }
        }
        if (!line.isEmpty()) emit(rows, line, indent, options.avoidEmpty());
        if (rows.isEmpty()) rows.add(new Row("", 0));
        return rows;
    }

    private static void emit(List<Row> rows, StringBuilder line, int indent, boolean avoidEmpty) {
        // 不生成只含空格的显示行，但把这些空格保留在上一行，原始索引不变。
        if (avoidEmpty && !line.isEmpty() && line.toString().isBlank() && !rows.isEmpty()) {
            Row previous = rows.remove(rows.size() - 1);
            rows.add(new Row(previous.text() + line, previous.indent()));
            line.setLength(0);
            return;
        }
        if (!line.isEmpty() || !avoidEmpty) rows.add(new Row(line.toString(), indent));
        line.setLength(0);
    }
}
