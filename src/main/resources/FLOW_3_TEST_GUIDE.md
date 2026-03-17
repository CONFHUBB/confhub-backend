# FLOW 3 TEST GUIDE: Reviewer Bid / Review Bài

> Hướng dẫn chi tiết test toàn bộ Flow 3 theo từng bước.
> Cần chạy Backend (Spring Boot) + Frontend (Next.js) đồng thời.

---

## CHUẨN BỊ

### Chạy Backend
```bash
cd d:\Coding\confms-backendx
./mvnw spring-boot:run
```
Backend chạy tại: `http://localhost:8080`

### Chạy Frontend
```bash
cd D:\Coding\confms-frontend
npm run dev
```
Frontend chạy tại: `http://localhost:3000`

### Dữ liệu cần có trước
1. Tài khoản có role **CONFERENCE_CHAIR**
2. Conference đã tạo với ít nhất **1 track**
3. Ít nhất **3 papers** đã submit (status SUBMITTED)
4. Ít nhất **3 users** có role **REVIEWER** trong conference
5. Review Questions đã tạo cho track (ít nhất 1 required question)

---

## PHASE 1: REVIEW SETTINGS

### Test 1.1: Xem và cập nhật Review Settings
**Bước thực hiện:**
1. Login với tài khoản Chair
2. Vào Conference → Update → Tab "Review Settings"
3. Chọn track từ dropdown
4. Verify hiển thị **15 toggle switches** + 1 input số + 1 textarea
5. Thay đổi từng toggle → Click "Save Changes"
6. Reload page → Verify settings vẫn giữ nguyên

**API test (Postman):**
```
GET /api/v1/tracks/{trackId}/review-settings
PUT /api/v1/tracks/{trackId}/review-settings
```

**Kết quả mong đợi:**
- ✅ Tất cả 17 fields hiển thị đúng
- ✅ Save thành công
- ✅ Reload giữ giá trị

### Test 1.2: Copy Settings giữa tracks
1. Tạo 2 tracks trong cùng conference
2. Config settings ở track A
3. Ở track B → Click "Copy Settings From Another Track" → chọn track A
4. Verify settings track B = settings track A

---

## PHASE 2: BIDDING

### Test 2.1: Reviewer Bidding
**Bước thực hiện:**
1. Chair: Enable activity `REVIEWER_BIDDING` + set deadline (tương lai)
2. Login với tài khoản Reviewer
3. Verify danh sách papers hiển thị (loại trừ WITHDRAWN, DRAFT, REVISION)
4. Bid cho papers: EAGER, WILLING, IN_A_PINCH, NOT_WILLING
5. Verify bid được lưu và hiển thị đúng
6. Thay đổi bid → Verify cập nhật

**API test:**
```
GET /api/v1/bidding/papers-for-bidding?reviewerId={id}&conferenceId={id}
POST /api/v1/bidding
GET /api/v1/bidding/summary/{reviewerId}/conference/{conferenceId}
```

**Kết quả mong đợi:**
- ✅ Không thấy papers WITHDRAWN/DRAFT/REVISION
- ✅ Không bid được paper có conflict
- ✅ Bid values lưu đúng

---

## PHASE 3: ASSIGNMENT

### Test 3.1: Auto-Assign Preview
**Bước thực hiện:**
1. Login Chair
2. Vào Reviewer Assignment tab
3. Config: minReviewersPerPaper=2, maxPapersPerReviewer=3
4. Click "Run Auto-Assign" → Xem preview
5. Verify: không có assignment cho pairs có conflict
6. Click "Confirm" → Assignments lưu vào DB

**API test:**
```
POST /api/v1/conferences/{id}/assignments/auto-assign
POST /api/v1/conferences/{id}/assignments/confirm
```

### Test 3.2: Manual Assign
1. Chair manual assign reviewer cho paper
2. Verify reviewer nhận assignment
3. Thử assign reviewer có conflict → Phải bị block

**API test:**
```
POST /api/v1/conferences/{id}/assignments/manual?paperId={id}&reviewerId={id}
DELETE /api/v1/conferences/{id}/assignments/{reviewId}
```

