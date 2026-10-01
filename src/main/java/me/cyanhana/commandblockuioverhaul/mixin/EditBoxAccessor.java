package me.cyanhana.commandblockuioverhaul.mixin;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EditBox.class)
public interface EditBoxAccessor {

    @Accessor
    long getFocusedTime();

    @Accessor
    boolean getIsEditable();

    @Accessor
    int getTextColor();

    @Accessor
    int getTextColorUneditable();

}
