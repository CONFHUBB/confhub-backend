# FLOW 3 IMPLEMENTATION PLAN: Reviewer Bid Bài / Review Bài

> Kế hoạch thực thi toàn bộ Flow 3 dựa trên `BUSINESS_RULES_FOR_FLOW_3.md` (BR-3.1 → BR-3.53)
> So sánh với code hiện tại trong `confms-backend` và `confms-frontend`

---

## TỔNG QUAN

Flow 3 bao gồm **19 sections**, chia thành **7 phases** theo lifecycle:

```
Phase 1: Setup Review     → Review Settings, Review Questions, Conflict Management
Phase 2: Bidding          → Reviewer Subject Areas, Bidding
Phase 3: Assignment       → Auto-Assign, Manual Assign
Phase 4: Reviewing        → Review Submission, Review Read-Only
Phase 5: Discussion       → Per-paper Discussion, Threaded Comments
Phase 6: Decision         → Meta-Review, Paper Status, Author Notification
Phase 7: Post-Decision    → Revision Cycle, Camera-Ready, Publish
```

---

## PHASE 1: SETUP REVIEW ✅ Phần lớn đã có

### 1.1 Review Settings (TrackReviewSetting)

#### Backend — Cần bổ sung 10 fields vào `TrackReviewSetting.java`

| Field cần thêm | Type | Default |
|---|---|---|
| `showReviewerIdentityToOtherReviewer` | Boolean | false |
| `showAggregateColumns` | Boolean | false |
| `allowReviewerSeeStatusBeforeNotification` | Boolean | false |
| `enableAllPapersForDiscussion` | Boolean | false |
| `allowDiscussNonAssignedPapers` | Boolean | false |
| `allowAuthorDiscuss` | Boolean | false |
| `notifyReviewerOnReviewUpdateDuringDiscussion` | Boolean | false |
| `notifyOnManualAssignment` | Boolean | false |
| `doNotShowWithdrawnPapers` | Boolean | false |
| `addReviewerOnInviteAccept` | Boolean | true |

**Files cần sửa:**
- `entity/TrackReviewSetting.java` — Thêm 10 fields
- `dto/TrackReviewSettingDTO.java` — Thêm 10 fields tương ứng
- `service/impl/TrackReviewSettingServiceImpl.java` — Map DTO ↔ Entity
- Migration SQL hoặc để Hibernate auto-update

#### Frontend — Cần cập nhật UI
- `review-settings.tsx` — Thêm 10 toggle switches/checkboxes cho settings mới

---

### 1.2 Review Questions ✅ ĐÃ CÓ

Backend: `ReviewQuestion.java` đã có 6 visibility flags, choices, ordering — đầy đủ theo BR-3.16 + BR-3.53
Frontend: `review-questions-list.tsx`, `review-question-dialog.tsx`, `review-questions-preview.tsx`, `review-questions-copy-dialog.tsx` — đầy đủ

**Không cần thêm gì.**

---

### 1.3 Conflict Management — ✅ Backend + Frontend đã có (MVP scope)

