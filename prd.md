# 📄 PRODUCT REQUIREMENTS DOCUMENT (PRD)
## Dự án: NeoAssetExtractor (Runtime Asset Extraction Mod)
**Nền tảng:** Minecraft Java Edition  
**Mod Loader:** NeoForge (Phiên bản 1.20.x hoặc mới nhất)  
**Đối tượng sử dụng:** Modder, Resource Pack Creator, 3D Artist.  
**Mục tiêu cốt lõi:** Trích xuất chính xác tài nguyên đồ họa (Model, Texture, Animation) của bất kỳ vật thể nào **đang được render trên màn hình**, giải quyết triệt để vấn đề file `.jar` bị giấu/mã hóa tài nguyên.

---

## 1. TỔNG QUAN HỆ THỐNG (SYSTEM OVERVIEW)
NeoAssetExtractor là một công cụ hoạt động trong thời gian thực (Runtime). Thay vì giải nén file tĩnh, Mod sẽ can thiệp vào bộ nhớ RAM (Hệ thống Render của game) để sao chép dữ liệu đồ họa của Item, Block, Entity và xuất ra ổ cứng dưới định dạng tiêu chuẩn (JSON, PNG) tương thích 100% với phần mềm Blockbench.

---

## 2. TÍNH NĂNG CỐT LÕI (CORE FEATURES)

### 2.1. Trích xuất Khối (Block Extractor)
*   **Kích hoạt:** Người chơi chĩa tâm (Crosshair) vào một Block và gõ lệnh.
*   **Cơ chế:** Dùng `Raycasting` lấy `BlockState` và tọa độ hiện tại.
*   **Đầu ra:**
    *   File `[block]_blockstate.json` (Logic trạng thái).
    *   File `[block].json` (Mô hình 3D tĩnh).
    *   Toàn bộ file `.png` gắn với model đó.
    *   *Xử lý màu nhuộm (Tinting):* Nếu Block có màu sinh ra từ code (VD: Lá cây, cỏ, nước), trích xuất 2 bản Texture: 1 bản gốc (trắng đen) và 1 bản đã áp dụng mã màu (Rendered View).

