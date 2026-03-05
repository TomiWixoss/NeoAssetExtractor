package com.neoassetextractor.client;

import com.neoassetextractor.core.ExtractionContext;
import com.neoassetextractor.core.ExtractionResult;
import com.neoassetextractor.extractor.block.BlockExtractor;
import com.neoassetextractor.extractor.entity.EntityExtractor;
import com.neoassetextractor.extractor.item.ItemExtractor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Xử lý sự kiện nhấn phím để extract assets (Forge 1.12.2)
 */
public class ClientEventHandler {
    
    private static final ItemExtractor itemExtractor = new ItemExtractor();
    private static final BlockExtractor blockExtractor = new BlockExtractor();
    private static final EntityExtractor entityExtractor = new EntityExtractor();
    
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        
        // Kiểm tra phím extract được nhấn
        if (KeyBindings.EXTRACT_KEY.isPressed()) {
            if (mc.player == null || mc.world == null) return;
            
            // Thử extract theo thứ tự: Entity -> Block -> Item
            if (tryExtractEntity(mc.player)) return;
            if (tryExtractBlock(mc.player)) return;
            tryExtractItem(mc.player);
        }
        
        // Phím L: Extract creative tab (khi mở creative inventory)
        if (KeyBindings.EXTRACT_TAB_KEY.isPressed()) {
            if (mc.player == null || mc.world == null) return;
            tryExtractCreativeTab(mc);
        }
        
