package com.neoassetextractor.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.neoassetextractor.core.ExtractionContext;
import com.neoassetextractor.core.ExtractionResult;
import com.neoassetextractor.extractor.block.BlockExtractor;
import com.neoassetextractor.extractor.entity.EntityExtractor;
import com.neoassetextractor.extractor.item.ItemExtractor;
import com.neoassetextractor.extractor.structure.StructureExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ExtractCommand {
    
    private static final ItemExtractor itemExtractor = new ItemExtractor();
    private static final BlockExtractor blockExtractor = new BlockExtractor();
    private static final EntityExtractor entityExtractor = new EntityExtractor();
    
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
                .then(Commands.literal("structures")
                    .then(Commands.argument("modId", com.mojang.brigadier.arguments.StringArgumentType.string())
                        .executes(ExtractCommand::extractStructures)))
        );
    }

    private static int extractItem(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof Player player)) {
            context.getSource().sendFailure(Component.literal("§cChỉ người chơi mới có thể sử dụng lệnh này!"));
            return 0;
        }

        var heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cBạn phải cầm vật phẩm trên tay!"));
            return 0;
        }

        ResourceLocation itemId = heldItem.getItem().builtInRegistryHolder().key().location();
        ExtractionContext extractionContext = new ExtractionContext(
            Minecraft.getInstance().getResourceManager(), itemId);
        
        ExtractionResult result = itemExtractor.extract(extractionContext);
        return sendResult(context.getSource(), result, itemId);
    }

    private static int extractBlock(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof Player player)) {
            context.getSource().sendFailure(Component.literal("§cChỉ người chơi mới có thể sử dụng lệnh này!"));
            return 0;
        }

        HitResult hitResult = player.pick(5.0D, 0.0F, false);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            context.getSource().sendFailure(Component.literal("§cKhông tìm thấy khối! Hãy nhìn vào một khối."));
            return 0;
        }

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        ResourceLocation blockId = state.getBlock().builtInRegistryHolder().key().location();
        
        ExtractionContext extractionContext = new ExtractionContext(
            Minecraft.getInstance().getResourceManager(), blockId);
        extractionContext.setBlockState(state);
        extractionContext.setLevel(player.level());
        extractionContext.setBlockPos(pos);
        
        ExtractionResult result = blockExtractor.extract(extractionContext);
        return sendResult(context.getSource(), result, blockId);
    }

    private static int extractEntity(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof Player player)) {
            context.getSource().sendFailure(Component.literal("§cChỉ người chơi mới có thể sử dụng lệnh này!"));
            return 0;
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(5.0D));
        AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(5.0D)).inflate(1.0D);
        
        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
            player.level(), player, eyePos, endPos, searchBox, 
            entity -> !entity.isSpectator() && entity.isPickable()
        );
        
        if (hitResult == null) {
            context.getSource().sendFailure(Component.literal("§cKhông tìm thấy thực thể! Hãy nhìn vào một thực thể."));
            return 0;
        }

        Entity entity = hitResult.getEntity();
        ResourceLocation entityId = entity.getType().builtInRegistryHolder().key().location();
        
        ExtractionContext extractionContext = new ExtractionContext(
            Minecraft.getInstance().getResourceManager(), entityId);
        
        ExtractionResult result = entityExtractor.extract(extractionContext);
        return sendResult(context.getSource(), result, entityId);
    }

    private static int extractAll(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof Player player)) {
            context.getSource().sendFailure(Component.literal("§cChỉ người chơi mới có thể sử dụng lệnh này!"));
            return 0;
        }

        // Priority: Entity -> Block -> Item
        int result = extractEntity(context);
        if (result == 1) return 1;

        result = extractBlock(context);
        if (result == 1) return 1;

        return extractItem(context);
    }
    
    private static int sendResult(CommandSourceStack source, ExtractionResult result, ResourceLocation id) {
        if (result.isSuccess()) {
            source.sendSuccess(() -> Component.literal(
                "§a✓ Đã trích xuất: §f" + id + "\n" +
                "§7" + result.getSummary()
            ), false);
            return 1;
        } else {
            source.sendFailure(Component.literal(
                "§c✗ Không thể trích xuất: " + id + "\n" +
                "§7" + String.join("\n", result.getErrors())
            ));
            return 0;
        }
    }
    
    private static int extractStructures(CommandContext<CommandSourceStack> context) {
        String modId = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "modId");
        
        context.getSource().sendSuccess(() -> Component.literal(
            "§aĐang extract structures từ mod: §e" + modId), false);
        
        int count = StructureExtractor.extractAllStructures(modId);
        
        if (count > 0) {
            context.getSource().sendSuccess(() -> Component.literal(
                "§a✓ Đã extract §e" + count + "§a structure(s) từ §e" + modId), false);
        } else {
            context.getSource().sendFailure(Component.literal(
                "§cKhông tìm thấy structure nào từ mod: " + modId));
        }
        
        return count;
    }
}
