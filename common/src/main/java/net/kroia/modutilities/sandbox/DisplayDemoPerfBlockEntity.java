package net.kroia.modutilities.sandbox;

import net.kroia.modutilities.gui.display.DisplayConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Perf-tuned variant of {@link DisplayDemoBlockEntity} that exercises the
 * Task #94 render optimizations: {@code renderInterval} (GUI redraw throttle)
 * and {@code maxRenderDistance} (LOD cutoff). All demo logic (sine/cosine plot,
 * speed slider, pause button, editable title, save/load, sync) is inherited
 * unchanged &mdash; only {@link #getDisplayConfig()} is overridden.
 * <p>
 * Placed next to a regular {@link DisplayDemoBlockEntity}, the throttling and
 * distance cutoff are visible as stuttering redraws and a hard freeze past the
 * distance threshold, respectively.
 */
public class DisplayDemoPerfBlockEntity extends DisplayDemoBlockEntity {

    public DisplayDemoPerfBlockEntity(BlockPos pos, BlockState blockState) {
        super(SandboxRegistration.DISPLAY_DEMO_PERF_BLOCK_ENTITY.get(), pos, blockState);
    }

    @Override
    public DisplayConfig getDisplayConfig() {
        // 256x256 matches the no-arg DisplayConfig.fullBlock() default so pixel
        // density is identical to the baseline demo block for A/B comparison.
        // renderInterval=10 -> GUI redrawn every 10 ticks (2 Hz).
        // maxRenderDistance=24 -> renderer skips draws past 24 blocks.
        return DisplayConfig.fullBlock(256, 256, 10, 24);
    }
}
