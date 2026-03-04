package com.neoassetextractor.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.neoassetextractor.extractor.BlockExtractor;
import com.neoassetextractor.extractor.EntityExtractor;
import com.neoassetextractor.extractor.ItemExtractor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ExtractCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("extract")
                .then(Commands.literal("item")
                    .executes(ExtractCommand::extractItem))
                .then(Commands.literal("block")
                    .executes(ExtractCommand::extractBlock))
                .then(Commands.literal("entity")
                    .executes(ExtractCommand::extractEntity))
                .then(Commands.literal("all")
                    .executes(ExtractCommand::extractAll))
        );
    }

    private static int extractItem(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof Player player)) {
            context.getSource().sendFailure(Component.literal("§cChỉ người chơi mới có thể sử dụng lệnh này!"));
            return 0;
        }

        return ItemExtractor.extract(player, context.getSource());
    }

    private static int extractBlock(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof Player player)) {
            context.getSource().sendFailure(Component.literal("§cChỉ người chơi mới có thể sử dụng lệnh này!"));
            return 0;
        }

        return BlockExtractor.extract(player, context.getSource());
    }

    private static int extractEntity(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof Player player)) {
            context.getSource().sendFailure(Component.literal("§cChỉ người chơi mới có thể sử dụng lệnh này!"));
            return 0;
        }

        return EntityExtractor.extract(player, context.getSource());
    }

    private static int extractAll(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof Player player)) {
            context.getSource().sendFailure(Component.literal("§cChỉ người chơi mới có thể sử dụng lệnh này!"));
            return 0;
        }

        // Priority: Entity -> Block -> Item
        int result = EntityExtractor.extract(player, context.getSource());
        if (result == 1) return 1;

        result = BlockExtractor.extract(player, context.getSource());
        if (result == 1) return 1;

        return ItemExtractor.extract(player, context.getSource());
    }
}
