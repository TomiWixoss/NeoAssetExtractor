package com.neoassetextractor.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Renders spawn eggs with their actual colors by applying ItemColors to template
 */
public class SpawnEggRenderer {
    
    /**
     * Check if an item is a spawn egg
     */
    public static boolean isSpawnEgg(Item item) {
        return item instanceof SpawnEggItem;
    }
    
    /**
     * Render spawn egg with actual colors using ItemColors
     */
    public static byte[] renderSpawnEgg(ItemStack itemStack, ResourceManager resourceManager) {
        try {
            System.out.println("[SpawnEggRenderer] Rendering spawn egg: " + itemStack.getItem());
            
            // Load template textures
            ResourceLocation layer0Loc = ResourceLocation.fromNamespaceAndPath(
                "minecraft", "textures/item/spawn_egg.png");
            ResourceLocation layer1Loc = ResourceLocation.fromNamespaceAndPath(
                "minecraft", "textures/item/spawn_egg_overlay.png");
            
            byte[] layer0Bytes = ResourceUtil.loadAsBytes(resourceManager, layer0Loc);
            byte[] layer1Bytes = ResourceUtil.loadAsBytes(resourceManager, layer1Loc);
            
            if (layer0Bytes == null || layer1Bytes == null) {
                System.err.println("[SpawnEggRenderer] Failed to load template textures");
                return null;
            }
            
            BufferedImage layer0 = ImageIO.read(new ByteArrayInputStream(layer0Bytes));
            BufferedImage layer1 = ImageIO.read(new ByteArrayInputStream(layer1Bytes));
            
            int width = layer0.getWidth();
            int height = layer0.getHeight();
            
            System.out.println("[SpawnEggRenderer] Template size: " + width + "x" + height);
            
            // Get colors from ItemColors
            ItemColors itemColors = Minecraft.getInstance().getItemColors();
            int color0 = itemColors.getColor(itemStack, 0); // Base layer
            int color1 = itemColors.getColor(itemStack, 1); // Overlay layer
            
            System.out.println("[SpawnEggRenderer] Color 0 (base): 0x" + Integer.toHexString(color0));
            System.out.println("[SpawnEggRenderer] Color 1 (overlay): 0x" + Integer.toHexString(color1));
            
            // Create result image
            BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            
            // Apply layer 0 with color 0
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = layer0.getRGB(x, y);
                    int alpha = (pixel >> 24) & 0xFF;
                    
                    if (alpha > 0) {
                        // Get grayscale from template
                        int r = (pixel >> 16) & 0xFF;
                        int g = (pixel >> 8) & 0xFF;
                        int b = pixel & 0xFF;
                        
                        // Apply color tint
                        int colorR = (color0 >> 16) & 0xFF;
                        int colorG = (color0 >> 8) & 0xFF;
                        int colorB = color0 & 0xFF;
                        
                        // Multiply template with color
                        int newR = (r * colorR) / 255;
                        int newG = (g * colorG) / 255;
                        int newB = (b * colorB) / 255;
                        
                        int newPixel = (alpha << 24) | (newR << 16) | (newG << 8) | newB;
                        result.setRGB(x, y, newPixel);
                    }
                }
            }
            
            // Blend layer 1 with color 1 on top
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = layer1.getRGB(x, y);
                    int alpha = (pixel >> 24) & 0xFF;
                    
                    if (alpha > 0) {
                        // Get grayscale from template
                        int r = (pixel >> 16) & 0xFF;
                        int g = (pixel >> 8) & 0xFF;
                        int b = pixel & 0xFF;
                        
                        // Apply color tint
                        int colorR = (color1 >> 16) & 0xFF;
                        int colorG = (color1 >> 8) & 0xFF;
                        int colorB = color1 & 0xFF;
                        
                        // Multiply template with color
                        int newR = (r * colorR) / 255;
                        int newG = (g * colorG) / 255;
                        int newB = (b * colorB) / 255;
                        
                        // Alpha blend with existing pixel
                        int existingPixel = result.getRGB(x, y);
                        int existingR = (existingPixel >> 16) & 0xFF;
                        int existingG = (existingPixel >> 8) & 0xFF;
                        int existingB = existingPixel & 0xFF;
                        
                        float alphaF = alpha / 255.0f;
                        int blendedR = (int) (newR * alphaF + existingR * (1 - alphaF));
                        int blendedG = (int) (newG * alphaF + existingG * (1 - alphaF));
                        int blendedB = (int) (newB * alphaF + existingB * (1 - alphaF));
                        
                        int blendedPixel = (255 << 24) | (blendedR << 16) | (blendedG << 8) | blendedB;
                        result.setRGB(x, y, blendedPixel);
                    }
                }
            }
            
            // Convert to PNG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(result, "PNG", baos);
            byte[] pngBytes = baos.toByteArray();
            
            System.out.println("[SpawnEggRenderer] Generated PNG: " + pngBytes.length + " bytes");
            
            return pngBytes;
            
        } catch (Exception e) {
            System.err.println("[SpawnEggRenderer] Error:");
            e.printStackTrace();
            return null;
        }
    }
}
