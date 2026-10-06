package me.cyanhana.commandblockuioverhaul.ui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** 嵌入命令界面的悬浮调色板；作为独立控件容器使用，不调用 Minecraft.setScreen。 */
public class ModPaletteScreen extends Screen {
    private final AbstractModCommandBlockScreen parent;
    private final int[] channels = new int[3];
    private final EditBox[] inputs = new EditBox[3];
    private final ChannelSlider[] sliders = new ChannelSlider[3];
    private EditBox hexOutput;
    private EditBox decimalOutput;
    private boolean synchronizing;
    private int left;
    private int top;
    private int contentWidth;

    public ModPaletteScreen(AbstractModCommandBlockScreen parent, int color) {
        super(label("title"));
        this.parent = parent;
        for (int i = 0; i < 3; ++i) channels[i] = color >> ((2 - i) * 8) & 255;
    }

    private static Component label(String key) {
        return Component.translatable("commandblockuioverhaul.palette." + key);
    }

    private int rgb() {
        return channels[0] << 16 | channels[1] << 8 | channels[2];
    }

    @Override
    protected void init() {
        // 【紧凑悬浮布局】内容宽 100；RGB 行距 18，输出两行，不另占一排放关闭按钮。
        contentWidth = Math.min(100, this.width - 24);
        // 面板右边缘停在 RGB 按钮左侧，保证小窗口下按钮也不会被面板挡住。
        left = Math.max(12, Math.min(parent.paletteButton.getX() - 8, this.width - contentWidth - 8));
        top = Math.max(22, Math.min(parent.paletteButton.getY() - 100, this.height - 98));
        for (int i = 0; i < 3; ++i) {
            final int channel = i;
            EditBox input = new EditBox(this.font, left + 12, top + i * 18, 28, 16,
                    Component.literal(new String[]{"R", "G", "B"}[i]));
            input.setMaxLength(3);
            input.setFilter(text -> text.chars().allMatch(c -> c >= '0' && c <= '9'));
            input.setValue(Integer.toString(channels[i]));
            inputs[i] = this.addRenderableWidget(input);
            sliders[i] = this.addRenderableWidget(new ChannelSlider(left + 46, top + i * 18,
                    contentWidth - 46, channel));
            input.setResponder(text -> {
                if (synchronizing) return;
                try {
                    int value = Integer.parseInt(text);
                    if (value > 255) { input.setTextColor(0xFF5555); return; }
                    input.setTextColor(0xE0E0E0);
                    channels[channel] = value;
                    sliders[channel].syncValue();
                    updateOutputs();
                } catch (NumberFormatException ignored) {
                    input.setTextColor(0xFF5555);
                }
            });
        }
        // 【颜色值输出】预览色块右侧，十进制在上，#RRGGBB 在下；右侧按钮复制对应值。
        decimalOutput = output(left + 18, top + 56, "decimal");
        hexOutput = output(left + 18, top + 74, "hex");
        updateOutputs();
        this.addRenderableWidget(Button.builder(Component.literal("x"), button -> this.onClose())
                .bounds(left + contentWidth - 14, top - 16, 14, 14).build());
    }

    private EditBox output(int x, int y, String key) {
        EditBox box = new EditBox(this.font, x, y, contentWidth - 34, 16, label(key));
        box.setMaxLength(16);
        box.setEditable(false);
        this.addRenderableWidget(box);
        this.addRenderableWidget(Button.builder(Component.literal("C"), button ->
                this.minecraft.keyboardHandler.setClipboard(box.getValue()))
                .bounds(left + contentWidth - 14, y, 14, 14).build());
        return box;
    }

    private void updateOutputs() {
        // 每次合法输入或滑条变化就更新缓存，不依赖用户先关闭调色板。
        // 即使直接关闭命令界面，下次打开仍会保留当前颜色。
        parent.rememberPaletteColor(rgb());
        decimalOutput.setValue(Integer.toString(rgb()));
        hexOutput.setValue(String.format(Locale.ROOT, "#%06X", rgb()));
    }

    /** 1.20.1 的输入框依赖 tick 更新光标闪烁。 */
    @Override
    public void tick() {
        for (var child : this.children()) {
            if (child instanceof EditBox input) input.tick();
        }
    }

    /** 显式解除子控件焦点：1.20.1 尚未提供 Screen.clearFocus。 */
    public void clearFocus() {
        if (this.getFocused() != null) this.getFocused().setFocused(false);
        this.setFocused(null);
    }

    @Override
    public void onClose() {
        this.setDragging(false);
        parent.rememberPaletteColor(rgb());
        parent.closePaletteWindow();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, left + contentWidth / 2, top - 14, 0xFFFFFF);
        String[] names = {"R", "G", "B"};
        int[] colors = {0xFF5555, 0x55FF55, 0x5555FF};
        for (int i = 0; i < 3; ++i) {
            graphics.drawString(this.font, names[i], left, top + i * 18 + 4, colors[i]);
        }
        graphics.fill(left, top + 56, left + 12, top + 90, 0xFF000000 | rgb());
    }

    /** 绘制有界面板背景，避免 Screen 的全屏背景模糊再次覆盖命令编辑界面。 */
    @Override
    public void renderBackground(GuiGraphics graphics) {
        graphics.fill(left - 4, top - 20, left + contentWidth + 4, top + 94, 0xFF43454A);
        graphics.fill(left - 3, top - 19, left + contentWidth + 3, top + 93, 0xFF1E1F22);
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= left - 4 && mouseX < left + contentWidth + 4
                && mouseY >= top - 20 && mouseY < top + 94;
    }

    private class ChannelSlider extends AbstractSliderButton {
        private final int channel;

        ChannelSlider(int x, int y, int width, int channel) {
            super(x, y, width, 16, Component.empty(), channels[channel] / 255.0);
            this.channel = channel;
            updateMessage();
        }

        void syncValue() {
            this.value = channels[channel] / 255.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Component.literal(new String[]{"R", "G", "B"}[channel] + ": " + channels[channel]));
        }

        @Override
        protected void applyValue() {
            channels[channel] = (int) Math.round(this.value * 255);
            synchronizing = true;
            try {
                inputs[channel].setValue(Integer.toString(channels[channel]));
                inputs[channel].setTextColor(0xE0E0E0);
            } finally {
                synchronizing = false;
            }
            updateOutputs();
            updateMessage();
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // 通道渐变从黑色到纯红/绿/蓝；只替换绘制，沿用原版滑条的鼠标与键盘交互。
            int shift = (2 - channel) * 8;
            for (int x = 0; x < this.width; ++x) {
                int intensity = x * 255 / Math.max(1, this.width - 1);
                graphics.fill(getX() + x, getY(), getX() + x + 1, getY() + this.height,
                        0xFF000000 | intensity << shift);
            }
            // 原版滑条以两端各 4 像素为鼠标映射边界，滑块标记也采用相同范围。
            int thumb = getX() + 4 + (int) Math.round(value * (this.width - 8));
            graphics.fill(thumb - 1, getY(), thumb + 1, getY() + this.height,
                    this.isFocused() || this.isHovered() ? 0xFFFFFFFF : 0xFFA0A0A0);
        }
    }
}
