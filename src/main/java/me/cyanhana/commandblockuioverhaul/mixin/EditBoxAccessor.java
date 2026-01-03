package me.cyanhana.commandblockuioverhaul.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Predicate;

@Mixin(EditBox.class)
public interface EditBoxAccessor {
    @Accessor
    Font getFont();

    @Accessor
    String getSuggestion();

    @Accessor
    int getFrame();

    @Accessor
    boolean getIsEditable();

    @Accessor
    int getTextColor();

    @Accessor
    int getTextColorUneditable();

    @Accessor
    boolean getBordered();

    @Accessor
    int getHighlightPos();

    @Accessor
    Predicate<String> getFilter();

    @Accessor
    void setCursorPos(int pCursorPos);

    @Invoker
    int invokeGetCursorPos(int pDelta);

    @Invoker
    void invokeOnValueChange(String pNewText);
}
