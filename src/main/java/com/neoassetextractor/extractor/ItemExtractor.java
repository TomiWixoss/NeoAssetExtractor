package com.neoassetextractor.extractor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.neoassetextractor.NeoAssetExtractor;
import com.neoassetextractor.util.AssetWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

public class ItemExtractor {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    public static int extract(Player player, CommandSourceStack source) {
        ItemStack heldItem = player.getMainHandItem();
        
        if (heldItem.isEmpty()) {
            source.sendFailure(Component.literal("§cBạn phải cầm vật phẩm trên tay!"));
            return 0;
        }

        try {
            ResourceLocation itemId = heldItem.getItem().builtInRegistryHolder().key().location();
            String namespace = itemId.getNamespace();
            String path = itemId.getPath();
            
            NeoAssetExtractor.LOGGER.info("Extracting item: {}:{}", namespace, path);
            
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            
            // Extract item model JSON
            boolean modelExtracted = extractItemModel(resourceManager, namespace, path);
            
            // Extract item textures
            boolean textureExtracted = extractItemTexture(resourceManager, namespace, path);
            
            if (modelExtracted || textureExtracted) {
                source.sendSuccess(() -> Component.literal(
                    "§a✓ Đã trích xuất: §f" + namespace + ":" + path + "\n" +
                    "§7Model: " + (modelExtracted ? "§a✓" : "§c✗") + 
                    " §7Texture: " + (textureExtracted ? "§a✓" : "§c✗")
                ), false);
                return 1;
            } else {
                source.sendFailure(Component.literal("§c✗ Không tìm thấy assets cho item này"));
                return 0;
            }
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Error extracting item", e);
            source.sendFailure(Component.literal("§cLỗi: " + e.getMessage()));
            return 0;
        }
    }
    
    private static boolean extractItemModel(ResourceManager resourceManager, String namespace, String itemPath) {
        try {
            // Try to get item model JSON
            ResourceLocation modelLocation = ResourceLocation.fromNamespaceAndPath(
                namespace, 
                "models/item/" + itemPath + ".json"
            );
            
            Optional<Resource> resourceOpt = resourceManager.getResource(modelLocation);
            if (resourceOpt.isEmpty()) {
                NeoAssetExtractor.LOGGER.warn("Item model not found: {}", modelLocation);
                return false;
            }
            
            Resource resource = resourceOpt.get();
            
            // Read JSON content
            String jsonContent;
            try (InputStream is = resource.open();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                jsonContent = sb.toString();
            }
            
            // Parse and pretty print
            JsonObject jsonObject = GSON.fromJson(jsonContent, JsonObject.class);
            String prettyJson = GSON.toJson(jsonObject);
            
            // Write to file
            Path outputPath = AssetWriter.getOutputPath(namespace, "items/models")
                .resolve(itemPath + ".json");
            AssetWriter.writeFile(outputPath, prettyJson);
            
            NeoAssetExtractor.LOGGER.info("Extracted item model: {}", outputPath);
            return true;
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to extract item model: {}:{}", namespace, itemPath, e);
            return false;
        }
    }
    
    private static boolean extractItemTexture(ResourceManager resourceManager, String namespace, String itemPath) {
        try {
            // Try to get item texture
            ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(
                namespace,
                "textures/item/" + itemPath + ".png"
            );
            
            Optional<Resource> resourceOpt = resourceManager.getResource(textureLocation);
            if (resourceOpt.isEmpty()) {
                NeoAssetExtractor.LOGGER.warn("Item texture not found: {}", textureLocation);
                return false;
            }
            
            Resource resource = resourceOpt.get();
            
            // Read texture bytes
            byte[] textureData;
            try (InputStream is = resource.open()) {
                textureData = is.readAllBytes();
            }
            
            // Write to file
            Path outputPath = AssetWriter.getOutputPath(namespace, "items/textures")
                .resolve(itemPath + ".png");
            AssetWriter.writeFile(outputPath, textureData);
            
            NeoAssetExtractor.LOGGER.info("Extracted item texture: {}", outputPath);
            return true;
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to extract item texture: {}:{}", namespace, itemPath, e);
            return false;
        }
    }
}
