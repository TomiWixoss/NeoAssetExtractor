# Output Structure Changes

## OLD Structure (Mixed Assets)
```
.minecraft/extracted_assets/
└── minecraft/
    ├── blocks/
    │   ├── blockstates/
    │   │   ├── grass_block.json
    │   │   ├── spruce_door.json
    │   │   └── dandelion.json
    │   ├── models/
    │   │   ├── grass_block.json
    │   │   ├── spruce_door_bottom_left.json
    │   │   └── dandelion.json
    │   ├── textures/
    │   │   ├── grass_block_top.png
    │   │   ├── spruce_door_bottom.png
    │   │   └── dandelion.png
    │   ├── textures_tinted/
    │   │   └── grass_block_top_tinted.png
    │   └── textures_rendered/
    │       └── grass_block_top_rendered.png
    └── items/
        ├── models/
        │   ├── diamond_sword.json
        │   └── creeper_spawn_egg.json
        └── textures/
            └── diamond_sword.png
```

## NEW Structure (Separated by Asset)
```
.minecraft/extracted_assets/
└── minecraft/
    ├── blocks/
    │   ├── grass_block/
    │   │   ├── blockstates/
    │   │   │   └── grass_block.json
    │   │   ├── models/
    │   │   │   └── grass_block.json
    │   │   ├── textures/
    │   │   │   ├── grass_block_top.png
    │   │   │   ├── grass_block_side.png
    │   │   │   └── dirt.png
    │   │   ├── textures_tinted/
    │   │   │   ├── grass_block_top_tinted.png
    │   │   │   └── grass_block_side_overlay_tinted.png
    │   │   └── textures_rendered/
    │   │       ├── grass_block_top_rendered.png
    │   │       └── grass_block_side_rendered.png
    │   ├── spruce_door/
    │   │   ├── blockstates/
    │   │   │   └── spruce_door.json
    │   │   ├── models/
    │   │   │   ├── spruce_door_bottom_left.json
    │   │   │   ├── spruce_door_bottom_right.json
    │   │   │   ├── spruce_door_top_left.json
    │   │   │   └── spruce_door_top_right.json
    │   │   └── textures/
    │   │       ├── spruce_door_bottom.png
    │   │       └── spruce_door_top.png
    │   └── dandelion/
    │       ├── blockstates/
    │       │   └── dandelion.json
    │       ├── models/
    │       │   └── dandelion.json
    │       └── textures/
    │           └── dandelion.png
    ├── items/
    │   ├── diamond_sword/
    │   │   ├── models/
    │   │   │   └── diamond_sword.json
    │   │   └── textures/
    │   │       └── diamond_sword.png
    │   └── creeper_spawn_egg/
    │       ├── models/
    │       │   └── creeper_spawn_egg.json
    │       └── textures/
    │           └── (parent model textures)
    └── entities/
        └── creeper/
            └── textures/
                └── creeper.png
```

## Benefits
1. **Organized**: Each asset has its own folder
2. **Easy to find**: All files for one asset are in one place
3. **Portable**: Can copy entire asset folder to another project
4. **Clean**: No mixing of different assets in same folder
5. **Scalable**: Works well with hundreds or thousands of assets

## Implementation Status
✅ AssetWriter.getAssetPath() - Updated with new structure
✅ BlockstateExtractor - Updated to use getAssetPath()
✅ BlockModelExtractor - Updated to use getAssetPath()
✅ BlockTextureExtractor - Updated to use getAssetPath()
✅ ItemExtractor - Updated to use getAssetPath()
✅ EntityExtractor - Updated to use getAssetPath()

## Testing
To test the new structure:
1. Delete old output: `runs/client/.minecraft/extracted_assets/`
2. Run game: `./gradlew runClient`
3. In game, run: `/extract block grass_block`
4. Check output structure matches NEW format above
