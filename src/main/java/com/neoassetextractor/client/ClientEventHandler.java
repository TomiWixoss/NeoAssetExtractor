package com.neoassetextractor.client;

import com.neoassetextractor.core.ExtractionContext;
import com.neoassetextractor.core.ExtractionResult;
import com.neoassetextractor.extractor.block.BlockExtractor;
import com.neoassetextractor.extractor.entity.EntityExtractor;
import com.neoassetextractor.extractor.item.ItemExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Xử lý sự kiện nhấn phím để extract assets
 */
@EventBusSubscriber(modid = "neoassetextractor", value = Dist.CLIENT)
public class ClientEventHandler {
    
    private static final ItemExtractor itemExtractor = new ItemExtractor();
    private static final BlockExtractor blockExtractor = new BlockExtractor();
    private static final EntityExtractor entityExtractor = new EntityExtractor();
    
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        
        // Kiểm tra phím extract được nhấn (chỉ khi không có GUI)
        if (KeyBindings.EXTRACT_KEY.consumeClick()) {
            if (mc.player == null || mc.level == null) return;
            
            // Thử extract theo thứ tự: Entity -> Block -> Item
            if (tryExtractEntity(mc.player)) return;
            if (tryExtractBlock(mc.player)) return;
            tryExtractItem(mc.player);
        }
    }
    
    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Post event) {
        Minecraft mc = Minecraft.getInstance();
        
        // Kiểm tra phím extract tab (trong creative inventory)
        if (KeyBindings.EXTRACT_TAB_KEY.consumeClick()) {
            if (mc.player == null || mc.level == null) return;
            tryExtractCreativeTab(mc);
        }
    }
    
    /**
     * Thử extract toàn bộ items trong creative tab hiện tại
     */
    private static void tryExtractCreativeTab(Minecraft mc) {
        // Kiểm tra xem có đang mở creative inventory không
        if (!(mc.screen instanceof CreativeModeInventoryScreen creativeScreen)) {
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal("§cBạn phải mở Creative Inventory để extract tab!"),
                    true
                );
            }
            return;
        }
        
        // Lấy tab hiện tại qua reflection
        CreativeModeTab selectedTab = null;
        try {
            var menuField = CreativeModeInventoryScreen.class.getDeclaredField("menu");
            menuField.setAccessible(true);
            var menu = menuField.get(creativeScreen);
            
            var selectedTabField = menu.getClass().getDeclaredField("selectedTab");
            selectedTabField.setAccessible(true);
            selectedTab = (CreativeModeTab) selectedTabField.get(menu);
        } catch (Exception e) {
            // Fallback: extract tất cả items trong tất cả tabs
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal("§eKhông lấy được tab hiện tại, sẽ extract tất cả items..."),
                    false
                );
            }
            extractAllItems(mc);
            return;
        }
        
        if (selectedTab == null) {
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal("§cKhông tìm thấy tab nào được chọn!"),
                    true
                );
            }
            return;
        }
        
        String tabName = selectedTab.getDisplayName().getString();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                Component.literal("§eĐang trích xuất tab: §f" + tabName + "§e..."),
                false
            );
        }
        
        // Lấy tất cả items thuộc tab này
        List<ItemStack> items = new ArrayList<>();
        for (var item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (!stack.isEmpty() && selectedTab.contains(stack)) {
                items.add(stack);
            }
        }
        
        if (items.isEmpty()) {
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal("§cTab này không có item nào!"),
                    false
                );
            }
            return;
        }
        
        // Extract từng item
        int successCount = 0;
        int totalCount = items.size();
        
        for (ItemStack itemStack : items) {
            if (itemStack.isEmpty()) continue;
            
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
            ExtractionContext context = new ExtractionContext(mc.getResourceManager(), itemId);
            
            ExtractionResult result = itemExtractor.extract(context);
            if (result.isSuccess()) {
                successCount++;
            }
        }
        
        final int finalSuccess = successCount;
        final int finalTotal = totalCount;
        if (mc.player != null) {
            mc.player.displayClientMessage(
                Component.literal(
                    "§a✓ Đã trích xuất §f" + finalSuccess + "/" + finalTotal + " §aitems từ tab: §f" + tabName
                ),
                false
            );
        }
    }
    
    /**
     * Extract tất cả items (fallback khi không lấy được tab)
     */
    private static void extractAllItems(Minecraft mc) {
        List<ItemStack> items = new ArrayList<>();
        for (var item : BuiltInRegistries.ITEM) {
            items.add(new ItemStack(item));
        }
        
        int successCount = 0;
        int totalCount = items.size();
        
        for (ItemStack itemStack : items) {
            if (itemStack.isEmpty()) continue;
            
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
            ExtractionContext context = new ExtractionContext(mc.getResourceManager(), itemId);
            
            ExtractionResult result = itemExtractor.extract(context);
            if (result.isSuccess()) {
                successCount++;
            }
        }
        
        final int finalSuccess = successCount;
        final int finalTotal = totalCount;
        if (mc.player != null) {
            mc.player.displayClientMessage(
                Component.literal(
                    "§a✓ Đã trích xuất §f" + finalSuccess + "/" + finalTotal + " §aitems"
                ),
                false
            );
        }
    }
    
    /**
     * Thử extract entity mà player đang nhìn
     */
    private static boolean tryExtractEntity(Player player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(5.0D));
        AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(5.0D)).inflate(1.0D);
        
        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
            player.level(), player, eyePos, endPos, searchBox, 
            entity -> !entity.isSpectator() && entity.isPickable()
        );
        
        if (hitResult == null) return false;
        
        Entity entity = hitResult.getEntity();
        ResourceLocation entityId = entity.getType().builtInRegistryHolder().key().location();
        
        ExtractionContext context = new ExtractionContext(
            Minecraft.getInstance().getResourceManager(), entityId);
        
        ExtractionResult result = entityExtractor.extract(context);
        sendResult(player, result, entityId);
        return true;
    }
    
    /**
     * Thử extract block mà player đang nhìn
     */
    private static boolean tryExtractBlock(Player player) {
        HitResult hitResult = player.pick(5.0D, 0.0F, false);
        if (hitResult.getType() != HitResult.Type.BLOCK) return false;
        
        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        ResourceLocation blockId = state.getBlock().builtInRegistryHolder().key().location();
        
        ExtractionContext context = new ExtractionContext(
            Minecraft.getInstance().getResourceManager(), blockId);
        context.setBlockState(state);
        context.setLevel(player.level());
        context.setBlockPos(pos);
        
        ExtractionResult result = blockExtractor.extract(context);
        sendResult(player, result, blockId);
        return true;
    }
    
    /**
     * Thử extract item mà player đang cầm
     */
    private static boolean tryExtractItem(Player player) {
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            player.displayClientMessage(
                Component.literal("§cKhông tìm thấy gì để extract! Hãy cầm item hoặc nhìn vào block/entity."),
                true
            );
            return false;
        }
        
        ResourceLocation itemId = heldItem.getItem().builtInRegistryHolder().key().location();
        ExtractionContext context = new ExtractionContext(
            Minecraft.getInstance().getResourceManager(), itemId);
        
        ExtractionResult result = itemExtractor.extract(context);
        sendResult(player, result, itemId);
        return true;
    }
    
    /**
     * Gửi kết quả extraction cho player
     */
    private static void sendResult(Player player, ExtractionResult result, ResourceLocation id) {
        if (result.isSuccess()) {
            player.displayClientMessage(
                Component.literal(
                    "§a✓ Đã trích xuất: §f" + id + "\n" +
                    "§7" + result.getSummary()
                ),
                false
            );
        } else {
            player.displayClientMessage(
                Component.literal(
                    "§c✗ Không thể trích xuất: " + id + "\n" +
                    "§7" + String.join("\n", result.getErrors())
                ),
                false
            );
        }
    }
}
