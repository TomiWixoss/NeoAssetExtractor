package com.neoassetextractor.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.neoassetextractor.core.ExtractionContext;
import com.neoassetextractor.core.ExtractionResult;
import com.neoassetextractor.extractor.block.BlockExtractor;
import com.neoassetextractor.extractor.entity.EntityExtractor;
import com.neoassetextractor.extractor.item.ItemExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CreativeModeTab;
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
                .then(Commands.literal("tab")
                    .executes(ExtractCommand::extractCurrentTab))
                .then(Commands.literal("all")
                    .executes(ExtractCommand::extractAll))
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
    
    private static int extractCurrentTab(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof Player player)) {
            context.getSource().sendFailure(Component.literal("§cChỉ người chơi mới có thể sử dụng lệnh này!"));
            return 0;
        }
        
        Minecraft mc = Minecraft.getInstance();
        
        // Check if creative inventory is open
        if (!(mc.screen instanceof CreativeModeInventoryScreen creativeScreen)) {
            context.getSource().sendFailure(Component.literal("§cBạn phải mở Creative Inventory!"));
            return 0;
        }
        
        // Get current selected tab
        CreativeModeTab selectedTab = creativeScreen.getSelectedTab();
        if (selectedTab == null) {
            context.getSource().sendFailure(Component.literal("§cKhông tìm thấy tab nào được chọn!"));
            return 0;
        }
        
        context.getSource().sendSuccess(() -> Component.literal(
            "§eĐang trích xuất tab: §f" + selectedTab.getDisplayName().getString() + "§e..."
        ), false);
        
        // Get all items in the tab
        var items = new java.util.ArrayList<ItemStack>();
        selectedTab.buildContents(new CreativeModeTab.ItemDisplayParameters(
            mc.level.enabledFeatures(),
            false,
            mc.level.registryAccess()
        )).forEach(items::add);
        
        if (items.isEmpty()) {
            context.getSource().sendFailure(Component.literal("§cTab này không có item nào!"));
            return 0;
        }
        
        // Extract each item
        int successCount = 0;
        int totalCount = items.size();
        
        for (ItemStack itemStack : items) {
            if (itemStack.isEmpty()) continue;
            
            ResourceLocation itemId = itemStack.getItem().builtInRegistryHolder().key().location();
            ExtractionContext extractionContext = new ExtractionContext(
                mc.getResourceManager(), itemId);
            
            ExtractionResult result = itemExtractor.extract(extractionContext);
            if (result.isSuccess()) {
                successCount++;
            }
        }
        
        final int finalSuccess = successCount;
        final int finalTotal = totalCount;
        context.getSource().sendSuccess(() -> Component.literal(
            "§a✓ Đã trích xuất §f" + finalSuccess + "/" + finalTotal + " §aitems từ tab: §f" + 
            selectedTab.getDisplayName().getString()
        ), false);
        
        return 1;
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
}
