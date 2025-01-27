package me.white.doublebooks.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.widget.PageTurnWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;
import java.util.function.Function;

@Mixin(BookScreen.class)
public abstract class BookScreenMixin extends Screen {
    // crucial for us vanilla book texture details
    @Unique private final int TEXTURE_LEFT_PADDING = 20;
    @Unique private final int TEXTURE_RIGHT_PADDING = 90;
    @Unique private final int TEXTURE_WIDTH = 146;
    @Unique private List<OrderedText> cachedPageLeft = List.of();
    @Unique private List<OrderedText> cachedPageRight = List.of();
    @Shadow private BookScreen.Contents contents;
    @Shadow private int pageIndex;
    @Shadow private int cachedPageIndex;
    @Shadow private Text pageIndexText;
    @Shadow private PageTurnWidget nextPageButton;
    @Shadow private PageTurnWidget previousPageButton;

    @Shadow protected abstract int getPageCount();

    @Shadow protected abstract void updatePageButtons();

    @Shadow public abstract Style getTextStyleAt(double x, double y);

    protected BookScreenMixin(Text title) {
        super(title);
    }

    @Redirect(method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIII)V"))
    private void drawBook(DrawContext instance, Function<Identifier, RenderLayer> renderLayers, Identifier sprite, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        // using the full texture width (256) instead of the weird magic width (192) so mirrored image doesn't get cut off
        instance.drawTexture(renderLayers, sprite, this.width / 2 - TEXTURE_LEFT_PADDING, y, u, v, 256, height, textureWidth, textureHeight);
        instance.drawTexture(renderLayers, sprite, this.width / 2 - 256 + TEXTURE_LEFT_PADDING, y, u, v, 256, height, -textureWidth, textureHeight);
    }

    @ModifyArgs(method = "addPageButtons()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/PageTurnWidget;<init>(IIZLnet/minecraft/client/gui/widget/ButtonWidget$PressAction;Z)V", ordinal = 0))
    private void addPageTurnNext(Args args) {
        // lots of questionable maths to shift the buttons exactly the way vanilla does, but respecting our double page placement
        int start = width / 2 - 256 + TEXTURE_RIGHT_PADDING;
        args.set(0, start + TEXTURE_WIDTH + 116);
    }

    @ModifyArgs(method = "addPageButtons()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/PageTurnWidget;<init>(IIZLnet/minecraft/client/gui/widget/ButtonWidget$PressAction;Z)V", ordinal = 1))
    private void addPageTurnPrevious(Args args) {
        int start = width / 2 - 256 + TEXTURE_RIGHT_PADDING;
        args.set(0, start + 43);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), cancellable = true)
    private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        super.render(context, mouseX, mouseY, delta);
        ci.cancel();
        if (pageIndex != cachedPageIndex) {
            StringVisitable visitable = contents.getPage(pageIndex);
            cachedPageLeft = textRenderer.wrapLines(visitable, 114);
            if (pageIndex + 1 < getPageCount()) {
                visitable = contents.getPage(pageIndex + 1);
                cachedPageRight = textRenderer.wrapLines(visitable, 114);
            } else {
                cachedPageRight = null;
            }
        }
        cachedPageIndex = pageIndex;
        Text pageIndexText = Text.translatable("book.pageIndicator", pageIndex + 1, Math.max(getPageCount(), 1));
        int textWidth = textRenderer.getWidth(pageIndexText);
        int start = width / 2 - 256 + TEXTURE_RIGHT_PADDING;
        context.drawText(textRenderer, pageIndexText, start - textWidth + 192 - 44, 18, 0, false);
        if (pageIndex + 1 < getPageCount()) {
            pageIndexText = Text.translatable("book.pageIndicator", pageIndex + 2, Math.max(getPageCount(), 1));
            textWidth = textRenderer.getWidth(pageIndexText);
            context.drawText(textRenderer, pageIndexText, start + TEXTURE_WIDTH - textWidth + 192 - 44, 18, 0, false);
        }
        int lines = Math.min(128 / textRenderer.fontHeight, cachedPageLeft.size());
        for (int line = 0; line < lines; ++line) {
            OrderedText orderedText = cachedPageLeft.get(line);
            context.drawText(textRenderer, orderedText, start + 36, 32 + line * textRenderer.fontHeight, 0, false);
        }
        if (pageIndex + 1 < getPageCount()) {
            lines = Math.min(128 / textRenderer.fontHeight, cachedPageRight.size());
            for (int line = 0; line < lines; ++line) {
                OrderedText orderedText = cachedPageRight.get(line);
                context.drawText(textRenderer, orderedText, start + TEXTURE_WIDTH + 36, 32 + line * textRenderer.fontHeight, 0, false);
            }
        }
        Style style = getTextStyleAt(mouseX, mouseY);
        if (style != null) {
            context.drawHoverEvent(textRenderer, style, mouseX, mouseY);
        }
    }

    @Inject(method = "goToPreviousPage()V", at = @At("HEAD"), cancellable = true)
    private void goToPreviousPage(CallbackInfo ci) {
        if (pageIndex > 0) {
            pageIndex -= 2;
        }
        updatePageButtons();
        ci.cancel();
    }

    @Inject(method = "goToNextPage()V", at = @At("HEAD"), cancellable = true)
    private void goToNextPage(CallbackInfo ci) {
        if (pageIndex < getPageCount() - 2) {
            pageIndex += 2;
        }
        updatePageButtons();
        ci.cancel();
    }

    @Inject(method = "updatePageButtons()V", at = @At("HEAD"), cancellable = true)
    private void updatePageButtonsMixin(CallbackInfo ci) {
        nextPageButton.visible = pageIndex < getPageCount() - 2;
        previousPageButton.visible = pageIndex > 0;
        ci.cancel();
    }

    @Inject(method = "getTextStyleAt(DD)Lnet/minecraft/text/Style;", at = @At("HEAD"), cancellable = true)
    private void getTextStyleAtMixin(double x, double y, CallbackInfoReturnable<Style> cir) {
        int start = width / 2 - 256 + TEXTURE_RIGHT_PADDING;
        List<OrderedText> page = cachedPageLeft;
        if (x >= (double)width / 2) {
            page = cachedPageRight;
            start += TEXTURE_WIDTH;
        }
        if (page == null) {
            cir.setReturnValue(null);
            return;
        }
        int normX = MathHelper.floor(x - start - 36);
        int normY = MathHelper.floor(y - 32);
        if (normX < 0 || normY < 0) {
            cir.setReturnValue(null);
            return;
        }
        int lines = Math.min(128 / textRenderer.fontHeight, page.size());
        if (normX <= 114 && normY < (client.textRenderer.fontHeight + 1) * lines) {
            int line = normY / client.textRenderer.fontHeight;
            if (line < page.size()) {
                OrderedText orderedText = page.get(line);
                cir.setReturnValue(client.textRenderer.getTextHandler().getStyleAt(orderedText, normX));
                return;
            }
        }
        cir.setReturnValue(null);
    }

    @ModifyExpressionValue(method = "setPage(I)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(III)I"))
    private int setPage(int original) {
        return original - original % 2;
    }
}
