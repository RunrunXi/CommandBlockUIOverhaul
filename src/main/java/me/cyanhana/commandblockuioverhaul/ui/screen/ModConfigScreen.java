package me.cyanhana.commandblockuioverhaul.ui.screen;

import me.cyanhana.commandblockuioverhaul.Config;
import me.cyanhana.commandblockuioverhaul.ui.CommandHierarchyColors;
import me.cyanhana.commandblockuioverhaul.ui.ModMultiLineEditBox;
import me.cyanhana.commandblockuioverhaul.ui.ModCommandSuggestions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/** 配置只改变客户端显示；返回原编辑界面时重新排版并保留草稿与撤销历史。 */
public class ModConfigScreen extends Screen {
    private final Screen parent;
    private int left;
    private int optionsX;
    private int numbersX;
    private int columnWidth;
    private boolean compact;
    private boolean draggingPreviewScroll;
    private double scrollGrabOffset;
    private final List<Caption> captions = new ArrayList<>();
    private record Caption(Component text, int x, int y, int maxWidth) {
        Caption(Component text, int x, int y) { this(text, x, y, Integer.MAX_VALUE); }
    }
    private int controlsY;
    private ModMultiLineEditBox preview;
    private ModCommandSuggestions previewSuggestions;
    private String previewText = "tellraw @a {\"text\":\"Root { brackets in a string }\",\"extra\":[{\"text\":\" Child\",\"extra\":[{\"text\":\" Grandchild: a long example to demonstrate automatic wrapping and indentation.\"}]}]}";

    public ModConfigScreen(Screen parent) {
        super(label("title"));
        this.parent = parent;
    }

    private static Component label(String key) {
        return Component.translatable("commandblockuioverhaul.config." + key);
    }

    private static Component schemeName(Config.ColorScheme scheme) {
        return label(scheme == Config.ColorScheme.MODERN ? "modern" : "vanilla");
    }

