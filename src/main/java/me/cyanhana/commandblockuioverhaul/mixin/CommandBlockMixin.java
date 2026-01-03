package me.cyanhana.commandblockuioverhaul.mixin;

import me.cyanhana.commandblockuioverhaul.ui.screen.ModCommandBlockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CommandBlock.class)
public class CommandBlockMixin {
    @Redirect(method = "use",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;" +
                    "openCommandBlock(Lnet/minecraft/world/level/block/entity/CommandBlockEntity;)V"))
    public void openModCommandBlock(Player instance, CommandBlockEntity pCommandBlockEntity) {
        if (instance instanceof LocalPlayer) {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.setScreen(new ModCommandBlockScreen(pCommandBlockEntity));
        } else if (instance instanceof ServerPlayer) {
            ((ServerPlayerAccessor)instance).getConnection().send(ClientboundBlockEntityDataPacket.create(pCommandBlockEntity, BlockEntity::saveWithoutMetadata));
        }
    }
}
