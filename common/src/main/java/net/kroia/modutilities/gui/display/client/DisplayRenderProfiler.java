package net.kroia.modutilities.gui.display.client;

import net.kroia.modutilities.ModUtilitiesMod;
import net.minecraft.core.BlockPos;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class DisplayRenderProfiler {

    public enum Category {
        TOTAL, GUI_RENDER, TEXTURE_TRANSFER, QUAD_RENDER
    }

    private static boolean enabled = false;
    private static final int REPORT_INTERVAL = 100;

    private static final Map<BlockPos, GroupStats> groups = new HashMap<>();

    private static class CategoryStats {
        long startNanos;
        long totalNanos;
        long minNanos = Long.MAX_VALUE;
        long maxNanos = Long.MIN_VALUE;
        int sampleCount;

        void record(long elapsed) {
            totalNanos += elapsed;
            if (elapsed < minNanos) minNanos = elapsed;
            if (elapsed > maxNanos) maxNanos = elapsed;
            sampleCount++;
        }

        void reset() {
            totalNanos = 0;
            minNanos = Long.MAX_VALUE;
            maxNanos = Long.MIN_VALUE;
            sampleCount = 0;
        }
    }

    private static class GroupStats {
        final EnumMap<Category, CategoryStats> categories = new EnumMap<>(Category.class);
        int texWidth;
        int texHeight;

        GroupStats() {
            for (Category c : Category.values()) {
                categories.put(c, new CategoryStats());
            }
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        if (DisplayRenderProfiler.enabled && !enabled) {
            flushAll();
        }
        DisplayRenderProfiler.enabled = enabled;
        if (!enabled) {
            groups.clear();
        }
    }

    private static void flushAll() {
        for (var entry : groups.entrySet()) {
            GroupStats group = entry.getValue();
            if (group.categories.get(Category.TOTAL).sampleCount > 0) {
                logAndReset(entry.getKey(), group);
            }
        }
    }

    public static void begin(BlockPos controllerPos, Category category) {
        if (!enabled) return;
        GroupStats group = groups.computeIfAbsent(controllerPos, k -> new GroupStats());
        group.categories.get(category).startNanos = System.nanoTime();
    }

    public static void end(BlockPos controllerPos, Category category) {
        if (!enabled) return;
        GroupStats group = groups.get(controllerPos);
        if (group == null) return;
        CategoryStats stats = group.categories.get(category);
        long elapsed = System.nanoTime() - stats.startNanos;
        stats.record(elapsed);

        if (category == Category.GUI_RENDER
                && stats.sampleCount >= REPORT_INTERVAL) {
            logAndReset(controllerPos, group);
        }
    }

    public static void storeTextureSize(BlockPos controllerPos, int texWidth, int texHeight) {
        if (!enabled) return;
        GroupStats group = groups.computeIfAbsent(controllerPos, k -> new GroupStats());
        group.texWidth = texWidth;
        group.texHeight = texHeight;
    }

    public static void cleanup(BlockPos controllerPos) {
        groups.remove(controllerPos);
    }

    private static void logAndReset(BlockPos pos, GroupStats group) {
        int guiSamples = group.categories.get(Category.GUI_RENDER).sampleCount;
        int totalSamples = group.categories.get(Category.TOTAL).sampleCount;
        ModUtilitiesMod.LOGGER.info(
                "[DisplayProfiler] group@({},{},{}) {}x{} samples={}/{} | total: {} | gui: {} | transfer: {} | quad: {}",
                pos.getX(), pos.getY(), pos.getZ(),
                group.texWidth, group.texHeight,
                guiSamples, totalSamples,
                formatStats(group.categories.get(Category.TOTAL)),
                formatStats(group.categories.get(Category.GUI_RENDER)),
                formatStats(group.categories.get(Category.TEXTURE_TRANSFER)),
                formatStats(group.categories.get(Category.QUAD_RENDER))
        );
        for (CategoryStats stats : group.categories.values()) {
            stats.reset();
        }
    }

    private static String formatStats(CategoryStats stats) {
        if (stats.sampleCount == 0) return "n/a";
        double minUs = stats.minNanos / 1000.0;
        double avgUs = (stats.totalNanos / (double) stats.sampleCount) / 1000.0;
        double maxUs = stats.maxNanos / 1000.0;
        return String.format("%.1f/%.1f/%.1f us (min/avg/max)", minUs, avgUs, maxUs);
    }
}
