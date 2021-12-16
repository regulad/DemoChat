package xyz.regulad.demochat.util;

import io.papermc.paper.chat.ChatRenderer;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public class ChatRendererUtil {
    /**
     * Tests if a {@link ChatRenderer} is {@link io.papermc.paper.chat.ChatRenderer.ViewerUnaware}.
     *
     * @param chatRenderer The {@link ChatRenderer} to be tested.
     * @return {@code true} if the renderer is viewer-unaware.
     */
    public static boolean isViewerUnaware(final @NotNull ChatRenderer chatRenderer) {
        try {
            Class<? extends ChatRenderer> chatRendererClass = chatRenderer.getClass();
            Field messageField = chatRendererClass.getDeclaredField("message");
            return true;
        } catch (final @NotNull NoSuchFieldException noSuchField) {
            return false; // I hate this.
        }
    }
}