---

## PHASE 4: REVIEWING

### Test 4.1: Review Submission
**Bước thực hiện:**
1. Chair: Enable activity `REVIEW_SUBMISSION` + set deadline
2. Login Reviewer → Vào danh sách papers assigned
3. Click vào paper → Trả lời review questions
4. Submit review → Verify status chuyển ASSIGNED → IN_PROGRESS → COMPLETED
5. Verify totalScore được tính

**API test:**
```
GET /api/v1/review/reviewer/{reviewerId}/conference/{conferenceId}
POST /api/v1/review-answers/bulk
```

### Test 4.2: Review Read-Only (BR-3.28) ❗ MỚI
1. Chair: Toggle paper thành Read-Only

**API test:**
```
PUT /api/v1/paper/{id}/review-read-only?readOnly=true
```

2. Reviewer thử sửa review → Phải bị block (nếu đã implement logic check)

---

## PHASE 5: DISCUSSION

### Test 5.1: Enable Discussion per paper (BR-3.30) ❗ MỚI
1. Chair: Enable activity `REVIEW_DISCUSSION`
2. Chair: Enable discussion cho paper cụ thể

**API test:**
```
PUT /api/v1/paper/{id}/discussion?enabled=true
PUT /api/v1/paper/bulk-discussion?enabled=true
Body: [1, 2, 3]  (paper IDs)
```

### Test 5.2: Discussion Comments
1. Reviewer post discussion comment cho paper
2. Other reviewer reply (threaded)
3. Verify visibility rules

**API test:**
```
POST /api/v1/review-comments
GET /api/v1/review-comments/review/{reviewId}
```

---

## PHASE 6: DECISION

### Test 6.1: Meta-Review
1. Chair/PC tạo meta-review cho paper
2. Set decision: APPROVE, REJECT, hoặc REVISION
3. Verify paper status tự động cập nhật

**API test:**
```
POST /api/v1/meta-review
```

### Test 6.2: Bulk Paper Status Update (BR-3.43) ❗ MỚI
1. Chair bulk update status cho nhiều papers cùng lúc

**API test:**
```
PUT /api/v1/paper/bulk-status
Body: [
  {"id": 1, "status": "ACCEPTED"},
  {"id": 2, "status": "REJECTED"},
  {"id": 3, "status": "REVISION"}
]
```

**Kết quả mong đợi:**
- ✅ Status transitions hợp lệ (UNDER_REVIEW → ACCEPTED/REJECTED/REVISION)
- ❌ Invalid transitions bị reject (VD: SUBMITTED → ACCEPTED)

### Test 6.3: Paper Status Flow ❗ MỚI
Verify toàn bộ flow:
```
DRAFT → SUBMITTED → UNDER_REVIEW → ACCEPTED → PUBLISHED
                                 → REJECTED
                                 → REVISION → UNDER_REVIEW (re-review)
Bất kỳ (trừ PUBLISHED) → WITHDRAWN
```

**API test tuần tự:**
```
PUT /api/v1/paper/status/{id}  Body: {"status": "SUBMITTED"}
PUT /api/v1/paper/status/{id}  Body: {"status": "UNDER_REVIEW"}
PUT /api/v1/paper/status/{id}  Body: {"status": "REVISION"}
PUT /api/v1/paper/status/{id}  Body: {"status": "UNDER_REVIEW"}  (re-review)
PUT /api/v1/paper/status/{id}  Body: {"status": "ACCEPTED"}
PUT /api/v1/paper/status/{id}  Body: {"status": "PUBLISHED"}
```

---

## PHASE 7: POST-DECISION (TBD)

> Các phần này nằm trong Sprint 4, sẽ bổ sung test khi implement.

### Test 7.1: Revision Cycle
- Author upload revised paper khi status = REVISION
- Chair re-review → set final status

### Test 7.2: Camera-Ready
- Author upload camera-ready khi status = ACCEPTED
- Paper → PUBLISHED sau deadline

---

## CONFLICT MANAGEMENT

