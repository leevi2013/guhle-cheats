package de.ragzan.utility.gui;

import de.ragzan.utility.config.ModConfig;
import net.minecraft.block.Block;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Filterable, paginated block picker. The list rows are drawn manually so controls can never overlap them. */
public final class BlockSelectionScreen extends Screen {
    private final Screen parent;
    private final boolean forBlockEsp;
    private final ModConfig c = ModConfig.INSTANCE;
    private final List<Block> allBlocks;

    private TextFieldWidget searchField;
    private ButtonWidget pageLabelButton;
    private int page = 0;
    private int panelX;
    private int panelWidth;
    private int listY;
    private int rowHeight;
    private int rowsPerPage;
    private int navY;

    public BlockSelectionScreen(Screen parent, boolean forBlockEsp) {
        super(Text.literal("Block-Auswahl"));
        this.parent = parent;
        this.forBlockEsp = forBlockEsp;
        // This registry contains every block available in this Minecraft instance, including modded blocks.
        this.allBlocks = Registries.BLOCK.stream()
                .sorted(Comparator.comparing(b -> Registries.BLOCK.getId(b).toString()))
                .toList();
    }

    private Set<String> selection() {
        return forBlockEsp ? c.blockEspBlocks : c.stashBlocks;
    }

    private List<Block> filteredBlocks() {
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) return allBlocks;
        return allBlocks.stream()
                .filter(block -> Registries.BLOCK.getId(block).toString().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    @Override
    protected void init() {
        clearChildren();
        panelWidth = Math.max(160, Math.min(430, width - 24));
        panelX = (width - panelWidth) / 2;
        listY = 72;
        rowHeight = 21;
        navY = height - 53;
        rowsPerPage = Math.max(1, Math.min(12, (navY - listY - 33) / rowHeight));

        searchField = new TextFieldWidget(textRenderer, panelX, 44, panelWidth, 20, Text.literal("Block suchen"));
        searchField.setMaxLength(100);
        searchField.setPlaceholder(Text.literal("z. B. chest, diamond_ore, modid:block"));
        searchField.setTextPredicate(value -> value.length() <= 100);
        searchField.setChangedListener(value -> page = 0);
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("◀"), b -> {
            page = Math.max(0, page - 1);
        }).dimensions(panelX, navY, 48, 22).build());
        pageLabelButton = ButtonWidget.builder(Text.literal("Seite"), b -> {})
                .dimensions(panelX + 54, navY, panelWidth - 108, 22).build();
        addDrawableChild(pageLabelButton);
        addDrawableChild(ButtonWidget.builder(Text.literal("▶"), b -> {
            int pages = pageCount();
            page = Math.min(pages - 1, page + 1);
        }).dimensions(panelX + panelWidth - 48, navY, 48, 22).build());

        int actionY = navY - 27;
        int actionHalf = (panelWidth - 10) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Treffer wählen"), b -> {
            for (Block block : filteredBlocks()) selection().add(Registries.BLOCK.getId(block).toString());
            c.save();
        }).dimensions(panelX, actionY, actionHalf, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Auswahl löschen"), b -> {
            selection().clear();
            c.save();
        }).dimensions(panelX + actionHalf + 10, actionY, actionHalf, 22).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Zurück"), b -> client.setScreen(parent))
                .dimensions(panelX, height - 27, panelWidth, 20).build());
    }

    private int pageCount() {
        return Math.max(1, (filteredBlocks().size() + rowsPerPage - 1) / rowsPerPage);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        if (click.buttonInfo().button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;

        List<Block> filtered = filteredBlocks();
        int pages = pageCount();
        page = Math.max(0, Math.min(page, pages - 1));
        int row = (int) ((click.y() - listY) / rowHeight);
        if (click.x() >= panelX && click.x() <= panelX + panelWidth && click.y() >= listY) {
            int idx = page * rowsPerPage + row;
            if (row >= 0 && row < rowsPerPage && idx >= 0 && idx < filtered.size()) {
                String id = Registries.BLOCK.getId(filtered.get(idx)).toString();
                if (!selection().remove(id)) selection().add(id);
                c.save();
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(forBlockEsp ? "BLOCK ESP – Block-Auswahl" : "STASH ESP – Lagerblock-Auswahl"),
                width / 2, 16, 0xFF7CEBFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Anklicken = Auswahl umschalten · alle registrierten Blöcke verfügbar"),
                width / 2, 30, 0xFFCCCCCC);

        List<Block> filtered = filteredBlocks();
        int pages = pageCount();
        page = Math.max(0, Math.min(page, pages - 1));
        int from = page * rowsPerPage;
        int to = Math.min(filtered.size(), from + rowsPerPage);

        context.drawTextWithShadow(textRenderer, Text.literal("Suche"), panelX, 34, 0xFFCCCCCC);
        for (int idx = from; idx < to; idx++) {
            Block block = filtered.get(idx);
            String id = Registries.BLOCK.getId(block).toString();
            int localRow = idx - from;
            int y = listY + localRow * rowHeight;
            boolean hovered = mouseX >= panelX && mouseX <= panelX + panelWidth
                    && mouseY >= y && mouseY < y + rowHeight - 2;
            boolean selected = selection().contains(id);
            int background = hovered ? 0xFF2C4056 : 0xC016202D;
            context.fill(panelX, y, panelX + panelWidth, y + rowHeight - 2, background);
            context.drawTextWithShadow(textRenderer, Text.literal(selected ? "✓" : "□"), panelX + 7, y + 5,
                    selected ? 0xFF77EE91 : 0xFFB9C1CC);
            String display = textRenderer.trimToWidth(id, panelWidth - 35);
            context.drawTextWithShadow(textRenderer, Text.literal(display), panelX + 25, y + 5,
                    selected ? 0xFFFFFFFF : 0xFFD7DEE8);
        }
        if (filtered.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Keine passenden Blöcke gefunden"),
                    width / 2, listY + 12, 0xFFFFC857);
        }
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("" + filtered.size() + " Treffer · " + selection().size() + " ausgewählt"),
                width / 2, navY - 45, 0xFFCCCCCC);
        // Update the label without rebuilding controls, so typing keeps the search cursor focused.
        pageLabelButton.setMessage(Text.literal("Seite " + (page + 1) + "/" + pages + " · " + filtered.size() + " Blöcke"));
        super.render(context, mouseX, mouseY, delta);
    }
}
