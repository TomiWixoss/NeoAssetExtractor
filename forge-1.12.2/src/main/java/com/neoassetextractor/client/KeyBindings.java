package com.neoassetextractor.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

/**
 * Đăng ký các phím tắt cho extraction (Forge 1.12.2)
 */
public class KeyBindings {
    
    public static final String CATEGORY = "key.categories.neoassetextractor";
    
    // Phím tắt extract (tự động detect loại asset)
    public static final KeyBinding EXTRACT_KEY = new KeyBinding(
        "key.neoassetextractor.extract",
        Keyboard.KEY_K,
        CATEGORY
    );
    
    // Phím tắt extract toàn bộ creative tab hiện tại
    public static final KeyBinding EXTRACT_TAB_KEY = new KeyBinding(
        "key.neoassetextractor.extract_tab",
        Keyboard.KEY_L,
        CATEGORY
    );
    
    // Phím tắt extract Blockbench format
    public static final KeyBinding EXTRACT_BLOCKBENCH_KEY = new KeyBinding(
        "key.neoassetextractor.extract_blockbench",
        Keyboard.KEY_J,
        CATEGORY
    );
    
    public static void register() {
        ClientRegistry.registerKeyBinding(EXTRACT_KEY);
        ClientRegistry.registerKeyBinding(EXTRACT_TAB_KEY);
        ClientRegistry.registerKeyBinding(EXTRACT_BLOCKBENCH_KEY);
    }
}