### 2.2. Trích xuất Vật phẩm (Item Extractor)
*   **Kích hoạt:** Người chơi cầm vật phẩm trên tay chính (Main hand) và gõ lệnh.
*   **Đầu ra cơ bản:** File `[item].json` và file `[item].png`.
*   **Xử lý Nâng cao (BẮT BUỘC):**
    *   **Hỗ trợ ItemOverrides (Cung, Nỏ, La bàn, Súng cơ bản):** Thuật toán tự động quét thuộc tính `overrides` trong file JSON gốc. Đệ quy tải toàn bộ các JSON phụ (`bow_pulling_0`, `bow_pulling_1`...) và tất cả Texture đi kèm.
    *   **Hỗ trợ Áo Giáp (Armor Layers):** Nhận diện class `ArmorItem`. Ngoài việc xuất Texture dạng icon cầm tay, tự động truy xuất và xuất file Texture bọc trên người (Ví dụ: `[tên_giáp]_layer_1.png` và `layer_2.png`).
    *   **Xử lý Item "Tàng hình" / Gen bằng Code (VD: Tinkers' Construct):** Nếu không tìm thấy file `.png` vật lý, hệ thống truy cập vào `TextureAtlasSprite` trong VRAM, dùng `NativeImage` để copy các pixel màu và lưu thành một file `.png` hoàn chỉnh.

### 2.3. Trích xuất Thực thể & Vũ khí 3D (Entity & 3D Item Extractor)
*   **Kích hoạt:** Chĩa tâm vào Entity (hoặc cầm vũ khí 3D) và gõ lệnh.
*   **Cơ chế Phân loại Thông minh:**
    *   **Trường hợp A (Vanilla Java Entity & BEWLR Items):** (Bò, Heo, Đinh ba, Khiên...)
        *   *Nhận diện:* Code được hardcode bằng Java.
        *   *Đầu ra:* **Chỉ xuất được Texture `.png`**. Gửi thông báo in-game cảnh báo người dùng mô hình này được tạo bằng toán học (MatrixStack) nên không có file JSON.
    *   **Trường hợp B (GeckoLib / AzureLib - Quái vật Mod, Súng 3D hiện đại):**
        *   *Nhận diện:* Vật thể implement interface `GeoEntity` hoặc `GeoItem`.
        *   *Đầu ra TRỌN BỘ:* Xuất thành công file `[tên].geo.json` (Mô hình 3D), `[tên].animation.json` (Hoạt ảnh) và `[tên].png` (Ảnh bề mặt). Sẵn sàng Import trực tiếp vào Blockbench.

---

## 3. GIAO DIỆN & TƯƠNG TÁC NGƯỜI DÙNG (UX/UI)

Hệ thống cung cấp các lệnh (Commands) in-game tích hợp Auto-complete:

1.  `/extract item` - Trích xuất vật phẩm trên tay.
2.  `/extract block` - Trích xuất khối đang nhìn.
3.  `/extract entity` - Trích xuất thực thể đang nhìn.
4.  `/extract all` *(Lệnh thông minh)* - Tự động dò tìm theo mức độ ưu tiên: Entity (nếu đang nhìn) -> Block (nếu đang nhìn) -> Item (nếu đang cầm). Trúng cái nào xuất cái đó.

**Hệ thống Phản hồi (Chat Feedback):**
*   🟢 **Thành công:** *"Đã trích xuất thành công [Tên vật thể] vào thư mục extracted_assets!"*
*   🟡 **Cảnh báo (Partial):** *"Vật thể này sử dụng Java Code/BEWLR. Chỉ trích xuất được file Texture (.png)."*
*   🔴 **Lỗi:** *"Không tìm thấy mục tiêu!"* hoặc *"Bạn phải cầm vật phẩm trên tay!"*

---

## 4. CẤU TRÚC THƯ MỤC ĐẦU RA (OUTPUT ARCHITECTURE)

Tự động tạo thư mục gốc trong `.minecraft`, phân loại thông minh theo **Mod ID** để người dùng dễ quản lý:

```text
.minecraft/
└── extracted_assets/
    ├── [mod_id_1]/ (Ví dụ: minecraft, create, alexsmobs)
    │   ├── items/
    │   │   ├── standard/ (Item thường)
    │   │   ├── overrides/ (Cung, nỏ, item nhiều trạng thái)
    │   │   └── armor_layers/ (Texture giáp 3D mặc trên người)
    │   ├── blocks/
    │   │   ├── blockstates/
    │   │   ├── models/
    │   │   └── textures/
    │   └── entities_and_3d/
    │       ├── models/ (.geo.json)
    │       ├── animations/ (.animation.json)
    │       └── textures/ (.png)
    └── [mod_id_2]/
        └── ...
```

---

## 5. YÊU CẦU KỸ THUẬT (TECHNICAL SPECIFICATIONS)

*   **Engine Truy xuất Data:** Sử dụng `Minecraft.getInstance().getResourceManager()` để tìm file.
*   **VRAM Dumper (Cực kỳ quan trọng):** Sử dụng `TextureAtlasSprite` kết hợp `NativeImage` để đọc pixel trực tiếp từ RAM, giải quyết triệt để các mod mã hóa file hoặc không có thư mục Texture.
*   **Raycasting Utils:** Sử dụng `player.pick(distance, 0.0F, false)` cho Block và `ProjectileUtil.getEntityHitResult(...)` cho Entity Hitbox.
*   **Dependencies:**
    *   Khai báo API của **GeckoLib** và **AzureLib** trong `build.gradle` (chế độ `compileOnly` hoặc `implementation` tùy cấu trúc) để gọi được các interface kiểm tra Model.
*   **Quản lý Ngoại lệ (Error Handling):** Sử dụng `try-catch` bọc quanh toàn bộ tiến trình IO (Đọc/Ghi file). Bất kỳ lỗi nào phát sinh (như thiếu file, mã hóa sâu) phải được ghi vào file Log Console, tuyệt đối **KHÔNG ĐƯỢC LÀM CRASH GAME**.

---

## 6. GIỚI HẠN NGOÀI PHẠM VI (OUT OF SCOPE)
*Để đảm bảo tính khả thi của dự án, các tính năng sau được xác nhận là KHÔNG phát triển:*
1.  **Không Dịch ngược Code Java (No Decompilation):** Mod sẽ không cố gắng dịch ngược các file `.class` (như `CowModel.class` của game gốc) thành file JSON tĩnh. Hệ thống toán học `MatrixStack` của GPU không tương thích với cấu trúc của Blockbench. 
2.  **Không Can thiệp file .OBJ:** Nếu một mod (BEWLR) dùng định dạng 3D `.obj` riêng biệt nạp qua code, Mod chỉ ưu tiên lấy Texture `.png`, không chịu trách nhiệm parse file `.obj` đó ra ngoài.

--- 
*Tài liệu PRD này là bản thiết kế cuối cùng, cung cấp cái nhìn toàn diện từ mặt logic sản phẩm đến chi tiết kỹ thuật hệ thống. Đã sẵn sàng cho giai đoạn lập trình (Development Phase).*