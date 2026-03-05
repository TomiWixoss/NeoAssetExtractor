package com.neoassetextractor.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Đăng ký các phím tắt cho extraction
 */
public class KeyBindings {
    
    public static final String CATEGORY = "key.categories.neoassetextractor";
    
    // Phím tắt extract (tự động detect loại asset)
    // Sử dụng phím K để tránh conflict với vanilla (E = inventory)
    public static final KeyMapping EXTRACT_KEY = new KeyMapping(
        "key.neoassetextractor.extract",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        CATEGORY
    );
    
    // Phím tắt extract toàn bộ creative tab hiện tại
    // Sử dụng L (List/aLl) khi đang trong creative inventory
    public static final KeyMapping EXTRACT_TAB_KEY = new KeyMapping(
        "key.neoassetextractor.extract_tab",
        KeyConflictContext.GUI,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_L,
        CATEGORY
    );
    
    // Phím tắt extract Blockbench format (flat structure)
    // Sử dụng J cho Blockbench-ready output
    public static final KeyMapping EXTRACT_BLOCKBENCH_KEY = new KeyMapping(
        "key.neoassetextractor.extract_blockbench",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_J,
        CATEGORY
    );
    
    public static void register() {
        // Keybinds sẽ được register trong ClientEventHandler
    }
}
