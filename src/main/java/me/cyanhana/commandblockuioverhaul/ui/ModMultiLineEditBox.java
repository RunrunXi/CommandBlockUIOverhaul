package me.cyanhana.commandblockuioverhaul.ui;

import me.cyanhana.commandblockuioverhaul.mixin.EditBoxAccessor;
import me.cyanhana.commandblockuioverhaul.Config;
import net.minecraft.util.StringUtil;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ModMultiLineEditBox extends EditBox {
    private final EditBoxAccessor accessor = (EditBoxAccessor) this;
    private ModCommandSuggestions suggestions;
    private boolean hasSuggestions = false;
    // 主要属性
    private String value = "";
    private final Font font;
    private Consumer<String> responder;
    private int highlightPos;
    private int displayPos;
    private boolean bordered = true;
    private Predicate<String> filter = Objects::nonNull;
    private int cursorPos;
    private String suggestion;
    private BiFunction<String, Integer, FormattedCharSequence> formatter = (command, maxLength) -> {
        return FormattedCharSequence.forward(command, Style.EMPTY);
    };
    // 多行文本管理
    private final List<String> lines = new ArrayList<>();
    private final List<Integer> indentLevels = new ArrayList<>();
    private final List<FormattedCharSequence> formattedLines = new ArrayList<>();
    private int visibleLines = 10;     // 可视行数
    private int scrolledLines = 0;     // 已滚动行数
    private int maxLength = 32;
    private static final int HISTORY_LIMIT = 100;
    private final Deque<EditState> undoHistory = new ArrayDeque<>();
    private final Deque<EditState> redoHistory = new ArrayDeque<>();
    private record EditState(String text, int cursor, int anchor) {}
    private boolean shiftPressed;
    // 设置
    private int indentation = Config.INDENTATION.get(); // 当前布局使用的缩进空格数
    // 光标
    private int cursorLine = 0;              // 光标所在行
    private int cursorIndexInLine = 0;       // 光标在行的位置
    private int cursorX = 0;            // 高亮位置
    private int cursorY = 0;
    // 布局计算
    private final int lineHeight;       // 行高（根据字体调整）

    public ModMultiLineEditBox(Font pFont, int pX, int pY, int pWidth, int pHeight, Component pMessage) {
        super(pFont, pX, pY, pWidth, pHeight, pMessage);
        font = pFont;
        // 计算可视行数
        lineHeight = pFont.lineHeight + 2; // 字体高度 + 间距
        visibleLines = Math.max(1, (pHeight - 8) / lineHeight);
        formatText(this.value);
        // 让命令提示框初始不显示(在屏幕外面就不显示了)
        cursorX = width * 2;
    }

    public void setCommandSuggestions(ModCommandSuggestions suggestions) {
        this.suggestions = suggestions;
        this.hasSuggestions = true;
    }

    private void updateSuggestions() {
        if (hasSuggestions) {
            suggestions.updateCommandInfo();
        }
    }

    @Override
    public void setResponder(@NotNull Consumer<String> pResponder) {
        this.responder = pResponder;
    }

    @Override
    public @NotNull String getValue() {
        return this.value;
    }

    @Override
    protected @NotNull MutableComponent createNarrationMessage() {
        Component component = this.getMessage();
        return Component.translatable("gui.narrate.editBox", component, this.value);
    }

    @Override
    public @NotNull String getHighlighted() {
        int i = Math.min(this.cursorPos, this.highlightPos);
        int j = Math.max(this.cursorPos, this.highlightPos);
        return this.value.substring(i, j);
    }

    @Override
    public void setBordered(boolean pEnableBackgroundDrawing) {
        this.bordered = pEnableBackgroundDrawing;
    }

    public boolean isBordered() {
        return this.bordered;
    }

    @Override
    public void setFilter(@NotNull Predicate<String> pValidator) {
        this.filter = pValidator;
    }

    @Override
    public int getCursorPosition() {
        return this.cursorPos;
    }

    @Override
    public void setSuggestion(@Nullable String pSuggestion) {
        this.suggestion = pSuggestion;
    }

    @Override
    public void setFormatter(@NotNull BiFunction<String, Integer, FormattedCharSequence> pTextFormatter) {
        this.formatter = pTextFormatter;
        formatColoredText();
    }

    public int getCursorX() {
        return cursorX;
    }

    public int getCursorY() {
        return cursorY;
    }

    /** 光标是否位于实际绘制的行范围内；手动滚动不应让建议悬浮在无关文本上。 */
    public boolean isCursorVisible() {
        return this.isVisible() && cursorLine >= scrolledLines
                && cursorLine < scrolledLines + Math.max(1, visibleLines);
    }

    public int getScrolledLines() {
        return scrolledLines;
    }

    public int getMaxScrolledLines() {
        return Math.max(0, lines.size() - visibleLines);
    }

    public int getVisibleLineCount() {
        return visibleLines;
    }

    public void scrollToLine(int line) {
        scrolledLines = Mth.clamp(line, 0, getMaxScrolledLines());
    }

    public int getSelectionAnchor() {
        return this.highlightPos;
    }

    public void resetSelectionModifier() {
        // 跨界面切换后不沿用编辑器上一次按键留下的 Shift 选择状态。
        this.shiftPressed = false;
    }

    @Override
    public void setValue(@NotNull String text) {
        String accepted = truncateText(text, this.maxLength);
        if (!this.filter.test(accepted)) return;
        undoHistory.clear();
        redoHistory.clear();
        restoreEdit(new EditState(accepted, accepted.length(), accepted.length()));
    }

    private static String truncateText(String text, int limit) {
        int end = Math.min(text.length(), Math.max(0, limit));
        if (end > 0 && end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))
                && Character.isLowSurrogate(text.charAt(end))) {
            --end;
        }
        return text.substring(0, end);
    }

    private EditState captureEdit() {
        return new EditState(this.value, this.cursorPos, this.highlightPos);
    }

    private void restoreEdit(EditState state) {
        this.value = state.text();
        formatText(this.value);
        this.setCursorPosition(state.cursor());
        this.setHighlightPos(state.anchor());
        // 更新实际插入点之后才通知补全器，避免中间状态触发错误的建议。
        this.onValueChange(this.value);
        formatColoredText();
    }

    public boolean applyUserEdit(String text, int cursor) {
        String accepted = truncateText(text, this.maxLength);
        if (!this.filter.test(accepted)) return false;
        if (!accepted.equals(this.value)) {
            if (undoHistory.size() == HISTORY_LIMIT) undoHistory.removeFirst();
            undoHistory.addLast(captureEdit());
            redoHistory.clear();
        }
        restoreEdit(new EditState(accepted, cursor, cursor));
        return true;
    }

    public void undo() {
        if (undoHistory.isEmpty()) return;
        redoHistory.addLast(captureEdit());
        restoreEdit(undoHistory.removeLast());
    }

    public void redo() {
        if (redoHistory.isEmpty()) return;
        undoHistory.addLast(captureEdit());
        restoreEdit(redoHistory.removeLast());
    }

    public void copyStateFrom(ModMultiLineEditBox previous) {
        restoreEdit(previous.captureEdit());
        undoHistory.clear();
        undoHistory.addAll(previous.undoHistory);
        redoHistory.clear();
        redoHistory.addAll(previous.redoHistory);
    }

    @Override
    public void insertText(@NotNull String pTextToWrite) {
        int i = Math.min(this.cursorPos, this.highlightPos);
        int j = Math.max(this.cursorPos, this.highlightPos);
        int k = this.maxLength - this.getValue().length() - (i - j);
        String s = truncateText(StringUtil.filterText(pTextToWrite), k);
        int l = s.length();

        String s1 = (new StringBuilder(this.getValue())).replace(i, j, s).toString();
        this.applyUserEdit(s1, i + l);
    }

    @Override
    public void setMaxLength(int pLength) {
        if (pLength < 0) throw new IllegalArgumentException("Negative maximum length");
        this.maxLength = pLength;
        undoHistory.clear();
        redoHistory.clear();
        if (this.value.length() > pLength) {
            String accepted = truncateText(this.value, pLength);
            restoreEdit(new EditState(accepted, cursorPos, highlightPos));
        }

    }

    private void onValueChange(String pNewText) {
        if (this.responder != null) {
            this.responder.accept(pNewText);
        }

    }

    private void deleteText(int pCount) {
        if (Screen.hasControlDown()) {
            this.deleteWords(pCount);
        } else {
            this.deleteChars(pCount);
        }
    }

    @Override
    public void deleteWords(int pNum) {
        if (!this.value.isEmpty()) {
            if (this.highlightPos != this.cursorPos) {
                this.insertText("");
            } else {
                this.deleteTo(this.getWordPosition(pNum));
            }
        }
    }

    @Override
    public void deleteChars(int pNum) {
        this.deleteTo(this.getCursorPos(pNum));
    }

    private void deleteTo(int position) {
        if (!this.getValue().isEmpty()) {
            if (this.highlightPos != this.cursorPos) {
                this.insertText("");
            } else {
                int i = position;
                int j = Math.min(i, this.cursorPos);
                int k = Math.max(i, this.cursorPos);
                if (j != k) {
                    String s = (new StringBuilder(this.getValue())).delete(j, k).toString();
                    this.applyUserEdit(s, j);
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (!this.canConsumeInput()) {
            return false;
        } else {
            this.shiftPressed = Screen.hasShiftDown();
            if (Screen.hasControlDown() && pKeyCode == 90) {
                if (Screen.hasShiftDown()) this.redo(); else this.undo();
                return true;
            }
            if (Screen.hasControlDown() && pKeyCode == 89) {
                this.redo();
                return true;
            }
            if (Screen.isSelectAll(pKeyCode)) {
//                System.out.println("Select all");
                this.moveCursorToEnd();
                this.setHighlightPos(0);
                return true;
            } else if (Screen.isCopy(pKeyCode)) {
//                System.out.println("Copy");
                Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
                return true;
            } else if (Screen.isPaste(pKeyCode)) {
//                System.out.println("Paste");
                if (accessor.getIsEditable()) {
                    this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
                }
                return true;
            } else if (Screen.isCut(pKeyCode)) {
//                System.out.println("Cut");
                Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
                if (accessor.getIsEditable()) {
                    this.insertText("");
                }
                return true;
            } else {
                switch (pKeyCode) {
                    case 259:
                        // 退格
//                        System.out.println("259");
                        if (accessor.getIsEditable()) {
                            this.shiftPressed = false;
                            this.deleteText(-1);
                            this.shiftPressed = Screen.hasShiftDown();
                        }
                        return true;
                    case 260:
                        // Insert
//                        System.out.println("260");
                    case 266:
                        // Page Up
//                        System.out.println("266");
                    case 267:
                        // Page Down
//                        System.out.println("267");
                    default:
//                        System.out.println("default false");
                        return false;
                    case 261:
                        // Delete
//                        System.out.println("261");
                        if (accessor.getIsEditable()) {
                            this.shiftPressed = false;
                            this.deleteText(1);
                            this.shiftPressed = Screen.hasShiftDown();
                        }

                        return true;
                    case 262:
                        // 右
//                        System.out.println("262");
                        if (Screen.hasControlDown()) {
//                            System.out.println("to pos " + this.getWordPosition(1));
                            this.moveCursorTo(this.getWordPosition(1));
                        } else {
                            this.moveCursor(1);
                        }
                        return true;
                    case 263:
                        // 左
//                        System.out.println("263");
                        if (Screen.hasControlDown()) {
//                            System.out.println("to pos " + this.getWordPosition(-1));
                            this.moveCursorTo(this.getWordPosition(-1));
                        } else {
                            this.moveCursor(-1);
                        }
                        return true;
                    case 264:
                        // 下
//                        System.out.println("264");
                        moveCursorVertical(-1);
                        return true;
                    case 265:
                        // 上
//                        System.out.println("265");
                        moveCursorVertical(1);
                        return true;
                    case 268:
                        // Home
//                        System.out.println("268");
                        this.moveCursorToStart();
                        return true;
                    case 269:
                        // End
//                        System.out.println("269");
                        this.moveCursorToEnd();
                        return true;
                }
            }
        }
    }

    // 换行计算
    private void formatText(String text) {
        lines.clear();
        indentLevels.clear();
        formattedLines.clear();
        CommandTextLayout.Options options = new CommandTextLayout.Options(
                Config.BREAK_BEFORE_OPEN.get(), Config.BREAK_AFTER_OPEN.get(),
                Config.BREAK_BEFORE_CLOSE.get(), Config.BREAK_AFTER_CLOSE.get(),
                Config.BREAK_AFTER_COMMA.get(), Config.FORMAT_STRINGS.get(),
                Config.AVOID_EMPTY_LINES.get(), indentation, Config.WRAP_WIDTH.get());
        for (CommandTextLayout.Row row : CommandTextLayout.layout(text, options, getInnerWidth(), font::width)) {
            lines.add(row.text());
            indentLevels.add(row.indent());
        }
    }

    /** 配置预览即时重排：不调用 setValue，避免清空用户编辑历史与选区。 */
    public void refreshFormatting() {
        indentation = Config.INDENTATION.get();
        formatText(this.value);
        setCursorPosition(this.cursorPos);
        setHighlightPos(this.highlightPos);
        onValueChange(this.value);
        formatColoredText();
    }

    private void formatColoredText() {
        formattedLines.clear();
        int charCount = 0;
        // 在处理每一行时，同时生成格式化版本
        for (String line : lines) {
            // 使用格式化器处理这一行
            FormattedCharSequence formattedLine;
            if (formatter != null) {
                // maxLength参数：通常是行的显示宽度
                formattedLine = formatter.apply(line, charCount);
            } else {
                // 默认格式化（无颜色）
                formattedLine = FormattedCharSequence.forward(line, Style.EMPTY);
            }

            formattedLines.add(formattedLine);
            charCount += line.length();
        }
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.isVisible()) {
            return;
        }

        // 背景与边框随配色方案切换，Dark 使用 IDEA 的深灰编辑区和蓝色焦点边框。
        if (this.isBordered()) {
            int borderColor = CommandHierarchyColors.editorBorder(this.isFocused());
            guiGraphics.fill(this.getX() - 1, this.getY() - 1,
                    this.getX() + this.width + 1, this.getY() + this.height + 1,
                    borderColor);
            guiGraphics.fill(this.getX(), this.getY(),
                    this.getX() + this.width, this.getY() + this.height,
                    CommandHierarchyColors.editorBackground());
        }

        // 裁剪深层缩进、预输入文字和选择高亮，避免绘制到输入框外。
        guiGraphics.enableScissor(this.getX() + 1, this.getY() + 1,
                this.getX() + this.width - 1, this.getY() + this.height - 1);
        // 颜色与文字坐标
        int textColor = accessor.getIsEditable() ? accessor.getTextColor() : accessor.getTextColorUneditable();
        int baseX = this.isBordered() ? this.getX() + 4 : this.getX();
        int baseY = this.isBordered() ? this.getY() + 4 : this.getY();
        int highlightPos = this.highlightPos;

        // 计算渲染区域
        int startLine = scrolledLines;
        int endLine = Math.min(scrolledLines + visibleLines, lines.size());
        int currentX; // 左边距
        int currentY = baseY; // 顶部边距

        // 高亮渲染
        int[] cursorXY = getGlobalPosXY(this.cursorPos);
        int[] highlightXY = getGlobalPosXY(highlightPos);
        int startHighlightLine = Math.min(cursorXY[0], highlightXY[0]);
        int endHighlightLine = Math.max(cursorXY[0], highlightXY[0]);

        // 渲染可视范围内的每一行
        for (int i = startLine; i < endLine; i++) {
            String line = lines.get(i);
            // x 初始位置
            currentX = baseX;
            // 当前行已输入的字符宽度
            int lineWidth = this.font.width(line);
            // 根据缩进级别添加缩进空格
            int indent = indentLevels.get(i);
            int indentWidth = indent * indentation * this.font.width(" ");
            if (indent > 0) {
                currentX = indentWidth + currentX;
            }
            boolean isCursorBetweenLine = this.cursorPos < this.getValue().length() || this.getValue().length() >= this.maxLength;
            // 绘制行
            renderColoredLine(guiGraphics, line, currentX, currentY, textColor, i);
//            guiGraphics.drawString(this.font, line, currentX, currentY, textColor);
            // 绘制命令建议
            if (!isCursorBetweenLine && this.suggestion != null && i == cursorLine) {
                guiGraphics.drawString(this.font, this.suggestion, currentX + lineWidth, currentY, -8355712);
            }
            // 绘制光标
            // 如果聚焦且光标可见
            if (this.isFocused() && (Util.getMillis() - accessor.getFocusedTime()) / 300L % 2L == 0L) {
                // 如果光标不在末尾或已达最大长度(需要显示为竖线的情况)
                if (isCursorBetweenLine) {
                    if (i == cursorLine) {
                        int offsetX = this.font.width(line.substring(0, Math.min(cursorIndexInLine, line.length())));
                        guiGraphics.fill(RenderType.guiOverlay(), currentX + offsetX, currentY - 1, currentX + 1 + offsetX, currentY + 1 + 9, -3092272);
                    }
                } else if (i == lines.size() - 1) {
                    guiGraphics.drawString(this.font, "_", currentX + lineWidth, currentY, textColor);
                }
            }
            // 绘制高亮
            if (highlightPos != this.cursorPos && i >= startHighlightLine && i <= endHighlightLine) {
                // 计算渲染位置
                boolean isForwardSelection = cursorXY[0] > highlightXY[0]; // 正向选择
                int lineStartX = baseX + indentWidth;
                int hCursorX = lineStartX + this.font.width(line.substring(0, Math.min(cursorXY[1], line.length())));
                int hHighlightX = lineStartX + this.font.width(line.substring(0, Math.min(highlightXY[1], line.length())));
                // 根据情况渲染
                if (i == startHighlightLine && i == endHighlightLine) {
                    // 只有一行时
                    int startX = cursorXY[1] > highlightXY[1] ? hHighlightX : hCursorX;
                    int endX = cursorXY[1] > highlightXY[1] ? hCursorX : hHighlightX;
                    renderHighlight(guiGraphics, startX, currentY - 1, endX, currentY + 1 + 9);
                } else if (i == startHighlightLine) {
                    // 起始行：从选择起点到行尾
                    int startX = isForwardSelection ? hHighlightX : hCursorX;
                    renderHighlight(guiGraphics, startX, currentY - 1, baseX + this.width - 6, currentY + 1 + 9);
                }
                else if (i == endHighlightLine) {
                    // 结束行：从行首到选择终点
                    int endX = isForwardSelection ? hCursorX : hHighlightX;
                    renderHighlight(guiGraphics, baseX - 2, currentY - 1, endX, currentY + 1 + 9);
                } else {
                    // 渲染中间完整行
                    renderHighlight(guiGraphics, baseX - 2, currentY - 1, baseX + this.width - 6, currentY + 1 + 9);
                }
            }

            // 计算下一行的坐标
            currentY += lineHeight;
        }
        guiGraphics.disableScissor();
    }

    private void renderColoredLine(GuiGraphics guiGraphics, String text, int x, int y, int color, int lineIndex) {
        // 检查是否有格式化版本
        if (lineIndex >= 0 && lineIndex < formattedLines.size()) {
            FormattedCharSequence formattedText = formattedLines.get(lineIndex);
            // 使用 drawString 的 FormattedCharSequence 版本
            guiGraphics.drawString(this.font, formattedText, x, y, color);
        } else {
            // 回退：使用普通文本渲染
            guiGraphics.drawString(this.font, text, x, y, color);
        }
    }

    private void renderHighlight(GuiGraphics guiGraphics, int minX, int minY, int maxX, int maxY) {
        if (minX < maxX) {
            int i = minX;
            minX = maxX;
            maxX = i;
        }

        if (minY < maxY) {
            int j = minY;
            minY = maxY;
            maxY = j;
        }

        if (maxX > this.getX() + this.width) {
            maxX = this.getX() + this.width;
        }

        if (minX > this.getX() + this.width) {
            minX = this.getX() + this.width;
        }

        guiGraphics.fill(RenderType.guiTextHighlight(), minX, minY, maxX, maxY, -16776961);
    }

    // 鼠标点击事件
    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!this.isVisible() || !this.isMouseOver(mouseX, mouseY)) {
            return;
        }
        this.moveCursorTo(this.positionAt(mouseX, mouseY), Screen.hasShiftDown());
    }

    private int positionAt(double mouseX, double mouseY) {
        int padding = this.isBordered() ? 4 : 0;
        int clickedLine = Mth.clamp(scrolledLines
                + Mth.floor((mouseY - this.getY() - padding) / lineHeight), 0, lines.size() - 1);
        String line = lines.get(clickedLine);
        double relativeX = mouseX - this.getX() - padding
                - indentLevels.get(clickedLine) * indentation * this.font.width(" ");
        int charIndex = 0;
        int previousWidth = 0;
        while (charIndex < line.length()) {
            int next = line.offsetByCodePoints(charIndex, 1);
            int nextWidth = this.font.width(line.substring(0, next));
            if (relativeX < (previousWidth + nextWidth) / 2.0) break;
            previousWidth = nextWidth;
            charIndex = next;
        }
        return getGlobalIndexForLine(clickedLine, charIndex);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button != 0 || !this.canConsumeInput()) return false;
        if (mouseY < this.getY()) scrolledLines = Math.max(0, scrolledLines - 1);
        if (mouseY >= this.getY() + this.height) {
            scrolledLines = Math.min(Math.max(0, lines.size() - visibleLines), scrolledLines + 1);
        }
        this.moveCursorTo(this.positionAt(mouseX, mouseY), true);
        return true;
    }
    // 计算全局光标位置（需要知道每行的起始索引）
    private int getGlobalIndexForLine(int lineIndex, int charInLine) {
        // 简单实现：遍历前面的行累加长度
        int index = 0;
        for (int i = 0; i < lineIndex; i++) {
            index += lines.get(i).length();
        }
        return index + Math.min(charInLine, lines.get(lineIndex).length());
    }

    private int[] getGlobalPosXY(int globalPos) {
        int[] returnPos = new int[2];
        int lineIndex = 0;
        int charCount = 0;
        // 计算光标位置在哪一行
        for (String line : lines) {
            if (charCount + line.length() >= globalPos) {
                returnPos[1] = globalPos - charCount;
                break;
            } else {
                charCount += line.length();
                lineIndex++;
            }
        }
        returnPos[0] = lineIndex;

        return returnPos;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalDelta, double delta) {
        if (!this.isVisible() || !this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        int scrollAmount = (int) Math.signum(delta) * 3; // 每次滚动3行
        scrolledLines = Math.max(0, scrolledLines - scrollAmount);
        scrolledLines = Math.min(scrolledLines, Math.max(0, lines.size() - visibleLines));

        return true;
    }

    @Override
    public void setCursorPosition(int pPos) {
        this.cursorPos = Mth.clamp(pPos, 0, this.getValue().length());

        int[] cursorXY = getGlobalPosXY(this.cursorPos);
        cursorLine = cursorXY[0];
        cursorIndexInLine = cursorXY[1];

        // 补全或键盘移动可能跨越显示行，始终让实际插入点保持可见。
        int visibleLineCount = Math.max(1, visibleLines);
        if (cursorLine < scrolledLines) {
            scrolledLines = cursorLine;
        } else if (cursorLine >= scrolledLines + visibleLineCount) {
            scrolledLines = cursorLine - visibleLineCount + 1;
        }
        scrolledLines = Mth.clamp(scrolledLines, 0, Math.max(0, lines.size() - visibleLineCount));

        if (!indentLevels.isEmpty() && !lines.isEmpty()) {
            // 计算光标位置
            int indentWidth = indentLevels.get(cursorLine) * indentation * this.font.width(" ");
            int charWidth = this.font.width(lines.get(cursorLine).substring(0, cursorIndexInLine));
            int padding = this.isBordered() ? 4 : 0;
            this.cursorX = this.getX() + padding + indentWidth + charWidth;
            this.cursorY = this.getY() + padding + cursorLine * lineHeight;
        }
    }

    private int getCursorPos(int pDelta) {
        return Util.offsetByCodepoints(this.getValue(), this.cursorPos, pDelta);
    }

    @Override
    public void moveCursor(int delta, boolean selecting) {
        this.moveCursorTo(this.getCursorPos(delta), selecting);
    }

    @Override
    public void moveCursorTo(int position, boolean selecting) {
        this.setCursorPosition(position);
        if (!selecting) {
            this.setHighlightPos(this.cursorPos);
        }
        this.onValueChange(this.getValue());
    }

    @Override
    public void moveCursorToStart(boolean selecting) {
        this.moveCursorTo(0, selecting);
    }

    @Override
    public void moveCursorToEnd(boolean selecting) {
        this.moveCursorTo(this.getValue().length(), selecting);
    }

    public void moveCursor(int pDelta) {
        this.moveCursorTo(this.getCursorPos(pDelta));
    }

    public void moveCursorTo(int pPos) {
//        System.out.println("moveCursorTo " + pPos);
//        System.out.println("shiftPressed " + this.shiftPressed);
        this.setCursorPosition(pPos);
        if (!this.shiftPressed) {
            this.setHighlightPos(this.cursorPos);
        }
        this.onValueChange(this.getValue());
    }

    public void moveCursorVertical(int direction) {
        int[] cursorXY = getGlobalPosXY(this.cursorPos);
        int lineIndex;
        int charIndexInLine;

        if (direction == 1) {
            lineIndex = Math.max(0, cursorXY[0] - 1);
        } else if (direction == -1) {
            lineIndex = Math.min(lines.size() - 1, cursorXY[0] + 1);
        } else {
            return;
        }
        charIndexInLine = Math.min(cursorXY[1], lines.get(lineIndex).length());

        int globalIndex = getGlobalIndexForLine(lineIndex, charIndexInLine);
        this.moveCursorTo(Math.min(globalIndex, this.getValue().length()));
    }

    public void moveCursorToStart() {
        this.moveCursorTo(0);
    }

    public void moveCursorToEnd() {
        this.moveCursorTo(this.getValue().length());
    }

    @Override
    public int getWordPosition(int pNumWords) {
        return this.getWordPosition(pNumWords, this.getCursorPosition());
    }

    private int getWordPosition(int pN, int pPos) {
        return this.getWordPosition(pN, pPos, true);
    }

    private int getWordPosition(int pN, int pPos, boolean pSkipWs) {
        int i = pPos;
        boolean flag = pN < 0;
        int j = Math.abs(pN);

        for(int k = 0; k < j; ++k) {
            if (!flag) {
                int l = this.value.length();
                i = this.value.indexOf(32, i);
                if (i == -1) {
                    i = l;
                } else {
                    while(pSkipWs && i < l && this.value.charAt(i) == ' ') {
                        ++i;
                    }
                }
            } else {
                while(pSkipWs && i > 0 && this.value.charAt(i - 1) == ' ') {
                    --i;
                }

                while(i > 0 && this.value.charAt(i - 1) != ' ') {
                    --i;
                }
            }
        }

        return i;
    }

    @Override
    public void setHighlightPos(int pPosition) {
        int i = this.value.length();
        this.highlightPos = Mth.clamp(pPosition, 0, i);
        if (this.font != null) {
            if (this.displayPos > i) {
                this.displayPos = i;
            }

            int j = this.getInnerWidth();
            String s = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), j);
            int k = s.length() + this.displayPos;
            if (this.highlightPos == this.displayPos) {
                this.displayPos -= this.font.plainSubstrByWidth(this.value, j, true).length();
            }

            if (this.highlightPos > k) {
                this.displayPos += this.highlightPos - k;
            } else if (this.highlightPos <= this.displayPos) {
                this.displayPos -= this.displayPos - this.highlightPos;
            }

            this.displayPos = Mth.clamp(this.displayPos, 0, i);
        }

    }

    @Override
    public int getScreenX(int pCharNum) {
        return pCharNum > this.value.length() ? this.getX() : this.getX() + this.font.width(this.value.substring(0, pCharNum));
    }

}
