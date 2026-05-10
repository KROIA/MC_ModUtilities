package net.kroia.modutilities.testing;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.CompletableFuture;

public class TestCommandRegistration {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                String commandRoot, String modName, boolean isSlave) {
        if (!TestRegistry.ENABLE_TESTS) return;

        dispatcher.register(
            Commands.literal(commandRoot)
                .then(Commands.literal("test")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        new TestRunner(modName, isSlave, player.getServer())
                            .runAll(player);
                        return 1;
                    })
                    .then(Commands.literal("list")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            new TestRunner(modName, isSlave, player.getServer())
                                .listCategories(player);
                            return 1;
                        })
                    )
                    .then(Commands.argument("category", StringArgumentType.string())
                        .suggests((context, builder) -> suggestCategories(builder, isSlave))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String cat = StringArgumentType.getString(context, "category");
                            new TestRunner(modName, isSlave, player.getServer())
                                .runCategory(player, cat);
                            return 1;
                        })
                    )
                )
        );
    }

    private static CompletableFuture<Suggestions> suggestCategories(
            SuggestionsBuilder builder, boolean isSlave) {
        for (String cat : TestRegistry.getAvailableCategories(isSlave)) {
            builder.suggest(cat);
        }
        return CompletableFuture.completedFuture(builder.build());
    }
}
