# NeoAssetExtractor

Mod trích xuất tài nguyên đồ họa (Models, Textures, Animations) từ Minecraft trong thời gian thực.

## Monorepo Structure

Project này chứa 2 phiên bản cho các Minecraft versions khác nhau:

| Thư mục | Minecraft | Mod Loader | Java | Gradle |
|---------|-----------|------------|------|--------|
| [`neoforge/`](./neoforge/) | 1.21.1 | NeoForge | Java 21 | 8.x |
| [`forge-1.12.2/`](./forge-1.12.2/) | 1.12.2 | MinecraftForge | Java 8 | 4.x |

## Tính năng

- ✅ Trích xuất Items (vật phẩm)
- ✅ Trích xuất Blocks (khối)
- ✅ Trích xuất Entities (thực thể)
- ✅ Hỗ trợ tinted textures (cỏ, lá, nước)
- ✅ Phím tắt (K: extract, L: extract creative tab, J: blockbench variants)
- 🚧 Hỗ trợ GeckoLib/AzureLib (NeoForge only)

## Build

### NeoForge (1.21.1)
```bash
cd neoforge
./gradlew build
```

### Forge (1.12.2)
```bash
cd forge-1.12.2
./gradlew build
```

## Sử dụng

Trong game, sử dụng các lệnh sau:

- `/extract item` - Trích xuất vật phẩm đang cầm
- `/extract block` - Trích xuất khối đang nhìn
- `/extract entity` - Trích xuất thực thể đang nhìn
- `/extract all` - Tự động phát hiện và trích xuất

Hoặc sử dụng phím tắt:
- **K** - Auto extract (Entity → Block → Item)
- **L** - Extract toàn bộ creative tab đang mở
- **J** - Extract block variants cho Blockbench

## Đầu ra

Assets được trích xuất vào `.minecraft/extracted_assets/` với cấu trúc:

```
extracted_assets/
└── [mod_id]/
    ├── items/
    ├── blocks/
    └── entities/
```

## Tài liệu

- [Kiến trúc](./ARCHITECTURE.md)
- [Cấu trúc Output](./OUTPUT_STRUCTURE.md)
- [PRD](./prd.md)

## License

MIT License
