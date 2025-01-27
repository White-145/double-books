package me.white.doublebooks.mixin;

import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.screen.ingame.LecternScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LecternScreen.class)
public abstract class LecternScreenMixin extends BookScreen {
    @Redirect(method = "goToPreviousPage()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/LecternScreen;sendButtonPressPacket(I)V"))
    private void goToPreviousPage(LecternScreen instance, int id) {
        instance.sendButtonPressPacket(id);
        instance.sendButtonPressPacket(id);
    }

    @Redirect(method = "goToNextPage()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/LecternScreen;sendButtonPressPacket(I)V"))
    private void goToNextPage(LecternScreen instance, int id) {
        instance.sendButtonPressPacket(id);
        instance.sendButtonPressPacket(id);
    }
}