> Tham khảo: [CMT3 Manage Conflicts](https://cmt3.research.microsoft.com/docs/help/chair/conflicts.html),
> [CMT3 Dispute Conflicts](https://cmt3.research.microsoft.com/docs/help/chair/dispute-conflicts.html)

#### Backend — ✅ Đã hoàn thành
- `PaperConflict.java` ✅ (paper + user + conflictType)
- `UserConflict.java` ✅ (user + conflictEmail + reason)
- `ConflictType.java` ✅ (7 values: CO_AUTHOR, PERSONAL, DOMAIN, COLLEAGUE, COLLABORATOR, THESIS_ADVISOR, RELATIVE_FRIEND)
- `DomainConflictUtil.java` ✅ (phát hiện domain conflict, loại trừ public domains)
- `PaperConflictController.java` ✅ (CRUD + query by paper + query by conference)
- `PaperConflictService.java` ✅ (getConflictsByPaperId, getConflictsByConferenceId)
- `UserConflictController.java` ✅ (CRUD per user + toggle active)

#### Frontend — ✅ MVP đã hoàn thành
- `types/conflict.ts` ✅ (7 ConflictType values, DTOs, labels)
- `app/api/conflict.api.ts` ✅ (createPaperConflict, getConflictsByConference, getConflictsByPaper, deletePaperConflict)
- `conflict-management.tsx` ✅ (Chair UI: thêm/xem/xóa conflicts, chọn Paper + User + Type)
- Sidebar tab "Conflict Management" ✅ (trong conference update page)

#### Post-MVP (không thuộc scope hiện tại, theo CMT3):
- Author/Reviewer tự khai báo conflicts (per-submission + per-user)
- Conflict settings per track (enable/lock editing, attestation)
- Dispute Conflicts (PC member dispute → Chair review → keep/delete)
- Import conflicts từ file (bulk import)
- DBLP co-authorship auto-detect

---

## PHASE 2: BIDDING ✅ ĐÃ CÓ

### 2.1 Reviewer Subject Areas ✅
- Backend: `ReviewerInterest.java`, `ReviewerInterestService`, `ReviewerInterestController` — đầy đủ
- Frontend: `reviewer-interest.api.ts` — đầy đủ

### 2.2 Bidding ✅
- Backend: `Bidding.java`, `BiddingService`, `BiddingController` — đầy đủ
- Frontend: `bidding.api.ts`, `bidding.ts` types — đầy đủ

**Cần bổ sung nhỏ:**
- Backend: Lọc thêm papers có status `REVISION` khỏi bidding list (BR-3.4)
- Frontend: Nếu chưa có **Bidding UI page** cho Reviewer → cần tạo

---

## PHASE 3: ASSIGNMENT ✅ ĐÃ CÓ

### 3.1 Auto-Assign ✅
- Backend: `ReviewerAssignmentServiceImpl.java` — auto-assign, load balancing, domain conflict detection
- Frontend: `assignment.api.ts`, `reviewer-assignment.tsx` — đầy đủ

### 3.2 Manual Assign ✅
- Backend: `manualAssign()`, `removeAssignment()` — đầy đủ
- Frontend: `manualAssign()`, `removeAssignment()` in `assignment.api.ts` — đầy đủ

**Cần bổ sung nhỏ:**
- Backend: Khi manual assign + setting `notifyOnManualAssignment = true` → gửi email cho reviewer (BR-3.40)

---

## PHASE 4: REVIEWING ⚠️ Phần lớn đã có, thiếu Read-Only + Discussion integration

### 4.1 Review Process ✅
- Backend: `Review.java`, `ReviewServiceImpl.java` (status transitions, validate required questions, totalScore)
- Frontend: `review.api.ts`, `review.ts` types

### 4.2 Review Answers ✅
- Backend: `ReviewAnswer.java`, `ReviewAnswerService`
- Frontend: `submitAnswer()`, `submitBulkAnswers()`, `getAnswersByReview()`

### 4.3 Review Read-Only ❌ CHƯA CÓ

**Backend cần:**
- Thêm field `Paper.isReviewReadOnly` (Boolean, default false)
- API endpoints:
  - `PUT /papers/{id}/review-read-only` — toggle Read-Only
  - `PUT /papers/bulk-review-read-only` — bulk toggle
- Logic: Nếu `isReviewReadOnly = true` → block update review answers

**Frontend cần:**
- Chair Console: Cột "Review Read-Only" per paper + bulk toggle action
- Logic: Disable review form khi paper is read-only

---

## PHASE 5: DISCUSSION ⚠️ Entity có, logic/UI thiếu nhiều

### 5.1 Per-paper Discussion Control ❌ CHƯA CÓ

**Backend cần:**
- Thêm field `Paper.isDiscussionEnabled` (Boolean, default false)
- API endpoints:
  - `PUT /papers/{id}/discussion` — enable/disable discussion
  - `PUT /papers/bulk-discussion` — bulk enable/disable
- Logic khi enable REVIEW_DISCUSSION activity + `enableAllPapersForDiscussion = true` → auto-enable tất cả papers

### 5.2 Discussion Comments ⚠️ Entity cơ bản có, cần mở rộng

**Backend cần bổ sung `ReviewComment.java`:**
- Thêm field `parentCommentId` (Integer, nullable) — support threaded replies
- Thêm field `isDiscussionPost` (Boolean, default false) — phân biệt review comment vs discussion post
- Thêm field `paper` (ManyToOne Paper) — link discussion trực tiếp tới paper (không chỉ qua Review)

**API endpoints cần thêm:**
- `GET /papers/{id}/discussion` — get discussion threads cho paper
- `POST /papers/{id}/discussion` — tạo discussion topic/reply
- Logic: Check visibility rules (BR-3.32, BR-3.33):
  - Reviewer phải submit review trước khi discuss (nếu setting bật)
  - Non-assigned reviewer chỉ discuss nếu setting bật + không conflict
  - Author chỉ discuss nếu setting bật + có PC post trước

**Frontend cần tạo mới:**
- **Discussion UI component** cho paper detail page
- Threaded comment view (parent-child)
- Chair controls: enable/disable discussion per paper

---

## PHASE 6: DECISION ⚠️ Meta-review có, Paper Status + Notification thiếu

### 6.1 Meta-Review ✅
- Backend: `ReviewMetaReview.java`, `ReviewMetaReviewService`, `ReviewMetaReviewController` — đầy đủ
- Decision → auto-update paper status

### 6.2 Paper Status ⚠️ ENUM CẦN CẬP NHẬT

**Backend cần:**
- `PaperStatus.java`: Giữ `DRAFT`, thêm `REVISION`, `PUBLISHED`
  ```java
  // Hiện tại:  DRAFT, SUBMITTED, UNDER_REVIEW, ACCEPTED, REJECTED, WITHDRAWN, PUBLISHED
  // Cần thêm: REVISION
  // DRAFT: Khi author register paper nhưng chưa upload manuscript file
  // SUBMITTED: Tự động chuyển khi author upload manuscript file
  ```
- API: `PUT /papers/{id}/status` + `PUT /papers/bulk-status` — Chair set status hàng loạt
- Review Aggregates API (BR-3.34-36):
  - `GET /conferences/{id}/review-aggregates` — avg score per question per paper

**Frontend cần:**
- Chair Console: Cột paper status + bulk edit UI
- Review Aggregates: Cột aggregate trên Chair Console (nếu setting bật)

### 6.3 Author Notification Wizard ❌ CHƯA CÓ

**Backend cần:**
- Tận dụng `EmailHistory`, `EmailTemplate`, `BulkEmailRequestDTO` đã có
- API endpoint:
  - `POST /conferences/{id}/author-notification` — Send notifications theo paper status
  - Request: templatePerStatus (Map<PaperStatus, templateId>), recipientType (PRIMARY_CONTACT | ALL_AUTHORS)
- Logic:
  - Validate tất cả papers đã có status
  - Gửi email per paper (1 author nhiều papers = nhiều emails)
  - Auto-mark activity `AUTHOR_NOTIFICATION` = COMPLETED

**Frontend cần tạo mới:**
- **Author Notification Wizard** (multi-step):
  1. Hiển thị papers grouped by status
  2. Chọn/tạo template cho mỗi status
  3. Chọn recipients
  4. Preview emails
  5. Confirm & Send

---

## PHASE 7: POST-DECISION ❌ CHƯA CÓ

### 7.1 Revision Cycle ❌

**Backend cần:**
- Khi status = `REVISION`:
  - Author thấy revision upload link
  - API: `POST /papers/{id}/revision` — upload revised paper
  - Reuse `PaperFile` entity (thêm field `isRevision` Boolean)
- Chair enable "Revision Submission" activity + deadline
- Chair enable review lại chỉ revision papers (setting `allowOnlyRevisionPapers`)
- Sau revision review → Chair set final status ACCEPTED/REJECTED

**Frontend cần:**
- Author Console: Revision upload form (hiện khi paper.status = REVISION)
- Chair: Toggle "Allow only revision papers for reviewing"

### 7.2 Camera-Ready Submission ❌

**Backend cần:**
- Khi status = `ACCEPTED`:
  - API: `POST /papers/{id}/camera-ready` — upload camera-ready files
  - Reuse `PaperFile` entity (thêm field `isCameraReady` Boolean)
  - Chair enable "Camera Ready Submission" activity + deadline
- Logic: Sau deadline → lock uploads, Paper.status → `PUBLISHED`

**Frontend cần:**
- Author Console: Camera-ready upload form (hiện khi paper.status = ACCEPTED + camera-ready enabled)
- Chair: Toggle camera-ready activity, view uploaded camera-ready files

---

## PHASE BỔ SUNG: EMAIL TRONG REVIEW FLOW

### Email tự động (BR-3.40-42)

**Backend cần:**
- Khi manual assign + `notifyOnManualAssignment = true` → gửi email reviewer
- Khi review update during discussion + settings bật → notify other reviewers/meta-reviewers
- Tận dụng `EmailService.sendSimpleMessage()` hoặc `sendInvitationEmail()` đã có

**Frontend cần:**
- Chair: "Email Reviewers" button trong Manage Reviewers page
- Email template selection + placeholders
- Tận dụng `email-template.api.ts` + `email-management.tsx` đã có

---

## BẢNG TÓM TẮT

| Phase | Backend | Frontend | Độ ưu tiên |
|---|---|---|---|
| 1.1 Review Settings | +10 fields | +10 toggles | Trung bình |
| 1.2 Review Questions | ✅ Đã đủ | ✅ Đã đủ | — |
| 1.3 Conflict Management | ✅ ConflictType 7 values, query APIs | ✅ conflict-management.tsx | — (MVP done) |
| 2.x Bidding | +Lọc REVISION papers | Bidding UI page (nếu chưa có) | Thấp |
| 3.x Assignment | +Email on manual assign | ✅ Đã đủ | Thấp |
| 4.3 Review Read-Only | +Paper field, APIs | +Chair Console column | Trung bình |
| 5.x Discussion | +Paper field, comment threading, APIs | +Discussion UI | **Cao** |
| 6.2 Paper Status | +Enum thêm REVISION, bulk API, aggregates | +Chair Console columns | **Cao** |
| 6.3 Author Notification | +Wizard API, auto-complete | +Wizard UI (5 steps) | **Cao** |
| 7.1 Revision Cycle | +Revision upload API | +Author revision UI | Trung bình |
| 7.2 Camera-Ready | +Camera-ready API | +Author camera-ready UI | Trung bình |
| Email trong Review | +Auto-send logic | +Bulk email reviewer UI | Trung bình |

---

## THỨ TỰ IMPLEMENT KHUYẾN NGHỊ

### Sprint 1: Nền tảng (ưu tiên cao nhất)
1. **PaperStatus enum update** — Thêm REVISION (giữ DRAFT cho flow register paper)
2. **TrackReviewSetting +10 fields** — Backend + Frontend settings UI
3. **Paper +2 fields** (`isReviewReadOnly`, `isDiscussionEnabled`) + APIs

### Sprint 2: Discussion + Conflict
4. **Conflict Management** — Mở rộng ConflictType, import API, Frontend UI
5. **Discussion System** — Comment threading, per-paper enable/disable, Discussion UI, visibility rules

### Sprint 3: Decision + Notification
6. **Paper Status Management** — Bulk set status, Review Aggregates
7. **Author Notification Wizard** — Backend API + Frontend multi-step wizard

### Sprint 4: Post-Decision
8. **Revision Cycle** — Revision upload, re-review flow
9. **Camera-Ready Submission** — Camera-ready upload, PUBLISHED status

### Sprint 5: Email + Polish
10. **Email trong Review Flow** — Auto-send on assign, deadline reminders
11. **Emergency Reviewer** (nếu cần) — Flag, assign flow

---

## DANH SÁCH FILES CẦN SỬA/TẠO MỚI

### Backend — Sửa
| File | Thay đổi |
|---|---|
| `entity/TrackReviewSetting.java` | +10 boolean fields |
| `entity/Paper.java` | +`isReviewReadOnly`, `isDiscussionEnabled` |
| `entity/ReviewComment.java` | +`parentCommentId`, `isDiscussionPost`, `paper` |
| `entity/PaperFile.java` | +`isRevision`, `isCameraReady` |
| `utils/enums/PaperStatus.java` | Thêm REVISION |
| `utils/enums/ConflictType.java` | +COLLEAGUE, COLLABORATOR, THESIS_ADVISOR, RELATIVE_FRIEND |
| `dto/TrackReviewSettingDTO.java` | +10 fields |
| `dto/PaperDTO.java` hoặc `PaperUpdateStatusDTO.java` | +bulk status update |
| `controller/PaperController.java` | +bulk status, read-only, discussion APIs |
| `controller/ReviewCommentController.java` | +discussion thread APIs |
| `service/impl/ReviewCommentServiceImpl.java` | +visibility rules, threading logic |
| `service/impl/PaperServiceImpl.java` | +status transitions, read-only logic |
| `service/impl/BiddingServiceImpl.java` | +lọc REVISION papers |

### Backend — Tạo mới
| File | Mô tả |
|---|---|
| `dto/request/AuthorNotificationRequestDTO.java` | Template per status, recipient type |
| `dto/request/BulkStatusUpdateDTO.java` | Bulk paper status update |
| `dto/response/ReviewAggregateDTO.java` | Aggregate score per question per paper |
| `controller/AuthorNotificationController.java` | Author notification wizard endpoints |
| `service/AuthorNotificationService.java` | Notification logic |
| `service/impl/AuthorNotificationServiceImpl.java` | Impl |
| `service/ReviewAggregateService.java` | Aggregate calculation |
| DB migration script | Thêm columns, update PaperStatus enum |

### Frontend — Sửa
| File | Thay đổi |
|---|---|
| `review-settings.tsx` | +10 toggle switches |
| `types/review.ts` | +AutoAssignConfig.loadBalancing, paper fields |
| `types/bidding.ts` | (nhỏ, thêm REVISION filter) |

### Frontend — Tạo mới
| File | Mô tả |
|---|---|
| `app/api/conflict.api.ts` | CRUD conflicts, import |
| `types/conflict.ts` | Conflict types |
| `app/(main)/conference/[conferenceId]/update/conflict-management.tsx` | Chair manage conflicts |
| `app/(main)/conference/[conferenceId]/update/discussion-panel.tsx` | Discussion threads per paper |
| `app/(main)/conference/[conferenceId]/update/author-notification-wizard.tsx` | Multi-step wizard |
| `app/(main)/conference/[conferenceId]/update/paper-status-management.tsx` | Bulk status + aggregates |
| Components: `discussion-thread.tsx`, `discussion-form.tsx` | Reusable discussion components |
| `app/api/discussion.api.ts` | Discussion APIs |
| `types/discussion.ts` | Discussion types |

---

## VERIFICATION PLAN

### Automated / Manual Tests

Dự án hiện tại **chưa có test suite** (không tìm thấy thư mục `test`). Do đó verification chủ yếu bằng manual testing:

1. **PaperStatus update**: Chạy app → tạo paper → confirm status flow: SUBMITTED → UNDER_REVIEW → ACCEPTED/REJECTED/REVISION → WITHDRAWN
2. **Review Settings**: Chair update settings → verify 17 toggle switches hoạt động
3. **Discussion**: Assign reviewer → enable discussion → reviewer post discussion → verify visibility rules
4. **Author Notification**: Set paper statuses → run wizard → verify emails sent + activity marked complete
5. **Revision**: Set paper REVISION → author upload revised file → Chair re-review
6. **Camera-Ready**: Set paper ACCEPTED → author upload camera-ready → verify PUBLISHED

### Cách test thủ công
1. Chạy backend: `./mvnw spring-boot:run` (hoặc IDE run)
2. Chạy frontend: `npm run dev`
3. Sử dụng Postman/Thunder Client test các API endpoints
4. Kiểm tra bằng frontend UI cho các flow end-to-end

> **Khuyến nghị**: User nên chỉ định scope test cụ thể cho từng Sprint. Mỗi Sprint implement xong → test manual → approve → Sprint tiếp.