> Tham khảo CMT3: [Manage Conflicts](https://cmt3.research.microsoft.com/docs/help/chair/conflicts.html)

### Test C.1: Xem danh sách conflicts
1. Login Chair → Conference → Update → Sidebar → **Conflict Management**
2. Verify hiển thị bảng conflicts (Paper, User, Type, Action)
3. Verify summary: total conflicts, papers involved, users involved

**API test:**
```
GET /api/v1/paper-conflict/conference/{conferenceId}
```

### Test C.2: Thêm conflict mới
1. Click "Add Conflict"
2. Chọn Paper (dropdown có search), User (dropdown có search), Type
3. Verify 7 conflict types hiển thị: Co-Author, Personal, Domain, Colleague, Collaborator, Thesis Advisor, Relative/Friend
4. Submit → Verify conflict xuất hiện trong bảng
5. Thử thêm trùng (cùng paper + user) → Phải báo lỗi "This conflict already exists"

**API test:**
```
POST /api/v1/paper-conflict
Body: {"paperId": 1, "userId": 5, "conflictType": "THESIS_ADVISOR"}
```

### Test C.3: Xóa conflict
1. Click 🗑️ trên row conflict
2. Verify conflict bị xóa khỏi bảng

**API test:**
```
DELETE /api/v1/paper-conflict/{id}
```

### Test C.4: Query conflicts theo paper
**API test:**
```
GET /api/v1/paper-conflict/paper/{paperId}
```
Kết quả: Danh sách conflicts chỉ cho paper đó

### Test C.5: Conflict trong Bidding/Assignment
1. Tạo conflict giữa reviewer A và paper X
2. **Bidding**: Login reviewer A → Verify paper X **ẩn** khỏi bidding list
3. **Auto-assign**: Chạy auto-assign → Verify reviewer A **không được** assign cho paper X
4. **Manual assign**: Chair thử manual assign reviewer A cho paper X → Phải bị **block**

### Test C.6: Domain Conflict (auto-detect)
1. Có reviewer email `reviewer@university.edu` và author email `author@university.edu`
2. Chạy auto-assign → Verify pair này bị skip (DomainConflictUtil tự phát hiện)
3. Verify public domains (gmail.com, hotmail.com, etc.) **KHÔNG tạo** domain conflict

---

## BẢNG TỔNG HỢP API MỚI

| Method | Endpoint | Mô tả | Sprint |
|---|---|---|---|
| PUT | `/api/v1/paper/{id}/review-read-only?readOnly=` | Toggle review read-only | 1 |
| PUT | `/api/v1/paper/{id}/discussion?enabled=` | Toggle discussion per paper | 1 |
| PUT | `/api/v1/paper/bulk-status` | Bulk update paper status | 1 |
| PUT | `/api/v1/paper/bulk-discussion?enabled=` | Bulk toggle discussion | 1 |

## BẢNG TỔNG HỢP THAY ĐỔI

| Component | Thay đổi | Files |
|---|---|---|
| PaperStatus | +REVISION | `PaperStatus.java` |
| TrackReviewSetting | +10 fields | Entity, DTO, ServiceImpl, Frontend type + UI |
| Paper | +isReviewReadOnly, +isDiscussionEnabled | Entity |
| ConflictType | +4 values | Enum |
| ReviewComment | +parentCommentId, +isDiscussionPost, +paper, +user, +title | Entity |
| PaperController | +4 endpoints | Controller |
| PaperServiceImpl | +4 methods, +REVISION transitions | Service |
| PaperUpdateStatusDTO | +id field | DTO |

---

## LƯU Ý KHI TEST

1. **Database**: Hibernate auto-update (`spring.jpa.hibernate.ddl-auto=update`) sẽ tự tạo columns mới. Nếu dùng `validate`, cần chạy migration script.
2. **Data cũ**: Nếu DB đã có data, columns mới sẽ nhận default values (false cho boolean, null cho nullable).
3. **Review Settings**: 10 settings mới sẽ hiện default=false trên UI. Phải save lại mới lưu vào DB.
4. **Bulk Status**: Cần gửi array JSON, mỗi item phải có `id` + `status`.
