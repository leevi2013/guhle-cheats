package de.ragzan.utility.gui;

import de.ragzan.utility.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class MainMenuScreen extends Screen {
    private final ModConfig c = ModConfig.INSTANCE;
    private final List<String> modules = List.of("Player ESP", "Block ESP", "Stash ESP", "Auto-Reconnect", "Freecam Mining");
    private final int rowWidth = 270;
    private int rowX;
    private int rowY;
    private int rowHeight = 30;

    public MainMenuScreen() { super(Text.literal("Ragzan Utility")); }

    @Override
    protected void init() {
        rowX = (width - rowWidth) / 2;
        rowY = height / 2 - 72;
        clearChildren();
        for (int i = 0; i < modules.size(); i++) {
            final int index = i;
            int y = rowY + i * rowHeight;
            addDrawableChild(ButtonWidget.builder(Text.literal(label(index)), b -> {
                toggle(index);
                b.setMessage(Text.literal(label(index)));
            }).dimensions(rowX, y, rowWidth, 24).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Schließen"), b -> close()).dimensions(rowX, rowY + modules.size() * rowHeight + 5, rowWidth, 22).build());
    }

    private String label(int index) {
        boolean enabled = switch (index) {
            case 0 -> c.playerEsp;
            case 1 -> c.blockEsp;
            case 2 -> c.stashEsp;
            case 3 -> c.autoReconnect;
            default -> c.freecamMining;
        };
        return modules.get(index) + "  [" + (enabled ? "AN" : "AUS") + "]   •   Rechtsklick: Einstellungen";
    }

    private void toggle(int index) {
        switch (index) {
            case 0 -> c.playerEsp = !c.playerEsp;
            case 1 -> c.blockEsp = !c.blockEsp;
            case 2 -> c.stashEsp = !c.stashEsp;
            case 3 -> c.autoReconnect = !c.autoReconnect;
            case 4 -> c.freecamMining = !c.freecamMining;
        }
        c.save();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.buttonInfo().button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            double mx = click.x(), my = click.y();
            for (int i = 0; i < modules.size(); i++) {
                int y = rowY + i * rowHeight;
                if (mx >= rowX && mx <= rowX + rowWidth && my >= y && my <= y + 24) {
                    client.setScreen(new ModuleSettingsScreen(this, i));
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("RAGZAN UTILITY"), width / 2, rowY - 35, 0xFF7CEBFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Linksklick = Modul an/aus  |  Rechtsklick = Optionen"), width / 2, rowY - 19, 0xFFCCCCCC);
        super.render(context, mouseX, mouseY, delta);
    }
}
