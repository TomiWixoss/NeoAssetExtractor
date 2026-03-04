package com.neoassetextractor.extractor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.neoassetextractor.NeoAssetExtractor;
import com.neoassetextractor.util.AssetWriter;
import com.neoassetextractor.util.TextureCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

public class BlockExtractor {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    public static int extract(Player player, CommandSourceStack source) {
        HitResult hitResult = player.pick(5.0D, 0.0F, false);
        
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.literal("§cKhông tìm thấy khối! Hãy nhìn vào một khối."));
            return 0;
        }

        try {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = player.level().getBlockState(pos);
            
            ResourceLocation blockId = state.getBlock().builtInRegistryHolder().key().location();
            String namespace = blockId.getNamespace();
            String path = blockId.getPath();
            
            NeoAssetExtractor.LOGGER.info("Extracting block: {}:{}", namespace, path);
            
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            
            // Extract blockstate JSON
            boolean blockstateExtracted = extractBlockstate(resourceManager, namespace, path);
            
            // Extract block model JSON
            boolean modelExtracted = extractBlockModel(resourceManager, namespace, path);
            
            // Extract block textures (with tinting support)
            int texturesExtracted = extractBlockTextures(resourceManager, namespace, path, state, player.level(), pos);
            
            if (blockstateExtracted || modelExtracted || texturesExtracted > 0) {
                source.sendSuccess(() -> Component.literal(
                    "§a✓ Đã trích xuất: §f" + namespace + ":" + path + "\n" +
                    "§7Blockstate: " + (blockstateExtracted ? "§a✓" : "§c✗") +
                    " §7Model: " + (modelExtracted ? "§a✓" : "§c✗") +
                    " §7Textures: §a" + texturesExtracted
                ), false);
                return 1;
            } else {
                source.sendFailure(Component.literal("§c✗ Không tìm thấy assets cho block này"));
                return 0;
            }
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Error extracting block", e);
            source.sendFailure(Component.literal("§cLỗi: " + e.getMessage()));
            return 0;
        }
    }
    
    private static boolean extractBlockstate(ResourceManager resourceManager, String namespace, String blockPath) {
        try {
            ResourceLocation blockstateLocation = ResourceLocation.fromNamespaceAndPath(
                namespace,
                "blockstates/" + blockPath + ".json"
            );
            
            Optional<Resource> resourceOpt = resourceManager.getResource(blockstateLocation);
            if (resourceOpt.isEmpty()) {
                NeoAssetExtractor.LOGGER.warn("Blockstate not found: {}", blockstateLocation);
                return false;
            }
            
            Resource resource = resourceOpt.get();
            
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
            
            JsonObject jsonObject = GSON.fromJson(jsonContent, JsonObject.class);
            String prettyJson = GSON.toJson(jsonObject);
            
            Path outputPath = AssetWriter.getOutputPath(namespace, "blocks/blockstates")
                .resolve(blockPath + ".json");
            AssetWriter.writeFile(outputPath, prettyJson);
            
            NeoAssetExtractor.LOGGER.info("Extracted blockstate: {}", outputPath);
            return true;
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to extract blockstate: {}:{}", namespace, blockPath, e);
            return false;
        }
    }
    
    private static boolean extractBlockModel(ResourceManager resourceManager, String namespace, String blockPath) {
        try {
            ResourceLocation modelLocation = ResourceLocation.fromNamespaceAndPath(
                namespace,
                "models/block/" + blockPath + ".json"
            );
            
            Optional<Resource> resourceOpt = resourceManager.getResource(modelLocation);
            if (resourceOpt.isEmpty()) {
                NeoAssetExtractor.LOGGER.warn("Block model not found: {}", modelLocation);
                return false;
            }
            
            Resource resource = resourceOpt.get();
            
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
            
            JsonObject jsonObject = GSON.fromJson(jsonContent, JsonObject.class);
            String prettyJson = GSON.toJson(jsonObject);
            
            Path outputPath = AssetWriter.getOutputPath(namespace, "blocks/models")
                .resolve(blockPath + ".json");
            AssetWriter.writeFile(outputPath, prettyJson);
            
            NeoAssetExtractor.LOGGER.info("Extracted block model: {}", outputPath);
            return true;
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to extract block model: {}:{}", namespace, blockPath, e);
            return false;
        }
    }
    
    private static boolean extractBlockTexture(ResourceManager resourceManager, String namespace, String blockPath) {
        try {
            ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(
                namespace,
                "textures/block/" + blockPath + ".png"
            );
            
            Optional<Resource> resourceOpt = resourceManager.getResource(textureLocation);
            if (resourceOpt.isEmpty()) {
                NeoAssetExtractor.LOGGER.warn("Block texture not found: {}", textureLocation);
                return false;
            }
            
            Resource resource = resourceOpt.get();
            
            byte[] textureData;
            try (InputStream is = resource.open()) {
                textureData = is.readAllBytes();
            }
            
            Path outputPath = AssetWriter.getOutputPath(namespace, "blocks/textures")
                .resolve(blockPath + ".png");
            AssetWriter.writeFile(outputPath, textureData);
            
            NeoAssetExtractor.LOGGER.info("Extracted block texture: {}", outputPath);
            return true;
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to extract block texture: {}:{}", namespace, blockPath, e);
            return false;
        }
    }
    
    private static int extractBlockTextures(ResourceManager resourceManager, String namespace, String blockPath,
                                           BlockState state, net.minecraft.world.level.Level level, BlockPos pos) {
        int count = 0;
        
        try {
            // Try to get the model JSON to parse texture references
            ResourceLocation modelLocation = ResourceLocation.fromNamespaceAndPath(
                namespace,
                "models/block/" + blockPath + ".json"
            );
            
            Optional<Resource> modelResourceOpt = resourceManager.getResource(modelLocation);
            if (modelResourceOpt.isEmpty()) {
                // Fallback: try direct texture
                if (extractBlockTexture(resourceManager, namespace, blockPath)) {
                    count++;
                }
                return count;
            }
            
            // Parse model JSON to get texture references
            Resource modelResource = modelResourceOpt.get();
            String jsonContent;
            try (InputStream is = modelResource.open();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                jsonContent = sb.toString();
            }
            
            JsonObject modelJson = GSON.fromJson(jsonContent, JsonObject.class);
            boolean hasTinting = TextureCapture.hasTinting(jsonContent);
            
            // Extract textures from "textures" object
            if (modelJson.has("textures")) {
                JsonObject textures = modelJson.getAsJsonObject("textures");
                for (String key : textures.keySet()) {
                    String texturePath = textures.get(key).getAsString();
                    
                    // Parse texture path
                    String textureNamespace = namespace;
                    String textureFile = texturePath;
                    
                    if (texturePath.contains(":")) {
                        String[] parts = texturePath.split(":", 2);
                        textureNamespace = parts[0];
                        textureFile = parts[1];
                    }
                    
                    if (textureFile.startsWith("block/")) {
                        textureFile = textureFile.substring(6);
                    }
                    
                    // Extract base texture
                    if (extractBlockTexture(resourceManager, textureNamespace, textureFile)) {
                        count++;
                        
                        // Extract tinted version if applicable
                        if (hasTinting) {
                            int tintIndex = TextureCapture.getTintIndex(jsonContent, key);
                            
                            if (tintIndex >= 0) {
                                ResourceLocation texLoc = ResourceLocation.fromNamespaceAndPath(
                                    textureNamespace,
                                    "block/" + textureFile
                                );
                                Path tintedPath = AssetWriter.getOutputPath(textureNamespace, "blocks/textures_tinted")
                                    .resolve(textureFile + "_tinted.png");
                                
                                TextureCapture.captureBlockTextureWithTint(
                                    texLoc, state, level, pos, tintIndex, tintedPath);
                            }
                        }
                    }
                }
            }
            
            // Fallback if no textures found
            if (count == 0 && extractBlockTexture(resourceManager, namespace, blockPath)) {
                count++;
            }
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to extract block textures: {}:{}", namespace, blockPath, e);
        }
        
        return count;
    }
}
