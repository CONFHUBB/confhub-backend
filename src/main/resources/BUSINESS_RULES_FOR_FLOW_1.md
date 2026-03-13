# BUSINESS RULES FOR FLOW 1: Tạo & Config Conference / Mời Members

> Tham khảo: [CMT3 Docs](https://cmt3.research.microsoft.com/docs/help/index.html)  
> So sánh với: confms-backend hiện tại

---

## 1. TẠO CONFERENCE

### BR-1.1: Thông tin bắt buộc khi tạo conference
| Field | Required | Validation |
|---|:---:|---|
| name | ✅ | Không rỗng, max 255 ký tự |
| acronym | ✅ | VD: "ICSE 2026", unique |
| description | ✅ | Không rỗng |
| location | ✅ | Không rỗng |
| startDate | ✅ | Phải >= ngày hiện tại |
| endDate | ✅ | Phải > startDate |
| status | ✅ | Mặc định = PENDING |
| websiteUrl | ❌ | URL format nếu có |
| bannerImageUrl | ❌ | URL format nếu có |

### BR-1.2: Khi tạo conference
- Người tạo tự động được gán role `CONFERENCE_CHAIR` trong conference
- ConferenceStatus = `PENDING`
- Hệ thống tự tạo 6 ConferenceActivity (disabled):
  - PAPER_SUBMISSION, REVIEWER_BIDDING, REVIEW_SUBMISSION, REVIEW_DISCUSSION, AUTHOR_NOTIFICATION, CAMERA_READY_SUBMISSION

---

## 2. CONFERENCE STATUS FLOW

```
PENDING → SCHEDULED → ONGOING → COMPLETED
                ↓
            CANCELLED
```

### BR-1.3: Rules chuyển status
| Từ | Đến | Điều kiện |
|---|---|---|
| PENDING | SCHEDULED | Phải có ít nhất 1 track + subject areas |
| SCHEDULED | ONGOING | Khi startDate đến hoặc Chair kích hoạt |
| ONGOING | COMPLETED | Khi tất cả activities hoàn tất hoặc Chair kích hoạt |
| Bất kỳ | CANCELLED | Chỉ CONFERENCE_CHAIR mới cancel được |
| COMPLETED/CANCELLED | → | Không thể chuyển tiếp |

---

## 3. QUẢN LÝ TRACKS

### BR-1.4: Track rules
- Conference có thể có 1 hoặc nhiều tracks
- Mỗi track có name riêng (unique trong conference)
- Subject areas được gắn theo conference (dùng chung cho tất cả tracks)
- Mỗi track có `TrackReviewSetting` riêng (double blind, etc.)

### BR-1.5: Subject Areas
- Chair tạo danh sách subject areas cho conference
- Subject areas sử dụng cho: author chọn khi submit, reviewer chọn khi đăng ký interest
- Phải có ít nhất 1 subject area trước khi bật PAPER_SUBMISSION

---

## 4. ACTIVITY TIMELINE

### BR-1.6: Bật/tắt activities
- Mỗi activity có: `isEnabled` (boolean) + `deadline` (nullable datetime)
- Chair bật activity bằng cách set `isEnabled = true` + đặt deadline
- Khi `deadline` qua → activity tự động đóng (cần logic check)
- **Thứ tự bật khuyến nghị:**
  1. PAPER_SUBMISSION (mở nhận bài)
  2. REVIEWER_BIDDING (mở bidding sau khi có bài)
  3. REVIEW_SUBMISSION (mở review sau khi assign)
  4. REVIEW_DISCUSSION (mở thảo luận)
  5. AUTHOR_NOTIFICATION (thông báo kết quả)
  6. CAMERA_READY_SUBMISSION (nộp bản cuối)

### BR-1.7: Ràng buộc activity
- REVIEWER_BIDDING chỉ bật khi PAPER_SUBMISSION đã có papers
- REVIEW_SUBMISSION chỉ bật khi đã có reviewer assignments
- AUTHOR_NOTIFICATION chỉ bật khi reviews đã hoàn tất

---

## 5. MỜI & QUẢN LÝ MEMBERS

### BR-1.8: Roles trong conference
| Role | Ai gán | Quyền |
|---|---|---|
| CONFERENCE_CHAIR | Tự động khi tạo conference | Toàn quyền config |
| PROGRAM_CHAIR | CONFERENCE_CHAIR gán | Quản lý track, assign reviewer |
| REVIEWER | Chair add trực tiếp | Bid papers, review papers |
| AUTHOR | Tự đăng ký khi submit paper | Submit/edit papers |
| ATTENDEE | Tự đăng ký tham dự | Xem conference info |

### BR-1.9: Gán member rules
- 1 user có thể có nhiều roles trong cùng 1 conference
- 1 user có thể tham gia nhiều conferences
- CONFERENCE_CHAIR có thể add PROGRAM_CHAIR + REVIEWER
- PROGRAM_CHAIR có thể add REVIEWER
- Không thể xóa CONFERENCE_CHAIR cuối cùng (luôn phải có ≥1)
- Khi gỡ role REVIEWER: phải kiểm tra reviewer chưa có assignments

---

## 6. REVIEW SETTINGS (per Track)

### BR-1.10: Settings MVP
| Setting | Type | Default | Mô tả |
|---|---|---|---|
| isDoubleBlind | boolean | false | Ẩn danh reviewer + author |
| requireSubjectAreas | boolean | false | Bắt reviewer chọn SA |
| reviewerInviteExpirationDays | int | 7 | Thời hạn lời mời |
| allowReviewerQuota | boolean | false | Reviewer tự đặt quota |
| allowReviewUpdateDuringDiscussion | boolean | false | Sửa review trong discussion |

---

## ⚠️ MIS/MATCH VỚI CODE HIỆN TẠI

> [!IMPORTANT]
> **ConferenceStatus cần xem xét lại.** Enum hiện tại (PENDING, SCHEDULED, ONGOING, COMPLETED, CANCELLED)
> là tự nghĩ ra, CMT3 không có concept "conference status" rõ ràng mà dùng Activity Timeline để kiểm soát phases.
> Có thể cần rethink: thêm/bớt values, hoặc gắn status với activities chặt chẽ hơn.

### ✅ ĐÃ IMPLEMENT
| Business Rule | File |
|---|---|
| BR-1.2: Auto-gán CONFERENCE_CHAIR | `ConferenceServiceImpl.java` |
| BR-1.2: Auto-tạo 6 activities | `ConferenceActivityServiceImpl.java` |
| BR-1.3: Status transition validation (PENDING→SCHEDULED→ONGOING→COMPLETED, CANCELLED) | `ConferenceServiceImpl.java` |
| BR-1.3: Endpoints complete + cancel conference | `ConferenceController.java` |
| BR-1.5: Check tracks + subject areas trước khi bật PAPER_SUBMISSION | `ConferenceActivityServiceImpl.validateActivityDependencies()` |
| BR-1.6: Auto-close expired activities khi lấy danh sách | `ConferenceActivityServiceImpl.getActivitiesByConferenceId()` |
| BR-1.7: Check papers trước REVIEWER_BIDDING, reviews trước REVIEW_SUBMISSION/DISCUSSION | `ConferenceActivityServiceImpl.validateActivityDependencies()` |
| BR-1.8: Enum roles đầy đủ | `ConferenceTrackRole.java` |
| BR-1.9a: Không xóa CONFERENCE_CHAIR cuối cùng | `ConferenceUserTrackServiceImpl.removeRoleFromUser()` |
| BR-1.9b: Check assignments trước khi gỡ REVIEWER | `ConferenceUserTrackServiceImpl.removeRoleFromUser()` |
| BR-1.10: TrackReviewSetting | `TrackReviewSetting.java` |

