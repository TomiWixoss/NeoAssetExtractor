# NeoAssetExtractor - Architecture Documentation

## Kiến trúc tổng quan

NeoAssetExtractor được thiết kế theo kiến trúc enterprise-grade với separation of concerns, dễ mở rộng và maintain.

## Cấu trúc thư mục

```
src/main/java/com/neoassetextractor/
├── core/                           # Core domain objects
│   ├── AssetType.java             # Enum định nghĩa các loại asset
│   ├── ExtractionContext.java     # Context object chứa state
│   └── ExtractionResult.java      # Result object với statistics
│
├── extractor/                      # Extraction logic
│   ├── base/                      # Base classes và interfaces
│   │   ├── IAssetExtractor.java   # Interface chung
│   │   └── BaseExtractor.java     # Abstract base với common logic
│   │
│   ├── block/                     # Block extraction
│   │   ├── BlockExtractor.java    # Main coordinator
│   │   ├── BlockstateExtractor.java
│   │   ├── BlockModelExtractor.java
│   │   └── BlockTextureExtractor.java
│   │
│   ├── item/                      # Item extraction
│   │   └── ItemExtractor.java
│   │
│   └── entity/                    # Entity extraction
│       └── EntityExtractor.java
│
├── parser/                         # JSON parsing logic
│   ├── BlockstateParser.java     # Parse blockstate JSON
│   └── ModelParser.java           # Parse model JSON
│
├── writer/                         # File writing logic
│   ├── IAssetWriter.java         # Writer interface
│   ├── JsonWriter.java           # Write JSON files
│   └── TextureWriter.java        # Write texture files
│
├── util/                           # Utility classes
│   ├── AssetWriter.java          # File I/O utilities
│   ├── ResourceUtil.java         # Resource loading utilities
│   └── TextureCapture.java       # Texture capture với tinting
│
└── command/                        # Command handling
    └── ExtractCommand.java        # Command registration và handling
```

## Design Patterns

### 1. Strategy Pattern
- `IAssetExtractor` interface cho phép swap extractors
- Mỗi extractor implement logic riêng

### 2. Template Method Pattern
- `BaseExtractor` định nghĩa skeleton algorithm
- Subclasses override `doExtract()` method

### 3. Facade Pattern
- `BlockExtractor` là facade cho block extraction
- Coordinates `BlockstateExtractor`, `BlockModelExtractor`, `BlockTextureExtractor`

### 4. Single Responsibility Principle
- Mỗi class có một trách nhiệm duy nhất
- `BlockstateParser` chỉ parse blockstate
- `JsonWriter` chỉ write JSON files

## Data Flow

```
Command
  ↓
ExtractionContext (created)
  ↓
Extractor.extract(context)
  ↓
BaseExtractor.extract()
  ↓
doExtract(context, result)
  ↓
Specialized extractors
  ↓
Parsers (extract info from JSON)
  ↓
Writers (write to disk)
  ↓
ExtractionResult (returned)
  ↓
Command (display result)
```

## Mở rộng

### Thêm extractor mới

1. Tạo class implement `IAssetExtractor` hoặc extend `BaseExtractor`
2. Override `canExtract()` và `doExtract()`
3. Register trong command

```java
public class MyExtractor extends BaseExtractor {
    @Override
    public boolean canExtract(ExtractionContext context) {
        return true; // Your logic
    }
    
    @Override
    protected void doExtract(ExtractionContext context, ExtractionResult result) {
        // Your extraction logic
    }
}
```

### Thêm parser mới

1. Tạo class trong `parser/` package
2. Implement static methods cho parsing logic
3. Sử dụng trong extractors

### Thêm writer mới

1. Implement `IAssetWriter` interface
2. Override `write()` method
3. Sử dụng trong extractors

## Best Practices

1. **Immutability**: `ExtractionContext` fields are final where possible
2. **Error Handling**: All exceptions caught và logged
3. **Logging**: Sử dụng SLF4J logger
4. **Resource Management**: Try-with-resources cho streams
5. **Null Safety**: Check null trước khi sử dụng

## Testing Strategy

1. **Unit Tests**: Test từng parser, writer riêng lẻ
2. **Integration Tests**: Test extractors với mock resources
3. **E2E Tests**: Test commands trong game environment

## Performance Considerations

1. **Caching**: Extracted textures được track để tránh duplicate
2. **Lazy Loading**: Resources chỉ load khi cần
3. **Parallel Processing**: Có thể implement parallel extraction cho multiple assets

## Future Enhancements

1. **Item Overrides**: Extract bow, crossbow variants
2. **Armor Layers**: Extract armor texture layers
3. **GeckoLib Support**: Full .geo.json và .animation.json extraction
4. **Batch Extraction**: Extract tất cả assets của một mod
5. **Export Formats**: Support thêm formats (OBJ, FBX)
6. **GUI**: In-game GUI cho extraction options
