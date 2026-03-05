package com.neoassetextractor.util;

import com.neoassetextractor.NeoAssetExtractor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

/**
 * Utility class for capturing textures with tinting applied (Forge 1.12.2)
 */
public class TextureCapture {
    
    /**
     * Capture a block texture with tinting applied
     */
    public static boolean captureBlockTextureWithTint(
            ResourceLocation textureLocation, 
            IBlockState blockState,
            World world,
            BlockPos pos,
            int tintIndex,
            Path outputPath) {
        
        try {
            Minecraft mc = Minecraft.getMinecraft();
            TextureMap textureMap = mc.getTextureMapBlocks();
            
            if (textureMap == null) {
                NeoAssetExtractor.LOGGER.warn("Texture map not found");
                return false;
            }
            
            // Get the sprite from atlas
            TextureAtlasSprite sprite = textureMap.getAtlasSprite(textureLocation.toString());
            if (sprite == null || sprite == textureMap.getMissingSprite()) {
                NeoAssetExtractor.LOGGER.warn("Sprite not found in atlas: {}", textureLocation);
                return false;
            }
            
            // Get tint color if applicable
            int tintColor = 0xFFFFFF; // Default white (no tint)
            if (tintIndex >= 0) {
                tintColor = mc.getBlockColors().colorMultiplier(blockState, world, pos, tintIndex);
            }
            
            // Get sprite dimensions
            int width = sprite.getIconWidth();
            int height = sprite.getIconHeight();
            
            // Get pixels from sprite
            int[][] framePixels = sprite.getFrameTextureData(0);
            if (framePixels == null || framePixels.length == 0 || framePixels[0] == null) {
                NeoAssetExtractor.LOGGER.warn("No pixel data in sprite");
                return false;
            }
            
            int[] pixels = framePixels[0]; // Mip level 0
            
            // Extract tint RGB
            float tintR = ((tintColor >> 16) & 0xFF) / 255.0f;
            float tintG = ((tintColor >> 8) & 0xFF) / 255.0f;
            float tintB = (tintColor & 0xFF) / 255.0f;
            
            // Create result image
            BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    if (index >= pixels.length) break;
                    
                    int pixel = pixels[index];
                    int a = (pixel >> 24) & 0xFF;
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;
                    
                    // Apply tint
                    int newR = Math.min(255, Math.max(0, (int)(r * tintR)));
                    int newG = Math.min(255, Math.max(0, (int)(g * tintG)));
                    int newB = Math.min(255, Math.max(0, (int)(b * tintB)));
                    
                    int newPixel = (a << 24) | (newR << 16) | (newG << 8) | newB;
                    resultImage.setRGB(x, y, newPixel);
                }
            }
            
            // Save to file
            AssetWriter.ensureDirectoryExists(outputPath.getParent());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(resultImage, "PNG", baos);
            AssetWriter.writeFile(outputPath, baos.toByteArray());
            
            NeoAssetExtractor.LOGGER.info("Captured texture with tint 0x{}: {}", 
                Integer.toHexString(tintColor), outputPath);
            return true;
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Failed to capture texture: {}", textureLocation, e);
            return false;
        }
    }
    
    public static boolean hasTinting(String modelJson) {
        return modelJson.contains("\"tintindex\"");
    }
    
    public static int getTintIndex(String modelJson, String textureKey) {
        try {
            String searchPattern = "\"texture\": \"#" + textureKey + "\"";
            int texturePos = modelJson.indexOf(searchPattern);
            
            if (texturePos == -1) {
                return -1;
            }
            
            String segment = modelJson.substring(texturePos, 
                Math.min(modelJson.length(), texturePos + 200));
            
            String tintPattern = "\"tintindex\": ";
            int tintPos = segment.indexOf(tintPattern);
            
            if (tintPos == -1) {
                return -1;
            }
            
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
