package me.cyanhana.commandblockuioverhaul.mixin;

import me.cyanhana.commandblockuioverhaul.ui.screen.ModMinecartCommandBlockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BaseCommandBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BaseCommandBlock.class)
public class BaseCommandBlockMixin {
    @Redirect(method="usedBy",
            at=@At(value="INVOKE", target = "Lnet/minecraft/world/entity/player/Player;openMinecartCommandBlock(Lnet/minecraft/world/level/BaseCommandBlock;)V"))
    public void openModMinecartCommandBlock(Player instance, BaseCommandBlock commandBlock) {
        if(instance instanceof LocalPlayer){
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new ModMinecartCommandBlockScreen(commandBlock));
        }
    }
}
