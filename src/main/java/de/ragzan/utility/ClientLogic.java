package de.ragzan.utility;

import de.ragzan.utility.config.ModConfig;
import de.ragzan.utility.mixin.MinecraftClientAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ClientLogic {
    private static final ModConfig C = ModConfig.INSTANCE;
    private static final List<BlockPos> BLOCK_ESP_POSITIONS = new ArrayList<>();
    private static final List<BlockPos> STASH_POSITIONS = new ArrayList<>();
    private static int scanCooldown = 0;
    private static int reconnectCooldown = 0;
    private static int reconnectWait = 0;
    private static boolean reconnectWasTriggered = false;
    private static ServerInfo queuedServer = null;
    private static long hudUntilTick = 0;
    private static String hudMessage = "";
    private static String lastAlertKey = "";
    private static long lastAlertTick = -10000;
    private static ArmorStandEntity freecamEntity;
    private static boolean freecamActive = false;
    private static boolean waitingForFreecamKey = false;

    private ClientLogic() {}

    public static void initialize() {}

    public static List<BlockPos> blockEspPositions() { return BLOCK_ESP_POSITIONS; }
    public static List<BlockPos> stashPositions() { return STASH_POSITIONS; }
    public static boolean isFreecamActive() { return freecamActive; }
    public static boolean waitingForFreecamKey() { return waitingForFreecamKey; }
    public static void setWaitingForFreecamKey(boolean waiting) { waitingForFreecamKey = waiting; }
    public static String hudMessage() { return hudUntilTick > currentTick() ? hudMessage : ""; }

    private static long currentTick() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world == null ? 0 : client.world.getTime();
    }

    public static void notifyHud(String message, int seconds) {
        hudMessage = message;
        hudUntilTick = currentTick() + Math.max(1, seconds) * 20L;
    }

    public static void tick(MinecraftClient client) {
        // The pending reconnect countdown must keep running after the old world disconnects.
        tickAutoReconnect(client);
        if (client.player == null || client.world == null) {
            BLOCK_ESP_POSITIONS.clear();
            STASH_POSITIONS.clear();
            if (freecamActive) stopFreecam(client);
            return;
        }

        if (waitingForFreecamKey) {
            // Input is caught by the custom screen; don't toggle other systems while waiting.
        } else if (C.freecamMining && C.freecamKeyCode > 0) {
            boolean down = GLFW.glfwGetKey(client.getWindow().getHandle(), C.freecamKeyCode) == GLFW.GLFW_PRESS;
            if (down && !freecamKeyWasDown) toggleFreecam(client);
            freecamKeyWasDown = down;
        } else {
            freecamKeyWasDown = false;
        }

        tickFreecam(client);

        if (scanCooldown-- <= 0) {
            scanCooldown = 10;
            scanBlocks(client.world, client.player.getBlockPos());
            checkStashClusters(client.world, client.player.getBlockPos());
        }
    }

    private static boolean freecamKeyWasDown = false;

    public static void toggleFreecam(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (freecamActive) {
            stopFreecam(client);
            return;
        }
        freecamEntity = new ArmorStandEntity(EntityType.ARMOR_STAND, client.world);
        freecamEntity.setId(-762341);
        freecamEntity.setInvisible(true);
        freecamEntity.setNoGravity(true);
        freecamEntity.noClip = true;
        freecamEntity.setPosition(client.player.getX(), client.player.getY(), client.player.getZ());
        freecamEntity.setYaw(client.player.getYaw());
        freecamEntity.setPitch(client.player.getPitch());
        client.world.addEntity(freecamEntity);
        client.setCameraEntity(freecamEntity);
        freecamActive = true;
        notifyHud("Freecam Mining: AN", 2);
    }

    public static void stopFreecam(MinecraftClient client) {
        if (freecamEntity != null && client.world != null) {
            client.world.removeEntity(freecamEntity.getId(), Entity.RemovalReason.DISCARDED);
        }
        freecamEntity = null;
        freecamActive = false;
        if (client.player != null) client.setCameraEntity(client.player);
        notifyHud("Freecam Mining: AUS", 2);
    }

    private static void tickFreecam(MinecraftClient client) {
        if (!freecamActive || freecamEntity == null || client.player == null || client.world == null) return;
        if (!C.freecamMining) { stopFreecam(client); return; }
        // Mouse look still updates the real player rotation; mirror it onto the freecam.
        freecamEntity.setYaw(client.player.getYaw());
        freecamEntity.setPitch(client.player.getPitch());
        float forward = (client.options.forwardKey.isPressed() ? 1 : 0) - (client.options.backKey.isPressed() ? 1 : 0);
        float strafe = (client.options.rightKey.isPressed() ? 1 : 0) - (client.options.leftKey.isPressed() ? 1 : 0);
        float vertical = (client.options.jumpKey.isPressed() ? 1 : 0) - (client.options.sneakKey.isPressed() ? 1 : 0);
        double speed = client.options.sprintKey.isPressed() ? 1.25 : 0.55;
        float yaw = (float) Math.toRadians(freecamEntity.getYaw());
        double dx = (-Math.sin(yaw) * forward + Math.cos(yaw) * strafe) * speed;
        double dz = ( Math.cos(yaw) * forward + Math.sin(yaw) * strafe) * speed;
        freecamEntity.setPosition(freecamEntity.getX() + dx, freecamEntity.getY() + vertical * speed, freecamEntity.getZ() + dz);
        freecamEntity.setNoGravity(true);
        freecamEntity.noClip = true;

        // Auto-break only asks vanilla's interaction manager to mine the selected block.
        // The server still validates player reach and permissions; this is not a server-side bypass.
        if (client.crosshairTarget instanceof BlockHitResult hit
                && client.player.squaredDistanceTo(hit.getBlockPos().getX() + 0.5, hit.getBlockPos().getY() + 0.5, hit.getBlockPos().getZ() + 0.5) <= 36.0) {
            var interactionManager = ((MinecraftClientAccessor) client).ragzan$getInteractionManager();
            if (interactionManager != null) interactionManager.updateBlockBreakingProgress(hit.getBlockPos(), hit.getSide());
        }
    }

    private static void tickAutoReconnect(MinecraftClient client) {
        if (reconnectCooldown > 0) reconnectCooldown--;
        if (reconnectWait > 0) {
            reconnectWait--;
            if (reconnectWait == 0 && queuedServer != null) {
                ServerInfo info = queuedServer;
                queuedServer = null;
                try {
                    ConnectScreen.connect(new TitleScreen(), client, ServerAddress.parse(info.address), info, false, null);
                } catch (Exception e) {
                    notifyHud("Auto-Reconnect fehlgeschlagen", 4);
                }
            }
            return;
        }
        if (!C.autoReconnect || client.player == null || client.world == null || client.isInSingleplayer()) return;
        if (client.player.getY() > C.reconnectY + 1) reconnectWasTriggered = false;
        if (client.player.getY() <= C.reconnectY && !reconnectWasTriggered && reconnectCooldown <= 0) {
            ServerInfo info = client.getCurrentServerEntry();
            if (info == null) return;
            reconnectWasTriggered = true;
            reconnectCooldown = 600;
            queuedServer = info;
            reconnectWait = 12;
            notifyHud("Y=" + C.reconnectY + " erreicht – verbinde neu …", 3);
            client.disconnect(new TitleScreen(), false);
        }
    }

    private static void scanBlocks(ClientWorld world, BlockPos center) {
        BLOCK_ESP_POSITIONS.clear();
        STASH_POSITIONS.clear();
        if (!C.blockEsp && !C.stashEsp) return;
        int r = C.scanRadius;
        int blockMinY = Math.max(world.getBottomY(), center.getY() - r);
        int blockMaxY = Math.min(world.getTopYInclusive(), center.getY() + Math.max(6, r / 2));
        int stashMinY = world.getBottomY();
        int stashMaxY = Math.min(world.getTopYInclusive(), center.getY() + Math.max(24, r));
        Set<String> enabledBlockIds = C.blockEspBlocks;
        Set<String> stashIds = C.stashBlocks;
        int scanMinY = C.stashEsp ? stashMinY : blockMinY;
        int scanMaxY = C.stashEsp ? stashMaxY : blockMaxY;
        for (int x = center.getX() - r; x <= center.getX() + r; x++) {
            for (int z = center.getZ() - r; z <= center.getZ() + r; z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                // Block ESP stays close to the player's height; stash ESP scans down to the
                // world's floor so hidden containers far below the surface can be detected.
                for (int y = scanMinY; y <= scanMaxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (state.isAir()) continue;
                    String id = Registries.BLOCK.getId(state.getBlock()).toString();
                    if (C.blockEsp && y >= blockMinY && y <= blockMaxY && enabledBlockIds.contains(id)) {
                        BLOCK_ESP_POSITIONS.add(pos.toImmutable());
                    }
                    if (C.stashEsp && y >= stashMinY && y <= stashMaxY && stashIds.contains(id)) {
                        STASH_POSITIONS.add(pos.toImmutable());
                    }
                }
            }
        }
    }

    private static void checkStashClusters(ClientWorld world, BlockPos center) {
        if (!C.stashEsp || STASH_POSITIONS.isEmpty()) return;
        Set<BlockPos> all = new HashSet<>(STASH_POSITIONS);
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> bestCluster = null;

        for (BlockPos start : STASH_POSITIONS) {
            if (visited.contains(start)) continue;
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            List<BlockPos> cluster = new ArrayList<>();
            queue.add(start);
            visited.add(start);
            while (!queue.isEmpty()) {
                BlockPos p = queue.removeFirst();
                cluster.add(p);
                for (Direction d : Direction.values()) {
                    BlockPos n = p.offset(d);
                    if (all.contains(n) && visited.add(n)) queue.addLast(n);
                }
            }

            // Trigger at four or more connected storage blocks, or a directly adjacent pair.
            if (cluster.size() >= 2 && (bestCluster == null || cluster.size() > bestCluster.size())) {
                bestCluster = cluster;
            }
        }

        if (bestCluster == null || bestCluster.size() < 2) return;
        BlockPos first = bestCluster.stream().min(BlockPos::compareTo).orElse(bestCluster.get(0));
        String key = first.toShortString() + ":" + bestCluster.size();
        long now = world.getTime();
        // Do not repeatedly announce the same stash every scan tick.
        if (!key.equals(lastAlertKey) && now - lastAlertTick >= 40) {
            lastAlertKey = key;
            lastAlertTick = now;
            String msg = bestCluster.size() >= 4
                    ? "Stash entdeckt: " + bestCluster.size() + " verbundene Lagerblöcke"
                    : "Stash entdeckt: benachbarte Lagerblöcke";
            notifyHud(msg, C.stashAlertSeconds);
        }
    }
}
