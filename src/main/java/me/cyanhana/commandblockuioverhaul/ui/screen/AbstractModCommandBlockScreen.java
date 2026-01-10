package me.cyanhana.commandblockuioverhaul.ui.screen;

import me.cyanhana.commandblockuioverhaul.ui.ModCommandSuggestions;
import me.cyanhana.commandblockuioverhaul.ui.ModMultiLineEditBox;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractModCommandBlockScreen extends Screen {
    private static final Component SET_COMMAND_LABEL = Component.translatable("advMode.setCommand");
    private static final Component COMMAND_LABEL = Component.translatable("advMode.command");
    private static final Component PREVIOUS_OUTPUT_LABEL = Component.translatable("advMode.previousOutput");
    private static final Component DESCRIBE_MESSAGE = Component.translatable("advMode.command");
    protected ModMultiLineEditBox commandEdit;
    protected EditBox previousEdit;
    protected Button doneButton;
    protected Button cancelButton;
    protected CycleButton<Boolean> outputButton;
    ModCommandSuggestions commandSuggestions;

    public AbstractModCommandBlockScreen() {
        super(GameNarrator.NO_TITLE);
    }

    public void tick() {
        this.commandEdit.tick();
        if (!this.getCommandBlock().isValid()) {
            this.onClose();
        }

    }

    abstract BaseCommandBlock getCommandBlock();

    protected void init() {
        int outputButtonX = this.width / 2 + 150 - 20;
        int outputButtonY = this.height / 6 * 5 - 35;
        int commandEditWidth = this.width / 4 * 3;
        // 完成按钮
        this.doneButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (p_97691_) -> {
            this.onDone();
        }).bounds(this.width / 2 - 4 - 150, this.height / 6 * 5 + 10, 150, 20).build());
        // 取消按钮
        this.cancelButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (p_289627_) -> {
            this.onClose();
        }).bounds(this.width / 2 + 4, this.height / 6 * 5 + 10, 150, 20).build());
        boolean flag = this.getCommandBlock().isTrackOutput();
        // 输出按钮
        this.outputButton = this.addRenderableWidget(CycleButton.booleanBuilder(Component.literal("O"), Component.literal("X"))
                .withInitialValue(flag).displayOnlyValue()
                .create(outputButtonX, outputButtonY, 20, 20, Component.translatable("advMode.trackOutput"), (p_169596_, p_169597_) -> {
            BaseCommandBlock basecommandblock = this.getCommandBlock();
            basecommandblock.setTrackOutput(p_169597_);
            this.updatePreviousOutput(p_169597_);
        }));
        // 命令输入框
        this.commandEdit = new ModMultiLineEditBox
                (this.font, (this.width - commandEditWidth) / 2, this.height / 6 - 22,
                        commandEditWidth, this.height / 5 * 3, DESCRIBE_MESSAGE)
        {
            protected @NotNull MutableComponent createNarrationMessage() {
                return super.createNarrationMessage().append(AbstractModCommandBlockScreen.this.commandSuggestions.getNarrationMessage());
            }
        };
        this.commandEdit.setMaxLength(32500);
        this.commandEdit.setResponder(this::onEdited);
        this.addWidget(this.commandEdit);
        // 输出框
        this.previousEdit = new EditBox(this.font, this.width / 2 - 150, outputButtonY, 276, 20, Component.translatable("advMode.previousOutput"));
        this.previousEdit.setMaxLength(32500);
        this.previousEdit.setEditable(false);
        this.previousEdit.setValue("-");
        this.addWidget(this.previousEdit);
        // 设置初始焦点
        this.setInitialFocus(this.commandEdit);
        // 初始化命令建议器
        this.commandSuggestions = new ModCommandSuggestions(this.minecraft, this, this.commandEdit,
                this.font, true, true, 0, 7, false, Integer.MIN_VALUE);
        this.commandSuggestions.setAllowSuggestions(true);
        this.commandSuggestions.updateCommandInfo();
        // 初始化输出框
        this.updatePreviousOutput(flag);
    }

    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        String s = this.commandEdit.getValue();
        this.init(pMinecraft, pWidth, pHeight);
        this.commandEdit.setValue(s);
        this.commandSuggestions.updateCommandInfo();
    }

    protected void updatePreviousOutput(boolean pTrackOutput) {
        this.previousEdit.setValue(pTrackOutput ? this.getCommandBlock().getLastOutput().getString() : "-");
    }

    protected void onDone() {
        BaseCommandBlock basecommandblock = this.getCommandBlock();
        this.populateAndSendPacket(basecommandblock);
        if (!basecommandblock.isTrackOutput()) {
            basecommandblock.setLastOutput((Component)null);
        }

        this.minecraft.setScreen((Screen)null);
    }

    protected abstract void populateAndSendPacket(BaseCommandBlock pCommandBlock);

    private void onEdited(String p_97689_) {
        this.commandSuggestions.updateCommandInfo();
    }

    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (this.commandSuggestions.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
        } else if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
        } else if (pKeyCode != 257 && pKeyCode != 335) {
            return false;
        } else {
            this.onDone();
            return true;
        }
    }

    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        return this.commandSuggestions.mouseScrolled(pDelta) || super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        return this.commandSuggestions.mouseClicked(pMouseX, pMouseY, pButton) || super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        // 背景色
        this.renderBackground(pGuiGraphics);
        // 中央文本
        pGuiGraphics.drawCenteredString(this.font, SET_COMMAND_LABEL, this.width / 2, 10, 16777215);
        // 输入框上方文本
//        pGuiGraphics.drawString(this.font, COMMAND_LABEL, this.width / 2 - 150, 40, 10526880);
        // 命令输入框
        this.commandEdit.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        int i = 75;
        if (!this.previousEdit.getValue().isEmpty()) {
//            i += 5 * 9 + 1 + (this.height / 5 * 4 - 40) - 135;
//            pGuiGraphics.drawString(this.font, PREVIOUS_OUTPUT_LABEL, this.width / 2 - 150, i + 4, 10526880);
            this.previousEdit.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }

        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        // 命令建议
        this.commandSuggestions.render(pGuiGraphics, pMouseX, pMouseY);
    }
}
