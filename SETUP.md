# Hướng dẫn Setup NeoAssetExtractor

## Yêu cầu hệ thống
- Java 21 JDK ✅ (Đã cài)
- Git ✅ (Đã cài)
- 4GB RAM trống (cho Gradle build)

## Bước 1: Build project (LẦN ĐẦU MẤT 5-10 PHÚT)

```bash
# Windows
gradlew.bat build

# Linux/Mac
./gradlew build
```

Lần build đầu tiên sẽ:
- Download Gradle wrapper
- Download Minecraft source code
- Download NeoForge
- Download GeckoLib
- Decompile Minecraft (mất nhiều thời gian nhất)
- Compile mod của bạn

## Bước 2: Setup IDE (IntelliJ IDEA khuyên dùng)

```bash
# Generate IntelliJ project files
gradlew.bat genIntellijRuns

# Hoặc cho Eclipse
gradlew.bat genEclipseRuns
```

## Bước 3: Mở project trong IDE

1. Mở IntelliJ IDEA
2. File → Open → Chọn thư mục NeoAssetExtractor
3. Đợi IDE index xong
4. Bạn sẽ thấy các run configurations:
   - `runClient` - Chạy Minecraft với mod
   - `runServer` - Chạy server
   - `runData` - Generate data

## Bước 4: Test mod

1. Click vào run configuration `runClient`
2. Đợi Minecraft khởi động
3. Tạo world mới hoặc vào world có sẵn
4. Thử lệnh:
   - `/extract item` (cầm một item trên tay)
   - `/extract block` (nhìn vào một block)
   - `/extract entity` (nhìn vào một entity)

## Cấu trúc code hiện tại

```
src/main/java/com/neoassetextractor/
├── NeoAssetExtractor.java          # Main mod class
├── command/
│   └── ExtractCommand.java         # Command registration
├── extractor/
│   ├── ItemExtractor.java          # [WIP] Item extraction
│   ├── BlockExtractor.java         # [WIP] Block extraction
│   └── EntityExtractor.java        # [WIP] Entity extraction
└── util/
    └── AssetWriter.java            # File I/O utilities
```

## Trạng thái hiện tại

✅ Project structure setup
✅ Command system hoạt động
✅ Basic detection (item/block/entity)
🚧 Extraction logic (chưa implement)

## Bước tiếp theo - Implement extraction logic

Xem file `prd.md` để hiểu đầy đủ requirements. Các tính năng cần implement:

1. **ItemExtractor.java**
   - Trích xuất model JSON từ ResourceManager
   - Trích xuất textures
   - Xử lý item overrides (bow, crossbow)
   - Xử lý armor layers
   - VRAM texture dumping

2. **BlockExtractor.java**
   - Trích xuất blockstate JSON
   - Trích xuất block model
   - Xử lý tinted blocks

3. **EntityExtractor.java**
   - Phát hiện GeckoLib entities
   - Trích xuất .geo.json và .animation.json
   - Xử lý vanilla entities (texture only)

## Troubleshooting

### Build bị lỗi "Could not get unknown property 'fg'"
→ Đã fix, dùng `compileOnly` thay vì `fg.deobf`

### Build mất quá lâu
→ Bình thường cho lần đầu. Các lần sau sẽ nhanh hơn nhờ cache.

### Không thấy run configurations trong IDE
→ Chạy lại `gradlew genIntellijRuns` và restart IDE

### Mod không load trong game
→ Check file `logs/latest.log` để xem lỗi

## Tài liệu tham khảo

- [NeoForge Docs](https://docs.neoforged.net/)
- [Minecraft Forge Community Wiki](https://forge.gemwire.uk/)
- [GeckoLib Wiki](https://github.com/bernie-g/geckolib/wiki)
