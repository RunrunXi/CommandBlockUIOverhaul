package me.cyanhana.commandblockuioverhaul.mixin;

import me.cyanhana.commandblockuioverhaul.ui.screen.ModCommandBlockScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleBlockEntityData", at = @At("TAIL"))
    public void handleBlockEntityDataInject(ClientboundBlockEntityDataPacket pPacket, CallbackInfo ci) {
        BlockPos blockpos = pPacket.getPos();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.level.getBlockEntity(blockpos, pPacket.getType()).ifPresent((blockEntity) -> {

            if (blockEntity instanceof CommandBlockEntity && minecraft.screen instanceof ModCommandBlockScreen) {
                ((ModCommandBlockScreen)minecraft.screen).updateGui();
            }

        });
    }
}
