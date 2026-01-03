package me.cyanhana.commandblockuioverhaul.ui;

import me.cyanhana.commandblockuioverhaul.mixin.EditBoxAccessor;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ModMultiLineEditBox extends EditBox {
    private final EditBoxAccessor accessor = (EditBoxAccessor) this;
    // 多行文本管理
    private final List<String> lines = new LinkedList<>();
    private final List<Integer> indentLevels = new ArrayList<>();
    private int visibleLines = 10;     // 可视行数
    private int scrolledLines = 0;     // 已滚动行数
    private int maxLength;
    private boolean shiftPressed;
    // 设置
    private final boolean isFormatString = true;  // 是否处理字符串内的换行
    private final int indentation = 2;          // 缩进空格数
    // 光标
    private int cursorLine = 0;              // 光标所在行
    private int cursorIndexInLine = 0;       // 光标在行的位置
    // 布局计算
    private final int lineHeight;       // 行高（根据字体调整）

    public ModMultiLineEditBox(Font pFont, int pX, int pY, int pWidth, int pHeight, Component pMessage) {
        super(pFont, pX, pY, pWidth, pHeight, pMessage);
        // 计算可视行数
        lineHeight = pFont.lineHeight + 2; // 字体高度 + 间距
        visibleLines = pHeight / lineHeight;

    }

    @Override
    public void setValue(@NotNull String text) {
        super.setValue(text);
        // 当文本改变时，重新计算换行
        formatText(text);
//        System.out.println(lineOffsets);
    }
    @Override
    public void insertText(@NotNull String pTextToWrite) {
        int i = Math.min(this.getCursorPosition(), accessor.getHighlightPos());
        int j = Math.max(this.getCursorPosition(), accessor.getHighlightPos());
        int k = this.maxLength - this.getValue().length() - (i - j);
        String s = SharedConstants.filterText(pTextToWrite);
        int l = s.length();
        if (k < l) {
            s = s.substring(0, k);
            l = k;
        }

        String s1 = (new StringBuilder(this.getValue())).replace(i, j, s).toString();
        if (accessor.getFilter().test(s1)) {
            this.setValue(s1);
            this.setCursorPosition(i + l);
            this.setHighlightPos(this.getCursorPosition());
            accessor.invokeOnValueChange(this.getValue());
            // 新加: 设置光标
        }
    }

    @Override
    public void setMaxLength(int maxLength) {
        super.setMaxLength(maxLength);
        this.maxLength = maxLength;
    }

    private void deleteText(int pCount) {
        if (Screen.hasControlDown()) {
            this.deleteWords(pCount);
        } else {
            this.deleteChars(pCount);
        }
    }

    @Override
    public void deleteChars(int pNum) {
        if (!this.getValue().isEmpty()) {
            if (accessor.getHighlightPos() != this.getCursorPosition()) {
                this.insertText("");
            } else {
                int i = accessor.invokeGetCursorPos(pNum);
                int j = Math.min(i, this.getCursorPosition());
                int k = Math.max(i, this.getCursorPosition());
                if (j != k) {
                    String s = (new StringBuilder(this.getValue())).delete(j, k).toString();
                    if (accessor.getFilter().test(s)) {
                        this.setValue(s);
                        this.moveCursorTo(j);
                    }
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
                        System.out.println("default false");
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

        int innerWidth = this.getInnerWidth();

        if (text.isEmpty()) {
            lines.add("");
            indentLevels.add(0);
            return;
        }

        char[] chars = text.toCharArray();
        StringBuilder currentLine = new StringBuilder();
        int currentIndent = 0; // 当前缩进级别
        int bracketDepth = 0;  // 括号深度，用于计算缩进
        boolean inString = false; // 是否在字符串内
        char stringChar = '\0';  // 字符串起始字符 ' 或 "

        // 遍历字符数组
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (!isFormatString) {// 处理字符串（字符串内的字符不触发特殊换行）
                if (!inString && (c == '\'' || c == '"')) {
                    inString = true;
                    stringChar = c;
                } else if (inString && c == stringChar && chars[i - 1] != '\\') {
                    inString = false;
                }
            }

            // 如果不是在字符串内，检查括号
            if (isFormatString || !inString) {
                if (c == '{' || c == '[') {
                    // 左括号前换行并添加当前行
                    if (!currentLine.isEmpty()) {
                        lines.add(currentLine.toString());
                        indentLevels.add(currentIndent);
                        currentLine = new StringBuilder();
                    }

                    // 添加左括号到新行
                    currentLine.append(c);
                    lines.add(currentLine.toString());
                    indentLevels.add(currentIndent);
                    currentLine = new StringBuilder();

                    // 增加括号深度和缩进
                    bracketDepth++;
                    currentIndent = bracketDepth;

                    continue; // 跳过宽度检查，因为已经处理了换行

                } else if (c == '}' || c == ']') {
                    // 右括号前换行并添加当前行
                    if (!currentLine.isEmpty()) {
                        lines.add(currentLine.toString());
                        indentLevels.add(currentIndent);
                        currentLine = new StringBuilder();
                    }

                    // 减少括号深度和缩进
                    bracketDepth = Math.max(0, bracketDepth - 1);
                    currentIndent = bracketDepth;

                    // 添加右括号到新行
                    currentLine.append(c);
                    lines.add(currentLine.toString());
                    indentLevels.add(currentIndent);
                    currentLine = new StringBuilder();

                    continue; // 跳过宽度检查
                }
            }

            // 添加字符到当前行
            currentLine.append(c);

            // 检查宽度是否超过文本框的五分之四
            if (accessor.getFont().width(currentLine.toString()) > innerWidth * 4 / 5) {
                // 找到合适的换行点
                int breakPoint = findBreakPoint(currentLine.toString());

                if (breakPoint > 0 && breakPoint < currentLine.length()) {
                    // 在合适位置换行
                    String line = currentLine.substring(0, breakPoint);
                    lines.add(line);
                    indentLevels.add(currentIndent);

                    // 剩余字符开始新行（保持相同缩进）
                    String remaining = currentLine.substring(breakPoint);
                    currentLine = new StringBuilder(remaining);
                } else {
                    // 强制在当前字符前换行
                    String line = currentLine.substring(0, currentLine.length() - 1);
                    lines.add(line);
                    indentLevels.add(currentIndent);

                    currentLine = new StringBuilder(String.valueOf(c));
                }
            }
        }

        // 添加最后一行
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
            indentLevels.add(currentIndent);
        }

        // 如果没有行，至少添加一个空行
        if (lines.isEmpty()) {
            lines.add("");
            indentLevels.add(0);
        }

    }

    /**
     * 找到合适的换行点（空格、标点等）
     */
    private int findBreakPoint(String line) {
        // 从后往前找，优先使用最近的换行点
        for (int i = line.length() - 1; i > 0; i--) {
            char c = line.charAt(i);
            if (c == ' ') {
                return i; // 在空格处换行
            } else if (c == ',' || c == '.' || c == ':') {
                return i + 1; // 在标点符号后换行
            } else if (c == '(' || c == ')' || c == '{' || c == '}' || c == '[' || c == ']') {
                return i; // 在括号处换行
            } else if (i > 1 && (c == '=' || c == '+' || c == '-' || c == '*' || c == '/')) {
                // 在运算符前换行
                return i;
            }
        }

        // 如果没有找到合适位置，尝试在80%长度处强制换行
        return line.length() * 4 / 5;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.isVisible()) {
            return;
        }

        // 绘制背景（复用父类逻辑）
        if (accessor.getBordered()) {
            int borderColor = this.isFocused() ? -1 : -6250336;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1,
                    this.getX() + this.width + 1, this.getY() + this.height + 1,
                    borderColor);
            guiGraphics.fill(this.getX(), this.getY(),
                    this.getX() + this.width, this.getY() + this.height,
                    -16777216);
        }

        // 颜色与文字坐标
        int textColor = accessor.getIsEditable() ? accessor.getTextColor() : accessor.getTextColorUneditable();
        int baseX = accessor.getBordered() ? this.getX() + 4 : this.getX();
        int baseY = accessor.getBordered() ? this.getY() + (this.height - 8) / 2 : this.getY();

        // 计算渲染区域
        int startLine = scrolledLines;
        int endLine = Math.min(scrolledLines + visibleLines, lines.size());
        int x; // 左边距
        int y = this.getY() + 4; // 顶部边距

        // 渲染可视范围内的每一行
        for (int i = startLine; i < endLine; i++) {
            String line = lines.get(i);
            // x 初始位置
            x = accessor.getBordered() ? this.getX() + 4 : this.getX();
            // 当前行已输入的字符宽度
            int lineWidth = accessor.getFont().width(line);
            // 根据缩进级别添加缩进空格
            int indent = indentLevels.get(i);
            if (indent > 0) {
                String spaces = " ".repeat(indentation).repeat(indent);
                x = spaces.length() * accessor.getFont().width(" ") + x;
            }
            boolean isCursorBetweenLine = this.getCursorPosition() < this.getValue().length() || this.getValue().length() >= this.maxLength;
            // 绘制命令建议
            if (!isCursorBetweenLine && accessor.getSuggestion() != null) {
                guiGraphics.drawString(accessor.getFont(), accessor.getSuggestion(), x + lineWidth, y, -8355712);
            }
            // 绘制行
            guiGraphics.drawString(accessor.getFont(), line, x, y, textColor);
            // 绘制光标
            // 如果聚焦且光标可见
            if (this.isFocused() && accessor.getFrame() / 6 % 2 == 0) {
                // 如果光标不在末尾或已达最大长度(需要显示为竖线的情况)
                if (isCursorBetweenLine) {
                    if (i == cursorLine) {
                        int offsetX = accessor.getFont().width(line.substring(0, Math.min(cursorIndexInLine, line.length())));
                        guiGraphics.fill(RenderType.guiOverlay(), x + offsetX, y - 1, x + 1 + offsetX, y + 1 + 9, -3092272);
                    }
                } else if (i == lines.size() - 1) {
                    guiGraphics.drawString(accessor.getFont(), "_", x + lineWidth, y, textColor);
                }
            }

            // 计算下一行的坐标
            y += lineHeight;
        }


        // j 是当前光标位置在当前显示范围内的偏移量，比如当前光标在第 5 行，第 3 个字符，j = 2；
        // 光标位置
//        int cursorPos = this.getCursorPosition() - this.displayPos;
//        int highlightPos = this.highlightPos - this.displayPos;
//        // 文本行
//        String s = accessor.getFont().plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
//        boolean isCursorVisible = this.getCursorPosition() >= 0 && this.getCursorPosition() <= s.length();
//        int i1 = accessor.getBordered() ? this.getY() + (this.height - 8) / 2 : this.getY();
//        int j1 = x;
//        if (highlightPos > s.length()) {
//            highlightPos = s.length();
//        }
        // 渲染文本
//        if (!s.isEmpty()) {
//            String s1 = isCursorVisible ? s.substring(0, cursorPos) : s;
//            j1 = guiGraphics.drawString(this.font, this.formatter.apply(s1, this.displayPos), l, i1, textColor);
//        }

//        boolean flag2 = this.getCursorPosition() < this.getValue().length() || this.getValue().length() >= accessor.getMaxLength();
//        int k1 = y;
//        if (!isCursorVisible) {
//            k1 = this.getCursorPosition() > 0 ? x + this.width : x;
//        } else if (flag2) {
//            k1 = y - 1;
//            --y;
//        }
//
//        if (!s.isEmpty() && isCursorVisible && cursorPos < s.length()) {
//            guiGraphics.drawString(this.font, this.formatter.apply(s.substring(cursorPos), this.cursorPos), j1, i1, textColor);
//        }
//
//        if (this.hint != null && s.isEmpty() && !this.isFocused()) {
//            guiGraphics.drawString(this.font, this.hint, j1, i1, textColor);
//        }
//
//        if (!flag2 && this.suggestion != null) {
//            guiGraphics.drawString(this.font, this.suggestion, k1 - 1, i1, -8355712);
//        }
                // 绘制光标
//        if (this.isFocused() && accessor.getFrame() / 6 % 2 == 0 && isCursorVisible) {
//            if (flag2) {
//                guiGraphics.fill(RenderType.guiOverlay(), k1, i1 - 1, k1 + 1, i1 + 1 + 9, -3092272);
//            } else {
//                guiGraphics.drawString(accessor.getFont(), "_", k1, i1, textColor);
//            }
//        }

//        if (highlightPos != this.getCursorPosition()) {
//            int l1 = x + accessor.getFont().width(s.substring(0, highlightPos));
//            this.renderHighlight(guiGraphics, k1, i1 - 1, l1 - 1, i1 + 1 + 9);
//        }
    }

    // 鼠标点击事件
    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!this.isVisible() || !this.isMouseOver(mouseX, mouseY)) {
            return;
        }

        // 计算点击的行
        int relativeY = (int) mouseY - this.getY();
        int clickedLine = scrolledLines + (relativeY / lineHeight);

        if (clickedLine >= 0 && clickedLine < lines.size()) {
            String line = lines.get(clickedLine);

            // 计算点击的字符位置
            int relativeX = (int) mouseX - (this.getX() + (accessor.getBordered() ? 4 : 0));
            int charIndex = 0;
            int accumulatedWidth = 0;
            int indent = indentLevels.get(clickedLine);
            // 计算缩进宽度
            if (indent > 0) {
                String spaces = " ".repeat(indentation).repeat(indent);
                accumulatedWidth = spaces.length() * accessor.getFont().width(" ");
            }
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                // 计算字符宽度
                accumulatedWidth += accessor.getFont().width(String.valueOf(c));

                if (accumulatedWidth > relativeX) {
//                    System.out.println("char " + c);
                    break;
                }
                charIndex++;
            }
            // 在 moveCursorTo 中赋值
