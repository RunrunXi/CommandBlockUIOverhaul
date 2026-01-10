package me.cyanhana.commandblockuioverhaul.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(EditBox.class)
public interface EditBoxAccessor {

    @Accessor
    int getFrame();

    @Accessor
    boolean getIsEditable();

    @Accessor
    int getTextColor();

    @Accessor
    int getTextColorUneditable();

}
