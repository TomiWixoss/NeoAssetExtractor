package com.neoassetextractor.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.neoassetextractor.NeoAssetExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;

/**
 * Utility class for capturing textures with tinting applied
 */
public class TextureCapture {
    
    /**
     * Capture a block texture with tinting applied
     * @param textureLocation The texture location (e.g., "minecraft:block/grass_block_top")
     * @param blockState The block state (for getting tint color)
     * @param level The level (for biome color)
     * @param pos The block position (for biome color)
     * @param tintIndex The tint index from model (-1 if no tinting)
     * @param outputPath Where to save the captured texture
     * @return true if successful
     */
    public static boolean captureBlockTextureWithTint(
            ResourceLocation textureLocation, 
            BlockState blockState,
            Level level,
            BlockPos pos,
            int tintIndex,
            Path outputPath) {
        
        try {
            Minecraft mc = Minecraft.getInstance();
            TextureManager textureManager = mc.getTextureManager();
            
            // Get the block atlas
            ResourceLocation atlasLocation = TextureAtlas.LOCATION_BLOCKS;
            AbstractTexture atlasTexture = textureManager.getTexture(atlasLocation);
            
            if (!(atlasTexture instanceof TextureAtlas atlas)) {
                NeoAssetExtractor.LOGGER.warn("Block atlas not found");
                return false;
            }
            
            // Get the sprite from atlas
            TextureAtlasSprite sprite = atlas.getSprite(textureLocation);
            if (sprite == null) {
                NeoAssetExtractor.LOGGER.warn("Sprite not found in atlas: {}", textureLocation);
                return false;
            }
            
            // Get tint color if applicable
            int tintColor = 0xFFFFFF; // Default white (no tint)
            if (tintIndex >= 0) {
                BlockColors blockColors = mc.getBlockColors();
                tintColor = blockColors.getColor(blockState, level, pos, tintIndex);
            }
            
            // Capture sprite with tint applied
            NativeImage image = captureSpriteWithTint(sprite, tintColor);
            if (image == null) {
                return false;
            }
            
            // Save to file
            AssetWriter.ensureDirectoryExists(outputPath.getParent());
            image.writeToFile(outputPath);
            image.close();
            
            NeoAssetExtractor.LOGGER.info("Captured texture with tint 0x{}: {}", 
                Integer.toHexString(tintColor), outputPath);
            return true;
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to capture texture: {}", textureLocation, e);
            return false;
        }
    }
    
    /**
     * Capture sprite pixels and apply tint color
     */
    private static NativeImage captureSpriteWithTint(TextureAtlasSprite sprite, int tintColor) {
        try {
            int width = sprite.contents().width();
            int height = sprite.contents().height();
            
            // Get source image from sprite
            NativeImage[] frames = sprite.contents().byMipLevel;
            if (frames == null || frames.length == 0 || frames[0] == null) {
                NeoAssetExtractor.LOGGER.warn("No image data in sprite");
                return null;
            }
            
            NativeImage sourceImage = frames[0];
            NativeImage resultImage = new NativeImage(width, height, false);
            
            // Extract RGB components from tint color
            float tintR = ((tintColor >> 16) & 0xFF) / 255.0f;
            float tintG = ((tintColor >> 8) & 0xFF) / 255.0f;
            float tintB = (tintColor & 0xFF) / 255.0f;
            
            // Copy and tint pixels
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = sourceImage.getPixelRGBA(x, y);
                    
                    // Extract RGBA components (ABGR format in NativeImage)
                    int a = (pixel >> 24) & 0xFF;
                    int b = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int r = pixel & 0xFF;
                    
                    // Apply tint
                    int newR = (int)(r * tintR);
                    int newG = (int)(g * tintG);
                    int newB = (int)(b * tintB);
                    
                    // Clamp values
                    newR = Math.min(255, Math.max(0, newR));
                    newG = Math.min(255, Math.max(0, newG));
                    newB = Math.min(255, Math.max(0, newB));
                    
                    // Reconstruct pixel (ABGR format)
                    int newPixel = (a << 24) | (newB << 16) | (newG << 8) | newR;
                    resultImage.setPixelRGBA(x, y, newPixel);
                }
            }
            
            return resultImage;
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to capture sprite pixels", e);
            return null;
        }
    }
    
    /**
     * Check if a texture has tinting applied (based on model JSON)
     */
    public static boolean hasTinting(String modelJson) {
        return modelJson.contains("\"tintindex\"");
    }
    
    /**
     * Extract tint index from model JSON for a specific texture key
     * Returns -1 if no tinting found
     */
    public static int getTintIndex(String modelJson, String textureKey) {
        try {
            // Simple parsing - look for tintindex near the texture reference
            // This is a simplified approach; full JSON parsing would be more robust
            String searchPattern = "\"texture\": \"#" + textureKey + "\"";
            int texturePos = modelJson.indexOf(searchPattern);
            
            if (texturePos == -1) {
                return -1;
            }
            
            // Look for tintindex in the same face object (within next 200 chars)
            String segment = modelJson.substring(texturePos, 
                Math.min(modelJson.length(), texturePos + 200));
            
            String tintPattern = "\"tintindex\": ";
            int tintPos = segment.indexOf(tintPattern);
            
            if (tintPos == -1) {
                return -1;
            }
            
            // Extract the number
            int numStart = tintPos + tintPattern.length();
            int numEnd = numStart;
            while (numEnd < segment.length() && Character.isDigit(segment.charAt(numEnd))) {
                numEnd++;
            }
            
            if (numEnd > numStart) {
                return Integer.parseInt(segment.substring(numStart, numEnd));
            }
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to parse tint index", e);
        }
        
        return -1;
    }
}
