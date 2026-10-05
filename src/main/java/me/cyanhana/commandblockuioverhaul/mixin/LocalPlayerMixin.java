package me.cyanhana.commandblockuioverhaul.mixin;

import me.cyanhana.commandblockuioverhaul.ui.screen.ModCommandBlockScreen;
import me.cyanhana.commandblockuioverhaul.ui.screen.ModMinecartCommandBlockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Inject(method = "openCommandBlock", at = @At("HEAD"), cancellable = true)
    private void openModCommandBlock(CommandBlockEntity commandBlock, CallbackInfo ci) {
        Minecraft.getInstance().setScreen(new ModCommandBlockScreen(commandBlock));
        ci.cancel();
    }

    @Inject(method = "openMinecartCommandBlock", at = @At("HEAD"), cancellable = true)
    private void openModMinecartCommandBlock(BaseCommandBlock commandBlock, CallbackInfo ci) {
        Minecraft.getInstance().setScreen(new ModMinecartCommandBlockScreen(commandBlock));
        ci.cancel();
    }
}
