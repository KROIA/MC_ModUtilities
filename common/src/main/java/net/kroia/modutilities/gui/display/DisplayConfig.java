package net.kroia.modutilities.gui.display;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

public record DisplayConfig(
    int virtualWidth,
    int virtualHeight,
    int renderScale,
    int maxTextureDim,
    float faceOffset,
    ShapeProvider shapeProvider,
    int renderInterval,
    int maxRenderDistance
) {
    private static final VoxelShape INSET_SHAPE = Block.box(0.2, 0.2, 0.2, 15.8, 15.8, 15.8);
    private static final VoxelShape PANEL_NS = Block.box(0, 0, 7, 16, 16, 9);
    private static final VoxelShape PANEL_EW = Block.box(7, 0, 0, 9, 16, 16);

    public static DisplayConfig fullBlock() {
        return new DisplayConfig(256, 256, 2, 4096, 0.005f,
            facing -> INSET_SHAPE, 1, 0);
    }

    public static DisplayConfig fullBlock(int virtualWidth, int virtualHeight) {
        return new DisplayConfig(virtualWidth, virtualHeight, 2, 4096, 0.005f,
            facing -> INSET_SHAPE, 1, 0);
    }

    public static DisplayConfig fullBlock(int virtualWidth, int virtualHeight,
                                          int renderInterval, int maxRenderDistance) {
        return new DisplayConfig(virtualWidth, virtualHeight, 2, 4096, 0.005f,
            facing -> INSET_SHAPE, renderInterval, maxRenderDistance);
    }

    private static final float PANEL_FACE_OFFSET = -7.0f / 16.0f + 0.005f;

    public static DisplayConfig flatPanel() {
        return new DisplayConfig(256, 256, 2, 4096, PANEL_FACE_OFFSET,
            facing -> switch (facing) {
                case NORTH, SOUTH -> PANEL_NS;
                case EAST, WEST -> PANEL_EW;
                default -> PANEL_NS;
            }, 1, 0);
    }

    public static DisplayConfig flatPanel(int virtualWidth, int virtualHeight) {
        return new DisplayConfig(virtualWidth, virtualHeight, 2, 4096, PANEL_FACE_OFFSET,
            facing -> switch (facing) {
                case NORTH, SOUTH -> PANEL_NS;
                case EAST, WEST -> PANEL_EW;
                default -> PANEL_NS;
            }, 1, 0);
    }

    public static DisplayConfig flatPanel(int virtualWidth, int virtualHeight,
                                          int renderInterval, int maxRenderDistance) {
        return new DisplayConfig(virtualWidth, virtualHeight, 2, 4096, PANEL_FACE_OFFSET,
            facing -> switch (facing) {
                case NORTH, SOUTH -> PANEL_NS;
                case EAST, WEST -> PANEL_EW;
                default -> PANEL_NS;
            }, renderInterval, maxRenderDistance);
    }
}
