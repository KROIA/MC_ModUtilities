package net.kroia.fabric;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.kroia.modutilities.placeholder.Placeholder;
import net.kroia.modutilities.placeholder.PlaceholderRegistry;
import net.minecraft.resources.ResourceLocation;

/**
 * Patbox Text Placeholder API bridge (Fabric only). Only class-loaded when "placeholder-api" is present
 * (guarded in {@link ModUtilitiesFabric}). Identifier format: {@code <namespace>:<name>}.
 * Placeholder API has no refresh interval; values are evaluated on every request.
 */
final class PlaceholderApiBackend {
    private PlaceholderApiBackend() {}

    // ponytail: no per-refresh-interval cache; add one if a placeholder is expensive and polled every tick.
    static void attach() {
        PlaceholderRegistry.addBackend(PlaceholderApiBackend::register);
    }

    private static void register(Placeholder p) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(p.namespace(), p.name());
        if (p.isPlayerPlaceholder()) {
            Placeholders.register(id, (ctx, arg) -> ctx.hasPlayer()
                    ? PlaceholderResult.value(p.evaluate(ctx.player()))
                    : PlaceholderResult.invalid("No player!"));
        } else {
            Placeholders.register(id, (ctx, arg) -> PlaceholderResult.value(p.evaluate(null)));
        }
    }
}
