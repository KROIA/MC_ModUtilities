package net.kroia.modutilities.gui.display;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

@FunctionalInterface
public interface ShapeProvider {
    VoxelShape getShape(Direction facing);
}
