package com.neoassetextractor.command;

import com.neoassetextractor.core.ExtractionContext;
import com.neoassetextractor.core.ExtractionResult;
import com.neoassetextractor.extractor.block.BlockExtractor;
import com.neoassetextractor.extractor.entity.EntityExtractor;
import com.neoassetextractor.extractor.item.ItemExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.block.state.IBlockState;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Extract command for Forge 1.12.2
 * Registered as client-side command (no op required)
 */
public class ExtractCommand extends CommandBase {
    
    private static final ItemExtractor itemExtractor = new ItemExtractor();
    private static final BlockExtractor blockExtractor = new BlockExtractor();
    private static final EntityExtractor entityExtractor = new EntityExtractor();
    
    @Override
    public String getName() {
        return "extract";
    }
    
    @Override
    public String getUsage(ICommandSender sender) {
        return "/extract <item|block|entity|all>";
    }
    
    @Override
    public int getRequiredPermissionLevel() {
        return 0; // No permission required (client-side command)
    }
    
    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, 
                                          String[] args, BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "item", "block", "entity", "all");
        }
        return Collections.emptyList();
    }
    
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendMessage(new TextComponentString("\u00a7cChỉ người chơi mới có thể sử dụng lệnh này!"));
            return;
        }
        
        EntityPlayer player = (EntityPlayer) sender;
        
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString("\u00a7cUsage: " + getUsage(sender)));
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "item":
                extractItem(player);
                break;
            case "block":
                extractBlock(player);
                break;
            case "entity":
                extractEntity(player);
                break;
            case "all":
                extractAll(player);
                break;
            default:
                sender.sendMessage(new TextComponentString("\u00a7cUnknown sub-command: " + subCommand));
                sender.sendMessage(new TextComponentString("\u00a7cUsage: " + getUsage(sender)));
                break;
        }
    }
    
    private void extractItem(EntityPlayer player) {
        ItemStack heldItem = player.getHeldItemMainhand();
        if (heldItem.isEmpty()) {
            player.sendMessage(new TextComponentString("\u00a7cBạn phải cầm vật phẩm trên tay!"));
            return;
        }
        
        ResourceLocation itemId = heldItem.getItem().getRegistryName();
        if (itemId == null) {
            player.sendMessage(new TextComponentString("\u00a7cKhông thể xác định ID vật phẩm!"));
            return;
        }
        
        ExtractionContext context = new ExtractionContext(
            Minecraft.getMinecraft().getResourceManager(), itemId);
        
        ExtractionResult result = itemExtractor.extract(context);
        sendResult(player, result, itemId);
    }
    
    private void extractBlock(EntityPlayer player) {
        RayTraceResult hitResult = player.rayTrace(5.0D, 0.0F);
        if (hitResult == null || hitResult.typeOfHit != RayTraceResult.Type.BLOCK) {
            player.sendMessage(new TextComponentString("\u00a7cKhông tìm thấy khối! Hãy nhìn vào một khối."));
            return;
        }
        
        BlockPos pos = hitResult.getBlockPos();
        IBlockState state = player.world.getBlockState(pos);
        ResourceLocation blockId = state.getBlock().getRegistryName();
        
        if (blockId == null) {
            player.sendMessage(new TextComponentString("\u00a7cKhông thể xác định ID khối!"));
            return;
        }
        
        ExtractionContext context = new ExtractionContext(
            Minecraft.getMinecraft().getResourceManager(), blockId);
        context.setBlockState(state);
        context.setWorld(player.world);
        context.setBlockPos(pos);
        
        ExtractionResult result = blockExtractor.extract(context);
        sendResult(player, result, blockId);
    }
    
    private void extractEntity(EntityPlayer player) {
        Vec3d eyePos = player.getPositionEyes(1.0F);
        Vec3d lookVec = player.getLook(1.0F);
        Vec3d endPos = eyePos.add(lookVec.x * 5.0D, lookVec.y * 5.0D, lookVec.z * 5.0D);
        
        // Find entity in line of sight
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
        
        if (closestEntity == null) {
            player.sendMessage(new TextComponentString("\u00a7cKhông tìm thấy thực thể! Hãy nhìn vào một thực thể."));
            return;
        }
        
        ResourceLocation entityId = net.minecraft.entity.EntityList.getKey(closestEntity);
        if (entityId == null) {
            player.sendMessage(new TextComponentString("\u00a7cKhông thể xác định ID thực thể!"));
            return;
        }
        
        ExtractionContext context = new ExtractionContext(
            Minecraft.getMinecraft().getResourceManager(), entityId);
        
        ExtractionResult result = entityExtractor.extract(context);
        sendResult(player, result, entityId);
    }
    
    private void extractAll(EntityPlayer player) {
        // Priority: Entity -> Block -> Item
        // Try entity first
        Vec3d eyePos = player.getPositionEyes(1.0F);
        Vec3d lookVec = player.getLook(1.0F);
        Vec3d endPos = eyePos.add(lookVec.x * 5.0D, lookVec.y * 5.0D, lookVec.z * 5.0D);
        
        AxisAlignedBB searchBox = player.getEntityBoundingBox()
            .expand(lookVec.x * 5.0D, lookVec.y * 5.0D, lookVec.z * 5.0D)
            .grow(1.0D, 1.0D, 1.0D);
        
        boolean foundEntity = false;
        for (Entity entity : player.world.getEntitiesWithinAABBExcludingEntity(player, searchBox)) {
            if (!entity.canBeCollidedWith()) continue;
            AxisAlignedBB entityBox = entity.getEntityBoundingBox().grow(entity.getCollisionBorderSize());
            if (entityBox.calculateIntercept(eyePos, endPos) != null) {
                foundEntity = true;
                break;
            }
        }
        
        if (foundEntity) {
            extractEntity(player);
            return;
        }
        
        // Try block
        RayTraceResult blockHit = player.rayTrace(5.0D, 0.0F);
        if (blockHit != null && blockHit.typeOfHit == RayTraceResult.Type.BLOCK) {
            extractBlock(player);
            return;
        }
        
        // Try item
        extractItem(player);
    }
    
    private void sendResult(EntityPlayer player, ExtractionResult result, ResourceLocation id) {
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
