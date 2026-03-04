# Spawn Egg Rendering - ItemColors Method

## Vấn Đề
Spawn eggs trong Minecraft không có texture tĩnh. Chúng sử dụng 2 template layers (`spawn_egg.png` và `spawn_egg_overlay.png`) với màu động được apply runtime qua `ItemColors`. Khi extract texture thông thường, bạn chỉ nhận được template trắng đen.

## Giải Pháp - ItemColors + Template Blending
`SpawnEggRenderer` sử dụng approach đơn giản và hiệu quả:

### Cách Hoạt Động
1. **Load 2 Template Textures**: `spawn_egg.png` (base) và `spawn_egg_overlay.png` (spots)
2. **Lấy Màu từ ItemColors**: 
   - Layer 0 = Base color (màu nền)
   - Layer 1 = Overlay color (màu chấm)
3. **Apply Color Tint**: Nhân từng pixel template với màu tương ứng
4. **Alpha Blend**: Blend layer 1 lên layer 0
5. **Export PNG**: Kết quả là spawn egg với màu thực tế

### Code Đơn Giản
```java
// Lấy màu từ game
ItemColors itemColors = Minecraft.getInstance().getItemColors();
int color0 = itemColors.getColor(itemStack, 0); // Base
int color1 = itemColors.getColor(itemStack, 1); // Overlay

// Apply tint: multiply template RGB với color RGB
int newR = (templateR * colorR) / 255;
int newG = (templateG * colorG) / 255;
int newB = (templateB * colorB) / 255;
```

## Kết Quả
```
minecraft/items/creeper_spawn_egg/
├── models/
│   └── creeper_spawn_egg.json
└── textures/
    ├── spawn_egg.png                      (template layer 1 - base)
    ├── spawn_egg_overlay.png              (template layer 2 - spots)
    └── creeper_spawn_egg_rendered.png     (RENDERED với màu thực tế)
```

## Sử Dụng
```
/extract item (cầm spawn egg)
```

File `*_rendered.png` sẽ có màu chính xác như trong game.

## Ưu Điểm So Với Offscreen Rendering

### Approach Cũ (Framebuffer - Thất Bại)
- ❌ Phức tạp: Cần quản lý OpenGL state, framebuffer, viewport
- ❌ Thread-dependent: Phải chạy trên render thread
- ❌ Dễ lỗi: Framebuffer có thể fail im lặng
- ❌ Overhead: Tạo/destroy framebuffer mỗi lần

### Approach Mới (ItemColors - Thành Công)
- ✅ Đơn giản: Chỉ cần load texture + apply màu
- ✅ Thread-safe: Chạy được ở bất kỳ thread nào
- ✅ Reliable: Không phụ thuộc OpenGL state
- ✅ Hiệu quả: Không có GPU overhead

## Technical Details

### ItemColors System
```java
ItemColors itemColors = Minecraft.getInstance().getItemColors();
int baseColor = itemColors.getColor(itemStack, 0);    // Layer 0
int overlayColor = itemColors.getColor(itemStack, 1); // Layer 1
```

### Color Tinting Algorithm
```java
// Multiply template pixel với color
int newR = (templateR * colorR) / 255;
int newG = (templateG * colorG) / 255;
int newB = (templateB * colorB) / 255;
```

### Alpha Blending
```java
// Blend overlay lên base
float alpha = overlayAlpha / 255.0f;
int blendedR = (int)(overlayR * alpha + baseR * (1 - alpha));
int blendedG = (int)(overlayG * alpha + baseG * (1 - alpha));
int blendedB = (int)(overlayB * alpha + baseB * (1 - alpha));
```

## Ưu Điểm
1. ✅ Texture giống 100% với trong game
2. ✅ Đơn giản, dễ maintain
3. ✅ Không phụ thuộc OpenGL/render thread
4. ✅ Hoạt động với mọi spawn egg (vanilla + mod)
5. ✅ Tự động lấy màu từ ItemColors registry

## Lessons Learned
- **KISS Principle**: Giải pháp đơn giản thường tốt hơn phức tạp
- **Avoid Premature Optimization**: Framebuffer rendering là overkill
- **Use Game APIs**: ItemColors đã cung cấp đủ thông tin cần thiết
- **Test Early**: Nên test approach đơn giản trước khi làm phức tạp

## Future Improvements
- [ ] Cache rendered textures để tránh re-render
- [ ] Support custom tint providers từ mods
- [ ] Batch processing nhiều spawn eggs cùng lúc


