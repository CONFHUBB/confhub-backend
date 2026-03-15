# 📋 Changelog — Những Thay Đổi Ngày 2026-03-15

> **Ngày:** 2026-03-15  
> **Scope:** Backend + Frontend  

---

## 1. 🔴 XÓA FEATURE: Plagiarism Check

**Lý do:** Tính năng chưa triển khai, bỏ ra khỏi cả BE lẫn FE.

| Layer | File | Thay đổi |
|---|---|---|
| **BE** | `Paper.java` | Xóa field `isPassedPlagiarism` + `@Column` annotation |
| **BE** | `PaperDTO.java` | Xóa field `isPassedPlagiarism` |
| **BE** | `PaperResponseDTO.java` | Xóa field `isPassedPlagiarism` |
| **BE** | `PaperServiceImpl.java` | Xóa setter + builder reference |
| **FE** | `types/paper.ts` | Xóa `isPassedPlagiarism` khỏi `PaperResponse` + `CreatePaperRequest` |
| **FE** | `types/submission-form.ts` | Xóa `isPassedPlagiarism` khỏi `PaperSubmissionRequest` |
| **FE** | `paper/page.tsx` | Xóa UI hiển thị Plagiarism status |
| **FE** | `submit/page.tsx` | Xóa `isPassedPlagiarism: false` khỏi payload |

> ⚠️ Cần chạy migration SQL: `ALTER TABLE papers DROP COLUMN is_passed_plagiarism;`

---

## 2. 🔔 Notification System — Hoàn Thiện

Đã triển khai đầy đủ notification cho tất cả hành động quan trọng trong cả 3 flows:

### BE Notifications (gửi khi):
| Hành động | Notification type |
|---|---|
| Assign user vào conference | `ROLE_ASSIGNED` |
| Xóa user khỏi conference | `ROLE_REMOVED` |
| User accept invitation | `INVITATION_ACCEPTED` |
| User decline invitation | `INVITATION_DECLINED` |
| Tạo conference thành công | `CONFERENCE_CREATED` |
| Nộp bài thành công | `PAPER_SUBMITTED` |
| Paper withdrawn | `PAPER_WITHDRAWN` |
| Review assigned | `REVIEW_ASSIGNED` |
| Review completed | `REVIEW_COMPLETED` |

### FE Notification Bell:
- `notification-bell.tsx`: Dropdown hiển thị notifications real-time
- Polling mỗi 30 giây
- Mark as read, mark all as read
- Navigate to link khi click notification

---

## 3. 🎨 FE — Reviewer Pages (Flow 3) Đã Có FE

Các trang sau đã được tạo mới:

| Trang | Path | Chức năng |
|---|---|---|
| Reviewer Select | `/conference/reviewer-select` | Chọn conference để review |
| Reviewer Console | `/conference/[id]/reviewer` | Dashboard reviewer |
| Subject Areas / Interests | `/conference/[id]/reviewer/interests` | Chọn SA + expertise level |
| Bidding | `/conference/[id]/reviewer/bidding` | Bid papers (EAGER/WILLING/IN_A_PINCH/NOT_WILLING) |
| Review Detail | `/conference/[id]/reviewer/review/[reviewId]` | Review form + answers |

---

## 4. 🎨 FE — Cải Tiến UI/UX

### 4.1 My Papers Page Redesign
- Summary stats cards (Total, Under Review, Accepted, Needs Action)
- Card-based layout thay table
- Status badges có màu + icon riêng cho 8 status
- Action required indicators (amber highlight cho DRAFT, CAMERA_READY)

### 4.2 Chuẩn Hóa Buttons
- Thêm variant `warning` vào `Button` component
- Đồng bộ tất cả back buttons sang pattern `← Back to [destination]`
- Refactor custom className → dùng đúng variant (`destructive`, `warning`, `outline`)

### 4.3 Dịch 100% FE Sang Tiếng Anh
- 7 files đã dịch từ tiếng Việt sang tiếng Anh
- Quét lại: 0 string tiếng Việt còn lại trong `app/` directory

---

## 5. 📊 Cập Nhật Status MVP

| Item | Status cũ | Status mới |
|---|---|---|
| 3.1 Bid on papers | 🔧 | ✅ |
| 3.3 Submit reviews | 🔧 | ✅ |
| 3.5 Edit own reviews | 🔧 | ✅ |
| 4.6 Withdraw submission | 🔧 | ✅ |
| 4.7 Plagiarism check | 📦 | 🚫 (removed) |
| 4.8 View submission status | ✅ | ✅ (redesigned) |
| 4.12 Notifications | 📦 | ✅ |
