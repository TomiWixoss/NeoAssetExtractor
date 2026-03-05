package com.neoassetextractor.core;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.state.IBlockState;

/**
 * Context object containing all information needed for extraction
 */
public class ExtractionContext {
    private final IResourceManager resourceManager;
    private final ResourceLocation resourceId;
    private final String namespace;
    private final String path;
    private String assetType = "items"; // Default to items
    private boolean blockbenchMode = false;
    
    // Optional fields for block extraction
    private IBlockState blockState;
    private World world;
    private BlockPos blockPos;
    
    public ExtractionContext(IResourceManager resourceManager, ResourceLocation resourceId) {
        this.resourceManager = resourceManager;
        this.resourceId = resourceId;
        this.namespace = resourceId.getNamespace();
        this.path = resourceId.getPath();
    }
    
    // Getters
    public IResourceManager getResourceManager() { return resourceManager; }
    public ResourceLocation getResourceId() { return resourceId; }
    public String getNamespace() { return namespace; }
    public String getPath() { return path; }
    public String getAssetType() { return assetType; }
    public boolean isBlockbenchMode() { return blockbenchMode; }
    public IBlockState getBlockState() { return blockState; }
    public World getWorld() { return world; }
    public BlockPos getBlockPos() { return blockPos; }
    
    // Setters for optional fields
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public void setBlockbenchMode(boolean blockbenchMode) { this.blockbenchMode = blockbenchMode; }
    public void setBlockState(IBlockState blockState) { this.blockState = blockState; }
    public void setWorld(World world) { this.world = world; }
    public void setBlockPos(BlockPos blockPos) { this.blockPos = blockPos; }
}
