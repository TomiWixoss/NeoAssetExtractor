package com.neoassetextractor.extractor;

import com.neoassetextractor.NeoAssetExtractor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class EntityExtractor {
    
    public static int extract(Player player, CommandSourceStack source) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(5.0D));
        
        AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(5.0D)).inflate(1.0D);
        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
            player.level(), 
            player, 
            eyePos, 
            endPos, 
            searchBox, 
            entity -> !entity.isSpectator() && entity.isPickable()
        );
        
        if (hitResult == null) {
            source.sendFailure(Component.literal("§cKhông tìm thấy thực thể! Hãy nhìn vào một thực thể."));
            return 0;
        }

        try {
            Entity entity = hitResult.getEntity();
            String entityId = entity.getType().toString();
            NeoAssetExtractor.LOGGER.info("Extracting entity: {}", entityId);
            
            // TODO: Implement actual extraction logic
            // - Detect if entity is GeckoLib/AzureLib
            // - Extract .geo.json, .animation.json, textures for GeckoLib entities
            // - Extract only textures for vanilla Java entities (with warning)
            // - Handle BEWLR items
            
            source.sendSuccess(() -> Component.literal("§a[WIP] Đã phát hiện thực thể: " + entityId), false);
            return 1;
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Error extracting entity", e);
            source.sendFailure(Component.literal("§cLỗi khi trích xuất: " + e.getMessage()));
            return 0;
        }
    }
}
