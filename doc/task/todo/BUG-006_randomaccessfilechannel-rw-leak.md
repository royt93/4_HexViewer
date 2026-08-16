# BUG-006 — RandomAccessFileChannel mode RW leak fd/stream

| Trường | Giá trị |
|---|---|
| Loại | Bug |
| Ưu tiên | P1 |
| Điểm | 3 |
| Trạng thái | todo |
| Độ tin cậy | 3/4 |
| Tham chiếu | [BACKLOG.md](../BACKLOG.md) |

## Vấn đề / Mô tả

close() chỉ đóng output stream khi mMode == Mode.WO; mode RW không bao giờ đóng mFileOutputStream/FileChannel tường minh. Hiện đang "ngủ yên" vì openForReadWrite() chưa được gọi ở đâu trong code (@SuppressWarnings("unused")) — nhưng sẽ leak thật ngay khi NF-001 (in-place edit) được implement.

## File liên quan

- `app/src/main/java/com/galaxyjoy/hexviewer/util/io/RandomAccessFileChannel.java:174-224`

## Acceptance Criteria

- [ ] close() phải đóng đầy đủ output stream + channel ở cả 2 mode WO và RW
- [ ] Nếu constructor throw giữa chừng sau khi 1 trong 2 file descriptor đã mở thành công, descriptor đó phải được đóng lại (không leak)
- [ ] Block/verify trước khi NF-001 wire mode RW vào production code path
