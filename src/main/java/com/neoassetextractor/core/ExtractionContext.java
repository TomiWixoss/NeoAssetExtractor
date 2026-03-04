package com.neoassetextractor.core;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Context object containing all information needed for extraction
 */
public class ExtractionContext {
    private final ResourceManager resourceManager;
    private final ResourceLocation resourceId;
    private final String namespace;
    private final String path;
    private String assetType = "items"; // Default to items
    
    // Optional fields for block extraction
    private BlockState blockState;
    private Level level;
    private BlockPos blockPos;
    
    public ExtractionContext(ResourceManager resourceManager, ResourceLocation resourceId) {
        this.resourceManager = resourceManager;
        this.resourceId = resourceId;
        this.namespace = resourceId.getNamespace();
        this.path = resourceId.getPath();
    }
    
    // Getters
    public ResourceManager getResourceManager() { return resourceManager; }
    public ResourceLocation getResourceId() { return resourceId; }
    public String getNamespace() { return namespace; }
    public String getPath() { return path; }
    public String getAssetType() { return assetType; }
    public BlockState getBlockState() { return blockState; }
    public Level getLevel() { return level; }
    public BlockPos getBlockPos() { return blockPos; }
    
    // Setters for optional fields
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public void setBlockState(BlockState blockState) { this.blockState = blockState; }
    public void setLevel(Level level) { this.level = level; }
    public void setBlockPos(BlockPos blockPos) { this.blockPos = blockPos; }
}