        // Phím J: Extract Blockbench variants
        if (KeyBindings.EXTRACT_BLOCKBENCH_KEY.isPressed()) {
            if (mc.player == null || mc.world == null) return;
            
            RayTraceResult hitResult = mc.player.rayTrace(5.0D, 0.0F);
            if (hitResult == null || hitResult.typeOfHit != RayTraceResult.Type.BLOCK) {
                mc.player.sendMessage(new TextComponentString("\u00a7cHãy nhìn vào block để extract variants!"));
                return;
            }
            
            BlockPos pos = hitResult.getBlockPos();
            IBlockState state = mc.world.getBlockState(pos);
            ResourceLocation blockId = state.getBlock().getRegistryName();
            
            if (blockId != null) {
                mc.player.sendMessage(new TextComponentString("\u00a7eĐang extract block: " + blockId));
                
                ExtractionContext context = new ExtractionContext(mc.getResourceManager(), blockId);
                context.setBlockState(state);
                context.setWorld(mc.world);
                context.setBlockPos(pos);
                
                ExtractionResult result = blockExtractor.extract(context);
                sendResult(mc.player, result, context);
            }
        }
    }
    
    /**
     * Thử extract toàn bộ items trong creative tab hiện tại
     */
    private void tryExtractCreativeTab(Minecraft mc) {
        if (!(mc.currentScreen instanceof GuiContainerCreative)) {
            if (mc.player != null) {
                mc.player.sendMessage(new TextComponentString(
                    "\u00a7cBạn phải mở Creative Inventory để extract tab!"));
            }
            return;
        }
        
        GuiContainerCreative creativeScreen = (GuiContainerCreative) mc.currentScreen;
        
        if (mc.player != null) {
            mc.player.sendMessage(new TextComponentString(
                "\u00a7eĐang trích xuất items từ creative inventory..."));
        }
        
        List<ItemStack> items = new ArrayList<ItemStack>();
        
        // Get items from creative inventory slots
        for (Slot slot : creativeScreen.inventorySlots.inventorySlots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                boolean isDuplicate = false;
                for (ItemStack existing : items) {
                    if (ItemStack.areItemStacksEqual(stack, existing)) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (!isDuplicate) {
                    items.add(stack.copy());
                }
            }
        }
        
        if (items.isEmpty()) {
            if (mc.player != null) {
                mc.player.sendMessage(new TextComponentString(
                    "\u00a7cKhông tìm thấy item nào trong tab hiện tại!"));
            }
            return;
        }
        
        int successCount = 0;
        int totalCount = items.size();
        
        for (ItemStack itemStack : items) {
            if (itemStack.isEmpty()) continue;
            
            ResourceLocation itemId = itemStack.getItem().getRegistryName();
            if (itemId == null) continue;
            
            ExtractionContext context = new ExtractionContext(mc.getResourceManager(), itemId);
            ExtractionResult result = itemExtractor.extract(context);
            if (result.isSuccess()) {
                successCount++;
            }
        }
        
        if (mc.player != null) {
            mc.player.sendMessage(new TextComponentString(
                "\u00a7a✓ Đã trích xuất \u00a7f" + successCount + "/" + totalCount + 
                " \u00a7aitems từ creative tab"));
        }
    }
    
    private boolean tryExtractEntity(EntityPlayer player) {
        Vec3d eyePos = player.getPositionEyes(1.0F);
        Vec3d lookVec = player.getLook(1.0F);
        Vec3d endPos = eyePos.add(lookVec.x * 5.0D, lookVec.y * 5.0D, lookVec.z * 5.0D);
        
        AxisAlignedBB searchBox = player.getEntityBoundingBox()
            .expand(lookVec.x * 5.0D, lookVec.y * 5.0D, lookVec.z * 5.0D)
            .grow(1.0D, 1.0D, 1.0D);
        
        Entity closestEntity = null;
        double closestDistance = 5.0D;
        
        for (Entity entity : player.world.getEntitiesWithinAABBExcludingEntity(player, searchBox)) {
            if (!entity.canBeCollidedWith()) continue;
            
            float collisionSize = entity.getCollisionBorderSize();
            AxisAlignedBB entityBox = entity.getEntityBoundingBox().grow(collisionSize);
            RayTraceResult hit = entityBox.calculateIntercept(eyePos, endPos);
            
            if (hit != null) {
                double distance = eyePos.distanceTo(hit.hitVec);
                if (distance < closestDistance) {
                    closestEntity = entity;
                    closestDistance = distance;
                }
            }
        }
        
        if (closestEntity == null) return false;
        
        ResourceLocation entityId = net.minecraft.entity.EntityList.getKey(closestEntity);
        if (entityId == null) return false;
        
        ExtractionContext context = new ExtractionContext(
            Minecraft.getMinecraft().getResourceManager(), entityId);
        context.setAssetType("entities");
        
        ExtractionResult result = entityExtractor.extract(context);
        sendResult(player, result, context);
        return true;
    }
    
    private boolean tryExtractBlock(EntityPlayer player) {
        RayTraceResult hitResult = player.rayTrace(5.0D, 0.0F);
        if (hitResult == null || hitResult.typeOfHit != RayTraceResult.Type.BLOCK) return false;
        
        BlockPos pos = hitResult.getBlockPos();
        IBlockState state = player.world.getBlockState(pos);
        ResourceLocation blockId = state.getBlock().getRegistryName();
        if (blockId == null) return false;
        
        ExtractionContext context = new ExtractionContext(
            Minecraft.getMinecraft().getResourceManager(), blockId);
        context.setAssetType("blocks");
        context.setBlockState(state);
        context.setWorld(player.world);
        context.setBlockPos(pos);
        
        ExtractionResult result = blockExtractor.extract(context);
        sendResult(player, result, context);
        return true;
    }
    
    private boolean tryExtractItem(EntityPlayer player) {
        ItemStack heldItem = player.getHeldItemMainhand();
        if (heldItem.isEmpty()) {
            player.sendMessage(new TextComponentString(
                "\u00a7cKhông tìm thấy gì để extract! Hãy cầm item hoặc nhìn vào block/entity."));
            return false;
        }
        
        ResourceLocation itemId = heldItem.getItem().getRegistryName();
        if (itemId == null) return false;
        
        ExtractionContext context = new ExtractionContext(
            Minecraft.getMinecraft().getResourceManager(), itemId);
        context.setAssetType("items");
        
        ExtractionResult result = itemExtractor.extract(context);
        sendResult(player, result, context);
        return true;
    }
    
    private void sendResult(EntityPlayer player, ExtractionResult result, ExtractionContext context) {
        ResourceLocation id = context.getResourceId();
        
        if (result.isSuccess()) {
            player.sendMessage(new TextComponentString(
                "\u00a7a✓ Đã trích xuất: \u00a7f" + id + "\n" +
                "\u00a77" + result.getSummary()
            ));
        } else {
            player.sendMessage(new TextComponentString(
                "\u00a7c✗ Không thể trích xuất: " + id + "\n" +
                "\u00a77" + String.join("\n", result.getErrors())
            ));
        }
    }
}
