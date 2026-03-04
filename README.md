# NeoAssetExtractor

Mod trích xuất tài nguyên đồ họa (Models, Textures, Animations) từ Minecraft trong thời gian thực.

## Tính năng

- ✅ Trích xuất Items (vật phẩm)
- ✅ Trích xuất Blocks (khối)
- ✅ Trích xuất Entities (thực thể)
- 🚧 Hỗ trợ GeckoLib/AzureLib (đang phát triển)
- 🚧 Xử lý Item Overrides (cung, nỏ, la bàn)
- 🚧 Xử lý Armor Layers
- 🚧 VRAM Texture Dumping

## Cài đặt

1. Cài đặt Java 21 JDK
2. Clone repository này
3. Chạy `./gradlew build` (Linux/Mac) hoặc `gradlew.bat build` (Windows)
4. File mod sẽ được tạo trong `build/libs/`

## Phát triển

### Setup môi trường

```bash
# Generate IDE files
./gradlew genIntellijRuns  # Cho IntelliJ IDEA
./gradlew genEclipseRuns   # Cho Eclipse
```

### Chạy game trong IDE

Sau khi generate, bạn sẽ thấy các run configurations:
- `runClient` - Chạy Minecraft client
- `runServer` - Chạy dedicated server
- `runData` - Generate data

## Sử dụng

Trong game, sử dụng các lệnh sau:

- `/extract item` - Trích xuất vật phẩm đang cầm
- `/extract block` - Trích xuất khối đang nhìn
- `/extract entity` - Trích xuất thực thể đang nhìn
- `/extract all` - Tự động phát hiện và trích xuất

## Cấu trúc dự án

```
src/main/java/com/neoassetextractor/
├── NeoAssetExtractor.java          # Main mod class
├── command/
│   └── ExtractCommand.java         # Command handler
├── extractor/
│   ├── ItemExtractor.java          # Item extraction logic
│   ├── BlockExtractor.java         # Block extraction logic
│   └── EntityExtractor.java        # Entity extraction logic
└── util/
    └── AssetWriter.java            # File writing utilities
```

## Đầu ra

Assets được trích xuất vào `.minecraft/extracted_assets/` với cấu trúc:

```
extracted_assets/
└── [mod_id]/
    ├── items/
    ├── blocks/
    └── entities_and_3d/
```

## Bước tiếp theo

Xem file `prd.md` để hiểu đầy đủ các tính năng cần implement.

### TODO List

- [ ] Implement ItemExtractor với đầy đủ tính năng
  - [ ] Trích xuất model JSON và textures
  - [ ] Xử lý Item Overrides (bow, crossbow, compass)
  - [ ] Xử lý Armor Layers
  - [ ] VRAM texture dumping cho generated items
- [ ] Implement BlockExtractor
  - [ ] Trích xuất blockstate JSON
  - [ ] Trích xuất block model và textures
  - [ ] Xử lý tinted blocks (grass, leaves, water)
- [ ] Implement EntityExtractor
  - [ ] Phát hiện GeckoLib/AzureLib entities
  - [ ] Trích xuất .geo.json và .animation.json
  - [ ] Xử lý vanilla Java entities (texture only)
- [ ] Tích hợp GeckoLib API
- [ ] Xử lý error và logging đầy đủ

## License

MIT License

## Tài liệu tham khảo

- [NeoForge Documentation](https://docs.neoforged.net/)
- [GeckoLib Documentation](https://github.com/bernie-g/geckolib)
- PRD đầy đủ: `prd.md`