//            cursorLine = clickedLine;
//            cursorIndexInLine = charIndex;

            // 计算全局光标位置（需要知道每行的起始索引）
            int globalIndex = getGlobalIndexForLine(clickedLine, charIndex);
//            System.out.println("globalIndex " + globalIndex);
            this.moveCursorTo(Math.min(globalIndex, this.getValue().length()));
//            System.out.println("moveCursorTo " + Math.min(globalIndex, this.getValue().length()));
        }
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

    private int[] getCursorXY() {
        int[] pos = new int[2];
        int lineIndex = 0;
        int charCount = 0;
        int cursorIndex = this.getCursorPosition();
        // 计算光标位置在哪一行
        for (String line : lines) {
            if (charCount + line.length() >= cursorIndex) {
                // 计算光标位置在这一行的哪个位置
                for (int i = 0; i <= line.length(); i++) {
                    if (charCount + i == cursorIndex) {
                        System.out.println("charCount: " + charCount + " i: " + i);
                        pos[1] = i;
                        if (i == 47) System.out.println(line);
                        break;
                    }
                }
                break;
            } else {
                charCount += line.length();
                lineIndex++;
            }
        }
        pos[0] = lineIndex;

        return pos;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
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
        accessor.setCursorPos(Mth.clamp(pPos, 0, this.getValue().length()));

        int[] cursorXY = getCursorXY();
        cursorLine = cursorXY[0];
        cursorIndexInLine = cursorXY[1];
    }

    @Override
    public void moveCursorTo(int pPos) {
        this.setCursorPosition(pPos);
        if (!this.shiftPressed) {
            this.setHighlightPos(this.getCursorPosition());
        }
        accessor.invokeOnValueChange(this.getValue());
    }

    public void moveCursorVertical(int direction) {
        int[] cursorXY = getCursorXY();
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

}
