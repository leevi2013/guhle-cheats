package de.ragzan.utility.gui;

import de.ragzan.utility.ClientLogic;
import de.ragzan.utility.config.ModConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class ModuleSettingsScreen extends Screen {
    private final Screen parent;
    private final int module;
    private final ModConfig c = ModConfig.INSTANCE;
    private boolean binding = false;

    public ModuleSettingsScreen(Screen parent, int module) {
        super(Text.literal("Modul-Einstellungen"));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        clearChildren();
        int x = width / 2 - 145;
        int y = height / 2 - 88;
        if (module <= 2) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Farbe: " + colorName()), b -> {
                cycleColor(); c.save(); b.setMessage(Text.literal("Farbe: " + colorName()));
            }).dimensions(x, y, 290, 22).build());
            y += 27;
            addDrawableChild(ButtonWidget.builder(Text.literal("Umrandungsdicke: " + String.format("%.1f", thickness())), b -> {}).dimensions(x, y, 290, 22).build());
            y += 27;
            addDrawableChild(ButtonWidget.builder(Text.literal("Dicke −"), b -> { changeThickness(-0.5f); c.save(); init(); }).dimensions(x, y, 140, 22).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Dicke +"), b -> { changeThickness(0.5f); c.save(); init(); }).dimensions(x + 150, y, 140, 22).build());
            y += 27;
            if (module == 0) {
                addDrawableChild(ButtonWidget.builder(Text.literal("ESP-Reichweite: " + c.playerRange + " Blöcke"), b -> {
                    c.playerRange = c.playerRange >= 256 ? 16 : c.playerRange + 16; c.save(); b.setMessage(Text.literal("ESP-Reichweite: " + c.playerRange + " Blöcke"));
                }).dimensions(x, y, 290, 22).build());
            } else {
                addDrawableChild(ButtonWidget.builder(Text.literal("Blöcke auswählen / Liste öffnen"), b -> client.setScreen(new BlockSelectionScreen(this, module == 1))).dimensions(x, y, 290, 22).build());
                y += 27;
                addDrawableChild(ButtonWidget.builder(Text.literal("Scan-Radius: " + c.scanRadius), b -> {
                    c.scanRadius = c.scanRadius >= 24 ? 6 : c.scanRadius + 2; c.save(); b.setMessage(Text.literal("Scan-Radius: " + c.scanRadius));
                }).dimensions(x, y, 290, 22).build());
            }
        } else if (module == 3) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Auslösen bei Y ≤ " + c.reconnectY), b -> {})
                    .dimensions(x, y, 290, 22).build());
            y += 27;
            addDrawableChild(ButtonWidget.builder(Text.literal("Y − 5"), b -> {
                c.reconnectY = Math.max(-64, c.reconnectY - 5); c.save(); init();
            }).dimensions(x, y, 140, 22).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Y + 5"), b -> {
                c.reconnectY = Math.min(320, c.reconnectY + 5); c.save(); init();
            }).dimensions(x + 150, y, 140, 22).build());
            y += 28;
            addDrawableChild(ButtonWidget.builder(Text.literal("Hinweis: nur Multiplayer-Server"), b -> {}).dimensions(x, y, 290, 22).build());
        } else {
            addDrawableChild(ButtonWidget.builder(Text.literal("Freecam-Hotkey: " + keyName()), b -> {
                binding = true;
                ClientLogic.setWaitingForFreecamKey(true);
                b.setMessage(Text.literal("Drücke jetzt eine Taste … (ESC = Abbrechen)"));
            }).dimensions(x, y, 290, 22).build());
            y += 28;
            addDrawableChild(ButtonWidget.builder(Text.literal("Freecam jetzt " + (ClientLogic.isFreecamActive() ? "ausschalten" : "einschalten")), b -> {
                ClientLogic.toggleFreecam(client);
                init();
            }).dimensions(x, y, 290, 22).build());
            y += 28;
            addDrawableChild(ButtonWidget.builder(Text.literal("WASD bewegen · Leertaste hoch · Shift runter"), b -> {}).dimensions(x, y, 290, 22).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Zurück"), b -> {
            ClientLogic.setWaitingForFreecamKey(false);
            client.setScreen(parent);
        }).dimensions(x, height / 2 + 112, 290, 22).build());
    }

    private String colorName() {
        return switch (module) { case 0 -> c.playerColorName(); case 1 -> c.blockColorName(); default -> c.stashColorName(); };
    }
    private float thickness() { return switch (module) { case 0 -> c.playerThickness; case 1 -> c.blockThickness; default -> c.stashThickness; }; }
    private void cycleColor() {
        switch (module) { case 0 -> c.playerColorIndex++; case 1 -> c.blockColorIndex++; default -> c.stashColorIndex++; }
    }
    private void changeThickness(float d) {
        switch (module) {
            case 0 -> c.playerThickness = Math.max(1, Math.min(8, c.playerThickness + d));
            case 1 -> c.blockThickness = Math.max(1, Math.min(8, c.blockThickness + d));
            default -> c.stashThickness = Math.max(1, Math.min(8, c.stashThickness + d));
        }
    }
    private String keyName() {
        if (c.freecamKeyCode == 0) return "nicht gesetzt";
        String name = GLFW.glfwGetKeyName(c.freecamKeyCode, 0);
        return name == null ? "Key " + c.freecamKeyCode : name.toUpperCase();
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (binding) {
            int code = input.getKeycode();
            if (code == GLFW.GLFW_KEY_ESCAPE) {
                binding = false;
                ClientLogic.setWaitingForFreecamKey(false);
                init();
                return true;
            }
            c.freecamKeyCode = code;
            c.save();
            binding = false;
            ClientLogic.setWaitingForFreecamKey(false);
            init();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 112, 0xFF7CEBFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Änderungen werden direkt gespeichert"), width / 2, height - 28, 0xFFAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }
}
