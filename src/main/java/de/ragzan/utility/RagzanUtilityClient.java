package de.ragzan.utility;

import de.ragzan.utility.config.ModConfig;
import de.ragzan.utility.gui.MainMenuScreen;
import de.ragzan.utility.render.EspRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class RagzanUtilityClient implements ClientModInitializer {
    private static KeyBinding openMenuKey;
    private static boolean iKeyWasDown;

    @Override
    public void onInitializeClient() {
        ModConfig.INSTANCE.save();

        KeyBinding.Category category = KeyBinding.Category.create(
                Identifier.of("ragzan_utility", "main")
        );
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ragzan_utility.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                category
        ));

        EspRenderer.initialize();
        ClientLogic.initialize();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Primary path: registered Minecraft keybinding (can be changed in Controls).
            boolean openRequested = false;
            while (openMenuKey.wasPressed()) {
                openRequested = true;
            }

            // Fallback path: ensure the default I key still works if a keybinding config
            // or another mod prevents the registered binding from being consumed.
            boolean iDown = GLFW.glfwGetKey(
                    client.getWindow().getHandle(), GLFW.GLFW_KEY_I
            ) == GLFW.GLFW_PRESS;
            if (iDown && !iKeyWasDown) {
                openRequested = true;
            }
            iKeyWasDown = iDown;

            if (openRequested) {
                if (client.currentScreen instanceof MainMenuScreen) {
                    client.setScreen(null);
                } else {
                    client.setScreen(new MainMenuScreen());
                }
            }

            ClientLogic.tick(client);
        });
    }
}
