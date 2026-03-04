package com.neoassetextractor.extractor;

import com.neoassetextractor.NeoAssetExtractor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BlockExtractor {
    
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
            
            String blockId = state.getBlock().toString();
            NeoAssetExtractor.LOGGER.info("Extracting block: {}", blockId);
            
            // TODO: Implement actual extraction logic
            // - Get blockstate JSON
            // - Get block model JSON
            // - Get block textures
            // - Handle tinted blocks (grass, leaves, water)
            
            source.sendSuccess(() -> Component.literal("§a[WIP] Đã phát hiện khối: " + blockId), false);
            return 1;
            
        } catch (Exception e) {
            NeoAssetExtractor.LOGGER.error("Error extracting block", e);
            source.sendFailure(Component.literal("§cLỗi khi trích xuất: " + e.getMessage()));
            return 0;
        }
    }
}
