package de.ragzan.utility.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public final class ModConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ragzan-utility.properties");
    private static final int[] COLORS = {0xFF45E4FF, 0xFFFF4BD8, 0xFF7CFF5B, 0xFFFFC857, 0xFFFFFFFF, 0xFFFF5555, 0xFF9B70FF, 0xFF5B8CFF};
    private static final String[] COLOR_NAMES = {"Cyan", "Pink", "Grün", "Gold", "Weiß", "Rot", "Lila", "Blau"};

    public boolean playerEsp = false;
    public boolean blockEsp = false;
    public boolean stashEsp = false;
    public boolean autoReconnect = false;
    public boolean freecamMining = false;
    public int playerColorIndex = 0;
    public int blockColorIndex = 2;
    public int stashColorIndex = 3;
    public float playerThickness = 2.0f;
    public float blockThickness = 2.0f;
    public float stashThickness = 2.5f;
    public int playerRange = 64;
    public int scanRadius = 12;
    public int reconnectY = -25;
    public int freecamKeyCode = 0; // 0 = none; configured in the settings page
    public int stashAlertSeconds = 5;
    public final Set<String> blockEspBlocks = new LinkedHashSet<>();
    public final Set<String> stashBlocks = new LinkedHashSet<>();

    public static final ModConfig INSTANCE = new ModConfig();

    private ModConfig() {
        for (Block block : Registries.BLOCK) {
            String id = Registries.BLOCK.getId(block).toString();
            String path = Registries.BLOCK.getId(block).getPath();
            if (isStorageLike(path)) stashBlocks.add(id);
        }
        load();
    }

    public int playerColor() { return COLORS[Math.floorMod(playerColorIndex, COLORS.length)]; }
    public int blockColor() { return COLORS[Math.floorMod(blockColorIndex, COLORS.length)]; }
    public int stashColor() { return COLORS[Math.floorMod(stashColorIndex, COLORS.length)]; }
    public String playerColorName() { return COLOR_NAMES[Math.floorMod(playerColorIndex, COLOR_NAMES.length)]; }
    public String blockColorName() { return COLOR_NAMES[Math.floorMod(blockColorIndex, COLOR_NAMES.length)]; }
    public String stashColorName() { return COLOR_NAMES[Math.floorMod(stashColorIndex, COLOR_NAMES.length)]; }
    public static int[] colors() { return COLORS.clone(); }
    public static String[] colorNames() { return COLOR_NAMES.clone(); }

    public void save() {
        Properties p = new Properties();
        p.setProperty("playerEsp", Boolean.toString(playerEsp));
        p.setProperty("blockEsp", Boolean.toString(blockEsp));
        p.setProperty("stashEsp", Boolean.toString(stashEsp));
        p.setProperty("autoReconnect", Boolean.toString(autoReconnect));
        p.setProperty("freecamMining", Boolean.toString(freecamMining));
        p.setProperty("playerColorIndex", Integer.toString(playerColorIndex));
        p.setProperty("blockColorIndex", Integer.toString(blockColorIndex));
        p.setProperty("stashColorIndex", Integer.toString(stashColorIndex));
        p.setProperty("playerThickness", Float.toString(playerThickness));
        p.setProperty("blockThickness", Float.toString(blockThickness));
        p.setProperty("stashThickness", Float.toString(stashThickness));
        p.setProperty("playerRange", Integer.toString(playerRange));
        p.setProperty("scanRadius", Integer.toString(scanRadius));
        p.setProperty("reconnectY", Integer.toString(reconnectY));
        p.setProperty("freecamKeyCode", Integer.toString(freecamKeyCode));
        p.setProperty("stashAlertSeconds", Integer.toString(stashAlertSeconds));
        p.setProperty("blockEspBlocks", String.join(",", blockEspBlocks));
        p.setProperty("stashBlocks", String.join(",", stashBlocks));
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) { p.store(out, "Ragzan Utility client configuration"); }
        } catch (IOException e) {
            System.err.println("[Ragzan Utility] Config konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    private void load() {
        if (!Files.exists(FILE)) { save(); return; }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) { p.load(in); } catch (IOException e) { return; }
        playerEsp = bool(p, "playerEsp", playerEsp);
        blockEsp = bool(p, "blockEsp", blockEsp);
        stashEsp = bool(p, "stashEsp", stashEsp);
        autoReconnect = bool(p, "autoReconnect", autoReconnect);
        freecamMining = bool(p, "freecamMining", freecamMining);
        playerColorIndex = integer(p, "playerColorIndex", playerColorIndex, 0, COLORS.length - 1);
        blockColorIndex = integer(p, "blockColorIndex", blockColorIndex, 0, COLORS.length - 1);
        stashColorIndex = integer(p, "stashColorIndex", stashColorIndex, 0, COLORS.length - 1);
        playerThickness = decimal(p, "playerThickness", playerThickness, 1.0f, 8.0f);
        blockThickness = decimal(p, "blockThickness", blockThickness, 1.0f, 8.0f);
        stashThickness = decimal(p, "stashThickness", stashThickness, 1.0f, 8.0f);
        playerRange = integer(p, "playerRange", playerRange, 8, 256);
        scanRadius = integer(p, "scanRadius", scanRadius, 4, 24);
        reconnectY = integer(p, "reconnectY", reconnectY, -64, 320);
        freecamKeyCode = integer(p, "freecamKeyCode", freecamKeyCode, 0, 348);
        stashAlertSeconds = integer(p, "stashAlertSeconds", stashAlertSeconds, 1, 30);
        blockEspBlocks.clear();
        readSet(p.getProperty("blockEspBlocks", ""), blockEspBlocks);
        stashBlocks.clear();
        readSet(p.getProperty("stashBlocks", ""), stashBlocks);
        // Preserve blocks from newer mod versions and only accept ids registered in this instance.
        blockEspBlocks.removeIf(id -> Identifier.tryParse(id) == null || !Registries.BLOCK.containsId(Identifier.tryParse(id)));
        stashBlocks.removeIf(id -> Identifier.tryParse(id) == null || !Registries.BLOCK.containsId(Identifier.tryParse(id)));
    }

    private static void readSet(String value, Set<String> target) {
        if (value == null || value.isBlank()) return;
        for (String s : value.split(",")) if (!s.isBlank()) target.add(s.trim());
    }
    private static boolean bool(Properties p, String k, boolean d) { return Boolean.parseBoolean(p.getProperty(k, Boolean.toString(d))); }
    private static int integer(Properties p, String k, int d, int min, int max) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(p.getProperty(k, Integer.toString(d))))); } catch (Exception e) { return d; }
    }
    private static float decimal(Properties p, String k, float d, float min, float max) {
        try { return Math.max(min, Math.min(max, Float.parseFloat(p.getProperty(k, Float.toString(d))))); } catch (Exception e) { return d; }
    }
    private static boolean isStorageLike(String path) {
        return path.contains("chest") || path.contains("barrel") || path.contains("hopper") || path.contains("shulker_box")
                || path.equals("dispenser") || path.equals("dropper") || path.contains("furnace") || path.equals("brewing_stand")
                || path.equals("trapped_chest") || path.equals("crafter") || path.equals("decorated_pot") || path.equals("lectern");
    }
}
