package de.ragzan.utility.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import de.ragzan.utility.ClientLogic;
import de.ragzan.utility.config.ModConfig;
import de.ragzan.utility.mixin.RenderLayerAccessor;
import de.ragzan.utility.mixin.RenderPipelinesAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.VertexConsumer;

public final class EspRenderer {
    private static RenderLayer X_RAY_LINES;
    private static final ModConfig C = ModConfig.INSTANCE;

    private EspRenderer() {}

    public static void initialize() {
        RenderPipeline pipeline = RenderPipeline.builder(RenderPipelinesAccessor.ragzan$linesSnippet())
                .withLocation(Identifier.of("ragzan_utility", "pipeline/xray_lines"))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.LINES)
                .build();
        pipeline = RenderPipelinesAccessor.ragzan$register(pipeline);
        X_RAY_LINES = RenderLayerAccessor.ragzan$create("ragzan_utility_xray_lines", RenderSetup.builder(pipeline)
                .expectedBufferSize(4096)
                .build());
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(EspRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || X_RAY_LINES == null) return;
        if (!C.playerEsp && !C.blockEsp && !C.stashEsp) return;

        MatrixStack matrices = context.matrices();
        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        matrices.push();
        matrices.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumerProvider consumers = context.consumers();

        if (C.playerEsp) {
            VertexConsumer lines = consumers.getBuffer(X_RAY_LINES);
            float[] color = rgba(C.playerColor());
            for (PlayerEntity target : client.world.getPlayers()) {
                if (target == client.player || target.isSpectator() || !target.isAlive()) continue;
                if (client.player.squaredDistanceTo(target) > (double) C.playerRange * C.playerRange) continue;
                Box box = target.getBoundingBox().expand(0.025);
                drawOutlinedBox(matrices, lines, box, color, C.playerThickness);
            }
            flushLines(consumers);
        }

        if (C.blockEsp) {
            VertexConsumer lines = consumers.getBuffer(X_RAY_LINES);
            float[] color = rgba(C.blockColor());
            for (BlockPos p : ClientLogic.blockEspPositions()) {
                Box box = new Box(p).expand(0.002);
                drawOutlinedBox(matrices, lines, box, color, C.blockThickness);
            }
            flushLines(consumers);
        }

        if (C.stashEsp) {
            VertexConsumer lines = consumers.getBuffer(X_RAY_LINES);
            float[] color = rgba(C.stashColor());
            for (BlockPos p : ClientLogic.stashPositions()) {
                Box box = new Box(p).expand(0.004);
                drawOutlinedBox(matrices, lines, box, color, C.stashThickness);
            }
            flushLines(consumers);
        }

        matrices.pop();
    }

    /** Draws all 12 edges explicitly so the 1.21.11 line-width vertex attribute is applied. */
    private static void drawOutlinedBox(MatrixStack matrices, VertexConsumer lines, Box box, float[] color, float width) {
        MatrixStack.Entry matrix = matrices.peek();
        double x0 = box.minX, y0 = box.minY, z0 = box.minZ;
        double x1 = box.maxX, y1 = box.maxY, z1 = box.maxZ;

        // Bottom rectangle
        line(lines, matrix, x0, y0, z0, x1, y0, z0, color, width);
        line(lines, matrix, x1, y0, z0, x1, y0, z1, color, width);
        line(lines, matrix, x1, y0, z1, x0, y0, z1, color, width);
        line(lines, matrix, x0, y0, z1, x0, y0, z0, color, width);
        // Top rectangle
        line(lines, matrix, x0, y1, z0, x1, y1, z0, color, width);
        line(lines, matrix, x1, y1, z0, x1, y1, z1, color, width);
        line(lines, matrix, x1, y1, z1, x0, y1, z1, color, width);
        line(lines, matrix, x0, y1, z1, x0, y1, z0, color, width);
        // Vertical edges
        line(lines, matrix, x0, y0, z0, x0, y1, z0, color, width);
        line(lines, matrix, x1, y0, z0, x1, y1, z0, color, width);
        line(lines, matrix, x1, y0, z1, x1, y1, z1, color, width);
        line(lines, matrix, x0, y0, z1, x0, y1, z1, color, width);
    }

    private static void line(VertexConsumer consumer, MatrixStack.Entry matrix,
                             double x0, double y0, double z0, double x1, double y1, double z1,
                             float[] color, float width) {
        float w = Math.max(1.0f, Math.min(8.0f, width));
        consumer.vertex(matrix, (float) x0, (float) y0, (float) z0)
                .color(color[0], color[1], color[2], color[3]).lineWidth(w);
        consumer.vertex(matrix, (float) x1, (float) y1, (float) z1)
                .color(color[0], color[1], color[2], color[3]).lineWidth(w);
    }

    private static void flushLines(VertexConsumerProvider consumers) {
        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw(X_RAY_LINES);
        }
    }

    private static float[] rgba(int argb) {
        return new float[] {
                ((argb >> 16) & 0xFF) / 255.0f,
                ((argb >> 8) & 0xFF) / 255.0f,
                (argb & 0xFF) / 255.0f,
                ((argb >>> 24) & 0xFF) / 255.0f
        };
    }
}
