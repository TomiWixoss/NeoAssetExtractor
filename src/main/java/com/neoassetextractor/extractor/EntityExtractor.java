package com.neoassetextractor.extractor;

import com.neoassetextractor.NeoAssetExtractor;
import com.neoassetextractor.util.AssetWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

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
            ResourceLocation entityId = entity.getType().builtInRegistryHolder().key().location();
            String namespace = entityId.getNamespace();
            String path = entityId.getPath();
            
            NeoAssetExtractor.LOGGER.info("Extracting entity: {}:{}", namespace, path);
            
            // Check if entity is GeckoLib
            boolean isGeckoLib = isGeckoLibEntity(entity);
            
            if (isGeckoLib) {
                source.sendSuccess(() -> Component.literal(
                    "§e⚠ Entity này sử dụng GeckoLib\n" +
                    "§7Tính năng trích xuất GeckoLib đang được phát triển"
                ), false);
                return 1;
            }
            
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            
            // Try to extract entity texture
            boolean textureExtracted = extractEntityTexture(resourceManager, namespace, path);
            
            if (textureExtracted) {
                source.sendSuccess(() -> Component.literal(
                    "§a✓ Đã trích xuất texture: §f" + namespace + ":" + path + "\n" +
                    "§e⚠ Entity này dùng Java code model (không có file JSON)"
                ), false);
                return 1;
            } else {
                source.sendFailure(Component.literal("§c✗ Không tìm thấy texture cho entity này"));
                return 0;
            }
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Error extracting entity", e);
            source.sendFailure(Component.literal("§cLỗi: " + e.getMessage()));
            return 0;
        }
    }
    
    private static boolean isGeckoLibEntity(Entity entity) {
        try {
            // Check if entity implements GeoEntity interface
            Class<?> geoEntityClass = Class.forName("software.bernie.geckolib.animatable.GeoEntity");
            return geoEntityClass.isInstance(entity);
        } catch (ClassNotFoundException e) {
            // GeckoLib not present
            return false;
        }
    }
    
    private static boolean extractEntityTexture(ResourceManager resourceManager, String namespace, String entityPath) {
        try {
            ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(
                namespace,
                "textures/entity/" + entityPath + ".png"
            );
            
            Optional<Resource> resourceOpt = resourceManager.getResource(textureLocation);
            if (resourceOpt.isEmpty()) {
                NeoAssetExtractor.LOGGER.warn("Entity texture not found: {}", textureLocation);
                return false;
            }
            
            Resource resource = resourceOpt.get();
            
            byte[] textureData;
            try (InputStream is = resource.open()) {
                textureData = is.readAllBytes();
            }
            
            Path outputPath = AssetWriter.getOutputPath(namespace, "entities/textures")
                .resolve(entityPath + ".png");
            AssetWriter.writeFile(outputPath, textureData);
            
            NeoAssetExtractor.LOGGER.info("Extracted entity texture: {}", outputPath);
            return true;
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to extract entity texture: {}:{}", namespace, entityPath, e);
            return false;
        }
    }
}
