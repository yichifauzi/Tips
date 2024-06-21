package net.darkhax.tipsmod.impl.client;

import net.darkhax.bookshelf.api.util.RenderHelper;
import net.darkhax.tipsmod.api.TipsAPI;
import net.darkhax.tipsmod.api.resources.ITip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

public class TipRenderHandler {

    private static long initTime = System.currentTimeMillis();

    @Nullable
    private static ITip tip;

    @Nullable
    private static WeakReference<Screen> lastScreen;

    private static void setTip(ITip newTip) {

        // The empty tip is deprecated and is now replaced with null.
        if (newTip == TipsAPI.EMPTY) {
            newTip = null;
        }

        tip = newTip;
        initTime = System.currentTimeMillis();
    }

    public static void drawTip(GuiGraphics graphics, Screen parentScreen) {

        // If the tip is null, and we're on a new screen try to load a new tip.
        if (tip == null && (lastScreen == null || !lastScreen.refersTo(parentScreen))) {
            setTip(TipsAPI.getRandomTip(parentScreen));
            lastScreen = new WeakReference<>(parentScreen);
        }

        if (tip != null) {

            // Cycle to the next tip if the timer has expired.
            final long currentTime = System.currentTimeMillis();
            final int currentCycleTime = tip.getCycleTime();
            if (currentTime - initTime > currentCycleTime) {
                setTip(TipsAPI.getRandomTip(parentScreen));
            }

            // Render the tip.
            if (tip != null && tip.canDisplayOnScreen(parentScreen)) {
                final int textWidth = Mth.floor(parentScreen.width * 0.35f);
                int height = parentScreen.height - 10;
                height -= RenderHelper.renderLinesReversed(graphics, 10, height, tip.getText(), textWidth);
                height -= 3; // padding for title
                RenderHelper.renderLinesReversed(graphics, 10, height, tip.getTitle(), textWidth);
            }
        }
    }
}