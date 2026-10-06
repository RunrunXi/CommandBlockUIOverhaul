package me.cyanhana.commandblockuioverhaul.ui.screen;

import me.cyanhana.commandblockuioverhaul.ui.ModCommandSuggestions;
import me.cyanhana.commandblockuioverhaul.ui.CommandHierarchyColors;
import me.cyanhana.commandblockuioverhaul.ui.ModMultiLineEditBox;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.BaseCommandBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
    protected Button configButton;
    ModCommandSuggestions commandSuggestions;
    protected boolean trackOutput;
    private boolean initialized;
    private int configReturnCursor;
    private int configReturnAnchor;

    public AbstractModCommandBlockScreen() {
        super(GameNarrator.NO_TITLE);
    }

    public void tick() {

        if (!this.getCommandBlock().isValid()) {
            this.onClose();
        }

    }

    abstract BaseCommandBlock getCommandBlock();

    protected void init() {
        // setScreen 返回和窗口缩放都会重建控件，保留旧编辑器的草稿、选区与历史。
        ModMultiLineEditBox previous = this.commandEdit;
        // 为右侧配置按钮预留空间，小窗口下也不让按钮超出屏幕。
        int outputButtonX = Math.min(this.width / 2 + 150 - 20, this.width - 46);
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
        if (!this.initialized) {
            this.trackOutput = this.getCommandBlock().isTrackOutput();
            this.initialized = true;
        }
        boolean flag = this.trackOutput;
        // 输出按钮
        this.outputButton = this.addRenderableWidget(CycleButton.booleanBuilder(Component.literal("O"), Component.literal("X"))
                .withInitialValue(flag).displayOnlyValue()
                .create(outputButtonX, outputButtonY, 20, 20, Component.translatable("advMode.trackOutput"), (p_169596_, p_169597_) -> {
            this.trackOutput = p_169597_;
            this.updatePreviousOutput(p_169597_);
        }));
        // 命令输入框
        this.configButton = this.addRenderableWidget(Button.builder(Component.literal("..."), button -> {
            this.configReturnCursor = this.commandEdit.getCursorPosition();
            this.configReturnAnchor = this.commandEdit.getSelectionAnchor();
            this.commandSuggestions.hide();
            this.setDragging(false);
            this.commandEdit.resetSelectionModifier();
            this.minecraft.setScreen(new ModConfigScreen(this));
        }).bounds(outputButtonX + 24, outputButtonY, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("commandblockuioverhaul.config.title"))).build());
        this.commandEdit = new ModMultiLineEditBox
                (this.font, (this.width - commandEditWidth) / 2, this.height / 6 - 22,
                        commandEditWidth, this.height / 5 * 3, DESCRIBE_MESSAGE)
        {
            protected @NotNull MutableComponent createNarrationMessage() {
                MutableComponent message = super.createNarrationMessage();
                return commandSuggestions == null ? message : message.append(commandSuggestions.getNarrationMessage());
            }
        };
        this.commandEdit.setMaxLength(32500);
        this.commandEdit.setResponder(this::onEdited);
        this.addWidget(this.commandEdit);
        // 输出框
        this.previousEdit = new EditBox(this.font, this.width / 2 - 150, outputButtonY,
                Math.max(1, outputButtonX - (this.width / 2 - 150) - 4), 20, Component.translatable("advMode.previousOutput"));
        this.previousEdit.setMaxLength(32500);
        this.previousEdit.setEditable(false);
        this.previousEdit.setValue("-");
        this.addWidget(this.previousEdit);
        // 设置初始焦点
        this.setInitialFocus(this.commandEdit);
        // 初始化命令建议器
        this.commandSuggestions = new ModCommandSuggestions(this.minecraft, this, this.commandEdit,
                this.font, true, true, 0, 7, false, CommandHierarchyColors.popupBackground());
        this.commandSuggestions.setAllowSuggestions(true);
        this.commandSuggestions.updateCommandInfo();
        this.commandEdit.setCommandSuggestions(this.commandSuggestions);
        if (previous != null) this.commandEdit.copyStateFrom(previous);
        // 初始化输出框
        this.updatePreviousOutput(flag);
    }

    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        this.init(pMinecraft, pWidth, pHeight);
    }

    public void returnFromConfig() {
        // 在 setScreen 完成控件重建与默认焦点导航之后恢复真实选区。
        // 清除跨界面的鼠标拖动状态，防止“返回”按钮的点击被延续成文本拖选。
        this.setDragging(false);
        this.clearFocus();
        this.commandEdit.resetSelectionModifier();
        this.setFocused(this.commandEdit);
        this.commandEdit.setCursorPosition(this.configReturnCursor);
        this.commandEdit.setHighlightPos(this.configReturnAnchor);
        this.commandSuggestions.hide();
    }

    protected void updatePreviousOutput(boolean pTrackOutput) {
        this.previousEdit.setValue(pTrackOutput ? this.getCommandBlock().getLastOutput().getString() : "-");
    }

    protected void onDone() {
        if (!this.doneButton.active || this.minecraft.getConnection() == null) return;
        BaseCommandBlock basecommandblock = this.getCommandBlock();
        this.populateAndSendPacket(basecommandblock);

        this.minecraft.setScreen((Screen)null);
    }

    protected abstract void populateAndSendPacket(BaseCommandBlock pCommandBlock);

    private void onEdited(String p_97689_) {
        if (this.commandSuggestions != null) this.commandSuggestions.updateCommandInfo();
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

    public boolean mouseScrolled(double pMouseX, double pMouseY, double horizontalDelta, double pDelta) {
        return this.commandSuggestions.mouseScrolled(pDelta) || super.mouseScrolled(pMouseX, pMouseY, horizontalDelta, pDelta);
    }

    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (!this.commandEdit.isMouseOver(pMouseX, pMouseY)) {
            // Screen 默认点击空白不会清空焦点，需要显式解除命令框的焦点归属。
            this.commandSuggestions.hide();
            if (this.getFocused() == this.commandEdit) this.setFocused(null);
            this.commandEdit.setFocused(false);
            this.setDragging(false);
            // 仍交给父类处理按钮和输出框；点击空白则保持无焦点。
            return super.mouseClicked(pMouseX, pMouseY, pButton);
        }
        return this.commandSuggestions.mouseClicked(pMouseX, pMouseY, pButton) || super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        // 1.21.1 的 Screen.render 会绘制背景并执行模糊，必须先于自定义控件。
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
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

        // 命令建议
        this.commandSuggestions.render(pGuiGraphics, pMouseX, pMouseY);
    }
}