    @Override
    protected void init() {
        // 【坐标规则】以屏幕左上角为 (0, 0)，X 增大向右，Y 增大向下。
        // width/height 是经过 Minecraft“GUI 缩放”后的逻辑尺寸，不是显示器实际像素。
        // bounds/create 的位置参数顺序为：X、Y、宽、高；pos 只有 X、Y。
        // 【整体布局】先调整下面的列起点与 controlsY，再调整各控件的局部偏移。
        captions.clear();
        // 【预览尺寸】宽度低于 560 时增加预览占比；配置区始终保持左中右三等分。
        compact = this.width < 560;
        // 【命令预览宽度】窄屏占 80%，宽屏占 64%，比原布局更窄；居中放置。
        int previewWidth = Math.max(160, (int) (this.width * (compact ? 0.80 : 0.64)));
        int previewX = (this.width - previewWidth) / 2;
        // 【配置区】沿用你修改的宽度，与上方预览框左右对齐。
        int controlsWidth = Math.min(this.width - 32, previewWidth);
        left = (this.width - controlsWidth) / 2;
        // 【三等分】左栏 [left, left+1/3)，中栏 [left+1/3, left+2/3)，右栏 [left+2/3, right)。
        // 取整最多产生 1 像素差异，不再使用固定 180 列宽或窄屏换列逻辑。
        columnWidth = controlsWidth / 3;
        int middleLeft = left + controlsWidth / 3;
        int rightLeft = left + controlsWidth * 2 / 3;
        optionsX = middleLeft + 4; // 中栏开关，距离本栏左边缘 4。
        numbersX = rightLeft + 4; // 右栏配色和数值框，距离本栏左边缘 4。
        // 【配置区 Y】沿用你修改的底部预留 90；三个区域从同一个 controlsY 开始。
        controlsY = this.height - 90;
        int previewHeight = Math.max(36, controlsY - 18 - 28);
        // 【返回按钮】左上角 (8, 8)，宽 48、高 20。
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> this.onClose())
                .bounds(8, 8, 48, 20).build());
        ModMultiLineEditBox previousPreview = this.preview;
        // 构造器的位置参数：previewX、顶部 Y=28、previewWidth、previewHeight。
        this.preview = new ModMultiLineEditBox(this.font, previewX,
                28, previewWidth, previewHeight, label("commandPreview"));
        this.preview.setMaxLength(32500);
        // 复用正式编辑器的解析着色与排版，但不绘制补全窗口，不提交预览命令。
        this.previewSuggestions = new ModCommandSuggestions(this.minecraft, this, this.preview,
                this.font, true, true, 0, 7, false, CommandHierarchyColors.popupBackground());
        this.previewSuggestions.setAllowSuggestions(false);
        this.preview.setResponder(text -> {
            this.previewText = text;
            this.previewSuggestions.updateCommandInfo();
        });
        this.preview.setCommandSuggestions(this.previewSuggestions);
        if (previousPreview == null) {
            this.preview.setValue(this.previewText);
            this.preview.moveCursorToStart(false);
        } else {
            this.preview.copyStateFrom(previousPreview);
        }
        this.addRenderableWidget(this.preview);
        // 【左栏断行矩阵】以左侧 1/3 区域的中心为锚点定位全部控件。
        int matrixCenterX = left + columnWidth / 2;
        int beforeX = matrixCenterX - 22;
        int afterX = matrixCenterX + 6;
        captions.add(new Caption(label("lineBreaks"),
                matrixCenterX - Math.min(this.font.width(label("lineBreaks")), columnWidth - 4) / 2,
                controlsY - 12, columnWidth - 4));
        // “前”右对齐到第一列左边缘前 4；“后”放在第二列右边缘后 4。
        // 英文标签在窄栏中按可用宽度缩放，避免跨入相邻区域。
        int beforeWidth = Math.max(1, beforeX - 4 - (left + 2));
        captions.add(new Caption(label("before"), beforeX - 4
                - Math.min(this.font.width(label("before")), beforeWidth), controlsY + 5, beforeWidth));
        int afterLabelX = afterX + 17 + 4;
        captions.add(new Caption(label("after"), afterLabelX, controlsY + 5,
                Math.max(1, middleLeft - 2 - afterLabelX)));
        // 【五个断行勾选框】最后一个参数是行号：0=左括号，1=右括号，2=逗号。
        // 实际 Y 在 matrix() 中按 controlsY + 行号*24 计算。
        matrix("beforeOpen", Config.BREAK_BEFORE_OPEN, beforeX, 0);
        matrix("afterOpen", Config.BREAK_AFTER_OPEN, afterX, 0);
        matrix("beforeClose", Config.BREAK_BEFORE_CLOSE, beforeX, 1);
        matrix("afterClose", Config.BREAK_AFTER_CLOSE, afterX, 1);
        matrix("afterComma", Config.BREAK_AFTER_COMMA, afterX, 2);
        // 【左栏符号】放在两列勾选框之间；行距与 matrix() 的 24 保持一致。
        captions.add(new Caption(Component.literal("{"), matrixCenterX - 2, controlsY + 5));
        captions.add(new Caption(Component.literal("}"), matrixCenterX - 2, controlsY + 29));
        captions.add(new Caption(Component.literal(","), matrixCenterX - 2, controlsY + 53));
        // 【中栏】两个开关始终在中间 1/3 内，顶部分别为 controlsY 和 controlsY+24。
        toggle("formatStrings", Config.FORMAT_STRINGS, optionsX, controlsY);
        toggle("avoidEmpty", Config.AVOID_EMPTY_LINES, optionsX, controlsY + 24);
        // 【右栏下方】缩进、行宽在配色下方竖直排列。
        number("indentation", Config.INDENTATION, 0, 16, numbersX, controlsY + 24);
        number("wrapWidth", Config.WRAP_WIDTH, 10, 6400, numbersX, controlsY + 48);
        // 【右栏上方】配色按钮填充右栏，左右各保留约 4。
        int paletteX = numbersX;
        int paletteY = controlsY;
        int paletteWidth = controlsWidth - (rightLeft - left) - 8;
        this.addRenderableWidget(CycleButton.builder(ModConfigScreen::schemeName)
                .withValues(Config.ColorScheme.values()).withInitialValue(Config.COLOR_SCHEME.get())
                .create(paletteX, paletteY, paletteWidth, 20, label("palette"),
                        (button, scheme) -> { Config.setColorScheme(scheme); refreshPreview(); }));
    }

    private void matrix(String key, ForgeConfigSpec.BooleanValue value, int x, int row) {
        // 【矩阵行距】24；17 是无文字勾选框的宽度。这里统一控制五个勾选框的 Y。
        checkbox(Component.empty(), key, value, x, controlsY + row * 24, 17);
    }

    private void toggle(String key, ForgeConfigSpec.BooleanValue value, int x, int y) {
        // 控件文字宽度按中栏尺寸计算，预留勾选框 17、文字间距 4 和栏边距 8。
        checkbox(label(key), key, value, x, y, Math.max(21, columnWidth - 8 - 21));
    }

    private void checkbox(Component message, String key, ForgeConfigSpec.BooleanValue value, int x, int y, int width) {
        // 空标签矩阵用 Tooltip 和旁白解释每一个勾选框的含义。
        // Forge 1.20.1 没有 Checkbox.builder；使用原版控件并缩放方框到 17 像素。
        boolean hasLabel = !message.getString().isEmpty();
        Checkbox checkbox = new Checkbox(x, y, hasLabel ? width + 21 : 17, 20,
                hasLabel ? message : label(key), value.get(), false) {
            @Override
            public void onPress() {
                super.onPress();
                value.set(this.selected());
                Config.save();
                refreshPreview();
            }

            @Override
            public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                graphics.pose().pushPose();
                graphics.pose().translate(getX(), getY(), 0);
                graphics.pose().scale(0.85F, 0.85F, 1);
                graphics.pose().translate(-getX(), -getY(), 0);
                super.renderWidget(graphics, mouseX, mouseY, partialTick);
                graphics.pose().popPose();
                if (hasLabel) {
                    float scale = Math.min(1.0F, (float) width / Math.max(1, font.width(message)));
                    graphics.pose().pushPose();
                    graphics.pose().translate(getX() + 21, getY() + 5, 0);
                    graphics.pose().scale(scale, scale, 1);
                    graphics.drawString(font, message, 0, 0, 0xE0E0E0);
                    graphics.pose().popPose();
                }
            }
        };
        checkbox.setTooltip(Tooltip.create(label(key)));
        this.addRenderableWidget(checkbox);
    }

    private void number(String key, ForgeConfigSpec.IntValue value, int min, int max, int x, int y) {
        // 【右栏数值框】宽度最多 42；窄栏时减小输入框，剩余宽度留给右侧标签。
        int inputWidth = Math.min(42, Math.max(28, (columnWidth - 8) / 3));
        EditBox input = new EditBox(this.font, x, y, inputWidth, 20, label(key));
        captions.add(new Caption(label(key), x + inputWidth + 6, y + 6,
                Math.max(1, columnWidth - 8 - inputWidth - 6)));
        input.setMaxLength(4);
        input.setFilter(text -> text.chars().allMatch(c -> c >= '0' && c <= '9'));
        input.setValue(Integer.toString(value.get()));
        input.setTooltip(Tooltip.create(Component.translatable(
                "commandblockuioverhaul.config.range", min, max)));
        // 允许暂时清空输入框；仅将范围内的完整数字写入配置，非法值以红色标记。
        input.setResponder(text -> {
            try {
                int number = Integer.parseInt(text);
                boolean valid = number >= min && number <= max;
                input.setTextColor(valid ? 0xE0E0E0 : 0xFF5555);
                if (valid) { value.set(number); Config.save(); refreshPreview(); }
            } catch (NumberFormatException ignored) {
                input.setTextColor(0xFF5555);
            }
        });
        this.addRenderableWidget(input);
    }

    /** 1.20.1 的输入框依赖 tick 更新光标闪烁。 */
    @Override
    public void tick() {
        for (var child : this.children()) {
            if (child instanceof EditBox input) input.tick();
        }
    }

    @Override
    public void onClose() {
        this.setDragging(false);
        this.draggingPreviewScroll = false;
        this.minecraft.setScreen(this.parent);
        if (this.parent instanceof AbstractModCommandBlockScreen commandScreen) {
            commandScreen.returnFromConfig();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        // 【配置标题】顶部居中，Y=10；与左侧返回按钮及 Y=28 的预览框错开。
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        for (Caption caption : captions) {
            // 所有文字标签的位置在 init()/number() 中登记；这里不再额外偏移。
            int textWidth = this.font.width(caption.text());
            float scale = Math.min(1.0F, (float) caption.maxWidth() / Math.max(1, textWidth));
            graphics.pose().pushPose();
            graphics.pose().translate(caption.x(), caption.y(), 0);
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(this.font, caption.text(), 0, 0, 0xE0E0E0);
            graphics.pose().popPose();
        }
        // 【竖向滚动条】预览框右侧间隔 4，宽 6，顶部与高度跟随预览框。
        // 修改 4 或 6 后，必须同步修改 mouseClicked() 中的命中区域。
        int scrollbarX = this.preview.getX() + this.preview.getWidth() + 4;
        graphics.fill(scrollbarX, this.preview.getY(), scrollbarX + 6,
                this.preview.getY() + this.preview.getHeight(), CommandHierarchyColors.popupBackground());
        int thumbY = previewThumbY();
        graphics.fill(scrollbarX, thumbY, scrollbarX + 6, thumbY + previewThumbHeight(), 0xFF6F737A);
    }

    private int previewThumbHeight() {
        // 【滚动滑块高度】按可见行占比计算，最少 12；不改变整个滚动条的位置。
        int total = this.preview.getMaxScrolledLines() + this.preview.getVisibleLineCount();
        return Math.max(12, this.preview.getHeight() * this.preview.getVisibleLineCount() / Math.max(1, total));
    }

    private int previewThumbY() {
        // 【滚动滑块 Y】从预览顶部开始，按当前滚动比例在轨道内移动。
        int travel = this.preview.getHeight() - previewThumbHeight();
        return this.preview.getY() + travel * this.preview.getScrolledLines()
                / Math.max(1, this.preview.getMaxScrolledLines());
    }

    private void dragPreviewScroll(double mouseY) {
        int travel = this.preview.getHeight() - previewThumbHeight();
        if (travel > 0) this.preview.scrollToLine((int) Math.round(
                (mouseY - this.preview.getY() - scrollGrabOffset) * this.preview.getMaxScrolledLines() / travel));
    }

    private void refreshPreview() {
        if (this.preview != null) this.preview.refreshFormatting();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 【滚动条点击区域】此处的间隔 4、宽度 6 必须与 render() 中保持一致。
        int scrollbarX = this.preview.getX() + this.preview.getWidth() + 4;
        if (button == 0 && mouseX >= scrollbarX && mouseX < scrollbarX + 6
                && mouseY >= this.preview.getY() && mouseY < this.preview.getY() + this.preview.getHeight()) {
            this.draggingPreviewScroll = true;
            int thumbY = previewThumbY();
            this.scrollGrabOffset = mouseY >= thumbY && mouseY < thumbY + previewThumbHeight()
                    ? mouseY - thumbY : previewThumbHeight() / 2.0;
            dragPreviewScroll(mouseY);
            return true;
        }
        if (this.preview != null && !this.preview.isMouseOver(mouseX, mouseY)) {
            if (this.getFocused() == this.preview) this.setFocused(null);
            this.preview.setFocused(false);
            this.setDragging(false);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.draggingPreviewScroll && button == 0) {
            dragPreviewScroll(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingPreviewScroll) {
            this.draggingPreviewScroll = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
