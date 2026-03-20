# BUSINESS RULES FOR FLOW 4: Meta-Review / Quyết Định Accept-Reject / Author Notification

> Tham khảo: [CMT3 Meta-Review](https://cmt3.research.microsoft.com/docs/help/chair/chairtasks-metareview.html),
> [CMT3 Author Notification](https://cmt3.research.microsoft.com/docs/help/chair/author-notification.html),
> [CMT3 Camera-Ready](https://cmt3.research.microsoft.com/docs/help/chair/camera-ready.html)  
> So sánh với: confms-backend hiện tại

> [!IMPORTANT]
> **ConfMS KHÔNG có role Meta-Reviewer riêng.** Program Chair / Conference Chair trực tiếp thực hiện meta-review.
> Trong CMT3, meta-reviewer (Area Chair) là role riêng — ConfMS đơn giản hóa bằng cách gộp vào Program Chair.

---

## 1. PRECONDITIONS (Điều kiện tiên quyết)

### BR-4.1: Khi nào Chair bắt đầu meta-review
- Reviews phải **đủ số lượng** (≥ minReviewersPerPaper đã COMPLETED cho mỗi paper)
- `ConferenceActivity` loại `REVIEW_SUBMISSION` nên đã đóng hoặc tất cả reviews đã COMPLETED
- Chair có thể bắt đầu meta-review **trước khi tất cả reviews hoàn tất** (partial decision)
- Discussion phase (nếu có) nên đã kết thúc trước khi quyết định cuối

### BR-4.2: Ai có quyền meta-review
| Role | Quyền |
|---|---|
| CONFERENCE_CHAIR | Tạo/sửa/xóa meta-review cho **tất cả** papers |
| PROGRAM_CHAIR | Tạo/sửa/xóa meta-review cho papers **trong track được gán** |
| REVIEWER | ❌ Không có quyền |
| AUTHOR | ❌ Không có quyền |

---

## 2. META-REVIEW PROCESS

### BR-4.3: Quy trình meta-review (Chair)
```
1. Chair vào Meta-Review Console
2. Xem danh sách papers + review aggregates (avg score, review count)
3. Chọn paper → Xem chi tiết:
   a. Tất cả reviews (read-only) — scores + comments
   b. Discussion threads (nếu có)
   c. Review aggregate scores
4. Đưa ra quyết định: APPROVE / REJECT / REVISION
5. Nhập reason/comments (bắt buộc)
6. Submit meta-review → Paper status tự động cập nhật
7. Authors được thông báo tự động
```

### BR-4.4: Meta-Review entity
| Field | Type | Required | Mô tả |
|---|---|---|---|
| paper | Paper | ✅ | Paper being decided |
| user | User | ✅ | Chair making the decision |
| finalDecision | Decision | ✅ | APPROVE / REJECT / REVISION |
| reason | String | ✅ | Lý do quyết định (không được rỗng) |

### BR-4.5: Decision values
| Decision | Ý nghĩa | Paper Status sau quyết định |
|---|---|---|
| `APPROVE` | Chấp nhận paper | `ACCEPTED` |
| `REJECT` | Từ chối paper | `REJECTED` |
| `REVISION` | Yêu cầu sửa lại | Giữ nguyên (mở revision cycle) |

### BR-4.6: Meta-review constraints
- **1 meta-review per paper** — Nếu đã có meta-review, Chair update (không tạo mới)
- Reason/comments **bắt buộc** — Chair phải giải thích lý do
- Meta-review **có thể update** — Chair thay đổi decision → paper status cập nhật theo
- **Không thể meta-review paper WITHDRAWN** — Papers withdrawn bị loại trừ
- **Chỉ meta-review papers UNDER_REVIEW** — Papers phải ở status UNDER_REVIEW (đã có reviewer assigned)

---

## 3. PAPER STATUS UPDATE (BR-3.21 mở rộng)

### BR-4.7: Auto-update paper status khi meta-review
```
Meta-review created/updated:
  Decision = APPROVE → Paper.status = ACCEPTED
  Decision = REJECT  → Paper.status = REJECTED
  Decision = REVISION → Paper.status giữ nguyên (UNDER_REVIEW)
```

### BR-4.8: Status transition validation
| Từ | Đến | Khi nào |
|---|---|---|
| UNDER_REVIEW → ACCEPTED | Meta-review decision = APPROVE |
| UNDER_REVIEW → REJECTED | Meta-review decision = REJECT |
| ACCEPTED → REJECTED | Chair thay đổi decision → REJECT |
| REJECTED → ACCEPTED | Chair thay đổi decision → APPROVE |
| ACCEPTED/REJECTED → UNDER_REVIEW | Chair xóa meta-review (revert) |

### BR-4.9: Xóa meta-review
- Khi xóa meta-review → **revert paper status về UNDER_REVIEW**
- Gửi notification cho authors: "Decision has been reverted"
- Chair phải confirm trước khi xóa

---

## 4. AUTHOR NOTIFICATION

### BR-4.10: Auto-notification khi meta-review
Khi meta-review được tạo/cập nhật:
1. Tìm tất cả authors của paper (qua `PaperAuthor`)
2. Tạo `Notification` cho mỗi author:
   - Title: "Paper {ACCEPTED/REJECTED}: {Paper.title}"
   - Message: Chi tiết quyết định + reason
   - Type: `PAPER_DECISION`
   - Link: `/conference/{conferenceId}/author`

### BR-4.11: Notification rules
- Decision = REVISION → **KHÔNG gửi notification tự động** (Chair gửi thủ công)
- 1 author có nhiều papers → nhận notification riêng cho mỗi paper
- Notification chỉ gửi cho **registered users** (co-authors chưa đăng ký → bỏ qua)
- Update decision → gửi notification **mới** (không sửa notification cũ)

### BR-4.12: Email notification (Post-MVP enhancement)
- Ngoài in-app notification → gửi email cho authors
- Email template per status:
  - Accept: congratulations + camera-ready instructions
  - Reject: decision + review comments
  - Revision: decision + review comments + revision deadline
- Template placeholders: `{Paper.Title}`, `{Paper.Id}`, `{Recipient.Name}`, `{Conference.Name}`, `{Decision.Reason}`

---

## 5. REVIEW AGGREGATES CHO META-REVIEW

### BR-4.13: Dữ liệu Chair cần thấy khi quyết định
Cho mỗi paper, Chair cần xem:

| Dữ liệu | Nguồn | Mô tả |
|---|---|---|
| Paper info | Paper entity | Title, abstract, keywords, track |
| Review count | Count(Review where paperId = P) | Số reviews hiện có |
| Completed reviews | Count(Review where status = COMPLETED) | Số reviews đã hoàn tất |
| Avg total score | AVG(Review.totalScore where COMPLETED) | Điểm trung bình |
| Per-question aggregates | AVG(ReviewAnswer.numericValue) per question | Aggregate per review question |
| Individual reviews | Review + ReviewAnswers | Chi tiết từng review (read-only) |
| Discussion threads | ReviewComment | Thảo luận giữa reviewers (nếu có) |
| Current status | Paper.status | Status hiện tại của paper |
| Existing decision | ReviewMetaReview | Decision hiện tại (nếu đã quyết định) |

### BR-4.14: Hiển thị papers trên Meta-Review Console
- **Hiện tất cả papers** trong conference (trừ WITHDRAWN, DRAFT)
- Sắp xếp theo: undecided first, then by avg score descending
- Filter: by track, by status (pending/decided), by reviewer count
- Quick stats: total papers, decided, pending, accepted, rejected

---

## 6. BULK DECISION

### BR-4.15: Batch accept/reject
- Chair có thể chọn nhiều papers → apply cùng 1 decision
- Bulk decision vẫn yêu cầu reason (1 reason chung cho tất cả selected papers)
- Mỗi paper tạo 1 meta-review record riêng
- Notifications gửi riêng cho mỗi paper

### BR-4.16: Suggest decision dựa trên scores
| Avg Score Range | Suggested Decision | Visual |
|---|---|---|
| ≥ 3.5 | APPROVE (Strong) | 🟢 |
| 3.0 — 3.49 | APPROVE (Borderline) | 🟡 |
| 2.0 — 2.99 | REVISION recommended | 🟠 |
| < 2.0 | REJECT | 🔴 |

> **Lưu ý**: Đây chỉ là **gợi ý**, Chair luôn có quyền override.

---

## 7. REVISION CYCLE (Khi Decision = REVISION)

### BR-4.17: Revision flow
```
1. Chair set decision = REVISION cho paper
2. Chair gửi Author Notification (thủ công) với review comments
3. Chair enable "Revision Submission" trong Activity Timeline + set deadline
4. Author xem reviews (nếu Chair bật visibility)
5. Author upload revised manuscript qua submission page
6. Chair/Reviewers review lại (re-review round)
7. Chair đưa ra final decision: APPROVE hoặc REJECT
8. Gửi final Author Notification
```

### BR-4.18: Revision rules
- Paper status **giữ UNDER_REVIEW** khi decision = REVISION
- Author chỉ thấy revision upload link khi paper có decision REVISION
- Reviewers có thể giữ nguyên hoặc Chair assign reviewers mới
- Re-review process giống round 1 (submit review → discussion → meta-review)
- **Chỉ 1 revision cycle** trong MVP (không hỗ trợ multiple rounds)

---

## 8. CAMERA-READY (Sau Accept)

### BR-4.19: Camera-Ready flow
```
1. Paper.status = ACCEPTED (sau meta-review APPROVE)
2. Chair enable "Camera Ready Submission" trong Activity Timeline + set deadline
3. Author upload final version (camera-ready files)
4. Chair review camera-ready files
5. Chair approve → Paper.status = PUBLISHED
```

### BR-4.20: Camera-Ready rules
- Chỉ papers có status `ACCEPTED` mới eligible
- Author upload qua submission page (reuse PaperFile upload)
- Chair có thể reject camera-ready (yêu cầu sửa lại)
- Sau deadline → locked, không upload được
- Khi tất cả accepted papers đã PUBLISHED → Conference có thể COMPLETED

---

## 9. DATA VISIBILITY TRONG META-REVIEW PHASE

### BR-4.21: Chair visibility
- Chair thấy **tất cả** reviews, review answers, discussion threads, reviewer identities
- Chair thấy review aggregates per paper
- Chair thấy meta-review history (nếu decision đã thay đổi)

### BR-4.22: Author visibility (sau notification)
| Data | Trước Notification | Sau Notification |
|---|---|---|
| Paper status | ❌ Ẩn | ✅ Hiện |
| Reviews content | ❌ Ẩn | Tùy Chair config |
| Meta-review reason | ❌ Ẩn | Tùy Chair config |
| Reviewer identity | ❌ Ẩn | ❌ Luôn ẩn (double blind) |

### BR-4.23: Reviewer visibility
- Reviewer **KHÔNG thấy** meta-review quyết định (trừ khi Chair bật setting)
- Reviewer thấy paper status khi `allowReviewerSeeStatusBeforeNotification = true`
- Reviewer thấy reviews người khác khi `allowOthersReviewAccessAfterSubmit = true`

---

## 10. FRONTEND — META-REVIEW CONSOLE

### BR-4.24: UI Components cần xây

| Component | Mô tả | Vị trí |
|---|---|---|
| Meta-Review Tab | Tab mới trong Conference Update page | `reviewer-assignment.tsx` đổi thành tab layout |
| Papers Decision Table | Bảng papers + aggregates + status + decision | Nội dung chính |
| Review Summary Panel | Xem tất cả reviews per paper (read-only) | Dialog/Drawer |
| Decision Form | Dropdown (APPROVE/REJECT/REVISION) + textarea reason | Inline hoặc Dialog |
| Decision Stats Dashboard | Quick stats: total/decided/pending/accepted/rejected | Header cards |

### BR-4.25: Papers Decision Table columns
| Column | Mô tả |
|---|---|
| # | Paper ID |
| Title | Paper title (link to details) |
| Track | Track name |
| Reviews | {completed}/{total} reviews |
| Avg Score | Average total score (nếu có) |
| Status | Current paper status badge |
| Decision | Current decision badge (nếu đã quyết định) + nút Edit |
| Action | View Reviews / Make Decision / Edit Decision |

---

## ⚠️ MIS/MATCH VỚI CODE HIỆN TẠI

### ✅ ĐÃ IMPLEMENT
| Business Rule | File |
|---|---|
| BR-4.4: ReviewMetaReview entity | `ReviewMetaReview.java` |
| BR-4.5: Decision enum (APPROVE, REJECT, REVISION) | `Decision.java` |
| BR-4.7: Auto-update paper status (BR-3.21) | `ReviewMetaReviewServiceImpl.updatePaperStatusFromDecision()` |
| BR-4.10: Auto-notification cho authors | `ReviewMetaReviewServiceImpl.notifyAuthorsAboutDecision()` |
| BR-4.11: Skip REVISION notification | `notifyAuthorsAboutDecision()` — Decision.REVISION → return |
| BR-4.4: CRUD API | `ReviewMetaReviewController` — POST/GET/PUT/DELETE |
| BR-4.8: Status transition ACCEPTED ↔ REJECTED | `updatePaperStatusFromDecision()` handles both directions |

### ❌ CHƯA IMPLEMENT
| Business Rule | Mô tả | Ưu tiên |
|---|---|---|
| BR-4.2: Permission check per role | Controller chưa check role Chair/PC | **Cao** |
| BR-4.6: 1 meta-review per paper constraint | Repository chưa có `findByPaperId`, chưa validate unique | **Cao** |
| BR-4.6: Chỉ meta-review papers UNDER_REVIEW | Service chưa validate paper status | Trung bình |
| BR-4.9: Revert status khi xóa meta-review | Service chưa revert paper status khi delete | **Cao** |
| BR-4.13: Papers list với aggregates cho Chair | Cần API endpoint mới | **Cao** |
| BR-4.14: Filter/sort papers cho Meta-Review Console | Cần API endpoint mới | **Cao** |
| BR-4.15: Bulk decision API | Chưa có batch endpoint | Trung bình |
| BR-4.24: Meta-Review Console FE | Chưa có FE page | **Cao** |
| BR-4.25: Papers Decision Table FE | Chưa có FE component | **Cao** |
| BR-4.12: Email notification cho decision | Chỉ có in-app notification | Trung bình |
| BR-4.17-18: Revision cycle | Chưa có revision upload flow | Thấp (post-MVP) |
| BR-4.19-20: Camera-Ready flow | Chưa có camera-ready submission | Thấp (post-MVP) |
| BR-4.22-23: Visibility controls | Chưa có visibility settings | Trung bình |

### 🔧 CẦN SỬA/BỔ SUNG BACKEND
| Item | Mô tả |
|---|---|
| `ReviewMetaReviewRepository` | Thêm `findByPaperId(Integer paperId)`, `findByPaper_Track_Conference_Id(Integer conferenceId)` |
| `ReviewMetaReviewController` | Thêm `GET /paper/{paperId}`, `GET /conference/{conferenceId}` |
| `ReviewMetaReviewResponseDTO` | Đổi `Paper` → `PaperInfo` (DTO nhẹ), `User` → `UserInfo` (tránh circular ref + data leak) |
| `ReviewMetaReviewServiceImpl` | Thêm validate: unique per paper, paper status check, role permission |
| `ReviewMetaReviewServiceImpl.deleteReviewMetaReview()` | Thêm logic revert paper status về UNDER_REVIEW |

> **2026-03-20:** Tạo Flow 4 Business Rules dựa trên nghiên cứu CMT3 + phân tích backend hiện tại.
