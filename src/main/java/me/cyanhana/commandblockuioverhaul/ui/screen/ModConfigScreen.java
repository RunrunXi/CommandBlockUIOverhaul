package me.cyanhana.commandblockuioverhaul.ui.screen;

import me.cyanhana.commandblockuioverhaul.Config;
import me.cyanhana.commandblockuioverhaul.ui.CommandHierarchyColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** 配置仅改变客户端显示，返回时复用原编辑界面，避免提交或丢失命令草稿。 */
public class ModConfigScreen extends Screen {
    private final Screen parent;

    public ModConfigScreen(Screen parent) {
        super(Component.translatable("commandblockuioverhaul.config.title"));
        this.parent = parent;
    }

    private static Component schemeName(Config.ColorScheme scheme) {
        return Component.translatable(scheme == Config.ColorScheme.MODERN
                ? "commandblockuioverhaul.config.modern" : "commandblockuioverhaul.config.vanilla");
    }

    @Override
    protected void init() {
        this.addRenderableWidget(CycleButton.<Config.ColorScheme>builder(ModConfigScreen::schemeName)
                .withValues(Config.ColorScheme.values()).withInitialValue(Config.COLOR_SCHEME.get())
                .create(this.width / 2 - 130, this.height / 2 - 30, 260, 20,
                        Component.translatable("commandblockuioverhaul.config.palette"),
                        (button, scheme) -> Config.setColorScheme(scheme)));
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(this.width / 2 - 100, this.height / 2 + 55, 200, 20).build());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 65, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.translatable("commandblockuioverhaul.config.preview"),
                this.width / 2, this.height / 2, CommandHierarchyColors.color(0));
        // 预览直接取与编辑器相同的色板，点击方案后立即刷新。
        graphics.fill(this.width / 2 - 110, this.height / 2 + 16,
                this.width / 2 + 110, this.height / 2 + 40, CommandHierarchyColors.editorBackground());
        for (int i = 0; i < 7; ++i) {
            graphics.drawCenteredString(this.font, Integer.toString(i + 1), this.width / 2 - 90 + i * 30,
                    this.height / 2 + 24, CommandHierarchyColors.color(i));
        }
    }
}
