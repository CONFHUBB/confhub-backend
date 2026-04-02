# ConfHub - Hướng Dẫn Test Toàn Bộ Flows

> **Ngày cập nhật:** 01/04/2026  
> **Backend:** Spring Boot (`d:\Coding\confms-backend`)  
> **Frontend:** Next.js (`d:\Coding\confms-frontend`)  
> **Database:** MySQL (JPA/Hibernate auto-DDL)  
> **Reference:** [CMT3 Docs](https://cmt3.research.microsoft.com/docs/help/index.html)

---

## Mục lục

1. [Chuẩn bị môi trường](#1-chuẩn-bị-môi-trường)
2. [Flow 1: Tạo Conference & Cấu hình](#2-flow-1-tạo-conference--cấu-hình)
3. [Flow 2: Nộp bài (Submission)](#3-flow-2-nộp-bài-submission)
4. [Flow 3: Bidding & Review](#4-flow-3-bidding--review)
5. [Flow 4: Thảo luận (Discussion)](#5-flow-4-thảo-luận-discussion)
6. [Flow 5: Camera-Ready Submission](#6-flow-5-camera-ready-submission)
7. [Flow 6: Diễn ra Hội nghị](#7-flow-6-diễn-ra-hội-nghị)
8. [Flow 7: Mua vé & Check-in](#8-flow-7-mua-vé--check-in)
9. [Tài khoản Test & Dữ liệu mẫu](#9-tài-khoản-test--dữ-liệu-mẫu)

---

## 1. Chuẩn bị môi trường

### 1.1 Chạy Backend

```bash
cd d:\Coding\confms-backend
# Linux/Mac:
./mvnw spring-boot:run
# Windows:
mvnw.cmd spring-boot:run
```

Backend chạy tại: `http://localhost:8080`

### 1.2 Chạy Frontend

```bash
cd d:\Coding\confms-frontend
npm run dev
```

Frontend chạy tại: `http://localhost:3000`

### 1.3 Kiểm tra Database

Đảm bảo MySQL đang chạy và database `confhub` tồn tại. Hibernate config `ddl-auto=update` sẽ tự động tạo/migrate schema.

### 1.4 Prerequisites chung

Trước khi test bất kỳ flow nào, cần có:

| Yêu cầu | Mô tả |
|---|---|
| Tài khoản Chair | User đã đăng ký, có quyền tạo conference |
| Tài khoản Reviewer | User để gán vai trò reviewer (tối thiểu 3 accounts) |
| Tài khoản Author | User để submit papers (tối thiểu 2 accounts) |
| Conference đang PENDING/SCHEDULED | Đã tạo conference theo Flow 1 |

---

## 2. Flow 1: Tạo Conference & Cấu hình

> **Scope:** Tạo conference, thêm tracks, subject areas, mời members, cấu hình review settings, bật activity timeline.

### 2.1 Tạo Conference

**Frontend:**

1. Login với tài khoản Chair
2. Vào **My Conferences** → Click **Create Conference**
3. Điền form:
   - **Name:** `International Conference on Software Engineering 2026`
   - **Acronym:** `ICSE 2026` (unique)
   - **Description:** Mô tả hội nghị
   - **Location:** `Ho Chi Minh City, Vietnam`
   - **Start Date:** Ngày trong tương lai (VD: 01/06/2026)
   - **End Date:** > Start Date (VD: 03/06/2026)
   - **Area:** `Software Engineering`
4. Click **Create**

**Expected Results:**
- ✅ Conference được tạo với status = `PENDING`
- ✅ Người tạo tự động có role `CONFERENCE_CHAIR`
- ✅ 6 ConferenceActivities được tự động tạo (disabled):
  - PAPER_SUBMISSION, REVIEWER_BIDDING, REVIEW_SUBMISSION, REVIEW_DISCUSSION, AUTHOR_NOTIFICATION, CAMERA_READY_SUBMISSION
- ✅ Chuyển hướng đến trang Update Conference

**API Test (Postman/curl):**
```
POST /api/v1/conferences
Content-Type: application/json
Authorization: Bearer {token}

{
  "name": "ICSE 2026",
  "acronym": "ICSE 2026",
  "description": "International Conference on Software Engineering",
  "location": "Ho Chi Minh City, Vietnam",
  "startDate": "2026-06-01T09:00:00",
  "endDate": "2026-06-03T18:00:00",
  "area": "Software Engineering"
}

# Verify response: 201 Created, body chứa conference ID
# Verify auto-created activities:
GET /api/v1/activities/conference/{conferenceId}
# Response: 6 activities, tất cả isEnabled = false
```

### 2.2 Thêm Tracks

**Frontend:**

1. Trong trang Update Conference → Tab **Tracks**
2. Click **Add Track**
3. Nhập track name: `Research Track`

**Expected Results:**
- ✅ Track được tạo thành công
- ✅ TrackReviewSetting tự động được tạo cho track (default values)

**API Test:**
```
POST /api/v1/conferences/{conferenceId}/tracks
Body: {"name": "Research Track", "description": "Main research track"}
```

### 2.3 Thêm Subject Areas

**Frontend:**

1. Trong trang Update Conference → Tab **Subject Areas**
2. Click **Add Subject Area**
3. Nhập: `Software Testing`, `Machine Learning`, `Cloud Computing`

**Expected Results:**
- ✅ Subject areas được lưu và hiển thị
- ✅ Phải có ít nhất 1 subject area để bật PAPER_SUBMISSION

**API Test:**
```
POST /api/v1/subject-areas
Body: {"conferenceId": 1, "name": "Software Testing", "description": "..."}
```

### 2.4 Cấu hình Review Settings (per Track)

**Frontend:**

1. Tab **Review Settings** → Chọn track từ dropdown
2. Verify hiển thị các settings:
   - `isDoubleBlind` (default: false)
   - `requireSubjectAreas` (default: false)
   - `reviewerInviteExpirationDays` (default: 7)
   - `allowReviewerQuota` (default: false)
   - `allowReviewUpdateDuringDiscussion` (default: false)
3. Bật tắt các toggles → **Save Changes**

**Expected Results:**
- ✅ Settings được lưu đúng cho từng track
- ✅ Reload page → Settings vẫn giữ nguyên

**API Test:**
```
GET /api/v1/tracks/{trackId}/review-settings
PUT /api/v1/tracks/{trackId}/review-settings
Body: {"isDoubleBlind": true, "requireSubjectAreas": true, ...}
```

### 2.5 Mời Reviewer

**Frontend:**

1. Tab **Members** hoặc **Config Members**
2. Tìm user qua email
3. Gán role `REVIEWER` cho conference

**Expected Results:**
- ✅ User được thêm với role REVIEWER
- ✅ User nhận notification ROLE_ASSIGNED
- ✅ Không thể xóa CONFERENCE_CHAIR cuối cùng

**API Test:**
```
POST /api/v1/conferences/{conferenceId}/users-roles
Body: {"userId": 5, "role": "REVIEWER"}

# Verify không xóa được Chair cuối cùng:
DELETE /api/v1/conferences/{conferenceId}/users-roles
# Response 400: "Cannot remove the last CONFERENCE_CHAIR"
```

### 2.6 Bật Activity Timeline

**Frontend:**

1. Tab **Activity Timeline**
2. Bật **PAPER_SUBMISSION**:
   - Toggle `isEnabled` = ON
   - Đặt deadline (VD: 01/05/2026)
3. Verify status conference chuyển: `PENDING` → `SCHEDULED`

**Expected Results:**
- ✅ PAPER_SUBMISSION được bật
- ✅ Conference status chuyển sang `SCHEDULED` (vì đã có tracks + subject areas)
- ✅ Bật REVIEWER_BIDDING → Cần đã có papers submitted
- ✅ Bật REVIEW_SUBMISSION → Cần đã có reviewer assignments
- ✅ Bật AUTHOR_NOTIFICATION → Cần reviews đã hoàn tất

**Conference Status Flow:**
```
PENDING → SCHEDULED → ONGOING → COMPLETED
                ↓
            CANCELLED
```

**API Test:**
```
PUT /api/v1/activities/{activityId}
Body: {"isEnabled": true, "deadline": "2026-05-01T23:59:59"}

# Verify status transition:
GET /api/v1/conferences/{conferenceId}
# Response: status = "SCHEDULED"
```

### 2.7 Gán thêm Program Chair

**Frontend:**

1. Chair gán thêm một user làm `PROGRAM_CHAIR`
2. Program Chair có quyền quản lý track, assign reviewers

**Expected Results:**
- ✅ User được thêm với role PROGRAM_CHAIR
- ✅ PROGRAM_CHAIR có thể thấy các tabs quản lý

---

## 3. Flow 2: Nộp bài (Submission)

> **Scope:** Author submit paper, upload files, quản lý submission, co-author, conflict management.

### 3.1 Author Submit Paper

**Prerequisites:**
- PAPER_SUBMISSION activity đang enabled
- Author đã có tài khoản và biết conference ID

**Frontend:**

1. Vào trang Conference Details (`/conference/{id}`)
2. Click **Submit Paper** (hoặc **Author Dashboard**)
3. Chọn Track → Nhập thông tin:
   - **Title:** `A Novel Approach to Software Testing using AI`
   - **Abstract:** Mô tả tóm tắt bài báo
   - **Subject Areas:** Chọn ít nhất 1 (VD: Software Testing)
   - **Authors:** Thêm co-authors (email, name)
4. Upload paper file (PDF, max size configured)
5. Submit

**Expected Results:**
- ✅ Paper được tạo với status = `SUBMITTED`
- ✅ Author tự động được gán role `AUTHOR` trong conference
- ✅ Email confirmation được gửi cho author
- ✅ Notification PAPER_SUBMITTED được tạo

**API Test:**
```
POST /api/v1/papers
Content-Type: multipart/form-data
- title: "A Novel Approach to Software Testing"
- abstract: "..."
- trackId: 1
- subjectAreaIds: [1, 2]
- authors: [{"email": "coauthor@example.com", "name": "John Doe"}]
- file: (PDF)

# Verify paper created:
GET /api/v1/papers/conference/{conferenceId}
# Status = "SUBMITTED"
```

### 3.2 Xem danh sách Papers (Author)

**Frontend:**

1. Vào **My Papers** (`/my-profile/papers`)
2. Verify hiển thị tất cả papers của user trong conference

**Expected Results:**
- ✅ Tất cả papers (submitted, under review, accepted, etc.)
- ✅ Status badges đúng màu
- ✅ Action buttons phù hợp với từng status

**Paper Status Flow:**
```
DRAFT → SUBMITTED → UNDER_REVIEW → ACCEPTED → PUBLISHED
                                 → REJECTED
                                 → REVISION → UNDER_REVIEW
Bất kỳ (trừ PUBLISHED) → WITHDRAWN
```

### 3.3 Quản lý Co-Author

**Frontend:**

1. Paper Details → **Authors** tab
2. Thêm co-author: nhập email → gửi invitation
3. Co-author accept/decline invitation

**Expected Results:**
- ✅ Co-author nhận email invitation
- ✅ Accept → co-author được thêm vào paper
- ✅ Decline → co-author không được thêm

### 3.4 Khai báo Conflict

**Frontend (Chair):**

1. Conference Update → **Conflict Management** tab
2. Click **Add Conflict**
3. Chọn Paper, User, Type:
   - `Co-Author`: Đồng tác giả
   - `Personal`: Quan hệ cá nhân
   - `Domain`: Cùng domain nghiên cứu
   - `Colleague`: Đồng nghiệp
   - `Collaborator`: Cộng tác nghiên cứu
   - `Thesis Advisor`: Thầy hướng dẫn
   - `Relative/Friend`: Người thân/bạn bè
4. Submit

**Expected Results:**
- ✅ Conflict được lưu
- ✅ Reviewer có conflict không thể bid/assign cho paper đó
- ✅ Domain conflict auto-detect khi cùng university email

**API Test:**
```
POST /api/v1/paper-conflict
Body: {"paperId": 1, "userId": 3, "conflictType": "PERSONAL"}

# Verify conflict ảnh hưởng đến bidding:
GET /api/v1/bidding/papers-for-bidding?reviewerId=3&conferenceId=1
# Paper 1 sẽ KHÔNG xuất hiện trong danh sách
```

### 3.5 Withdraw Paper

**Frontend (Author):**

1. Paper Details → **Withdraw** button
2. Confirm withdrawal

**Expected Results:**
- ✅ Paper status chuyển → `WITHDRAWN`
- ✅ Paper không còn visible cho reviewers
- ✅ Notification gửi cho Chair

**API Test:**
```
PUT /api/v1/paper/status/{paperId}
Body: {"status": "WITHDRAWN"}
# Response: 200, status = "WITHDRAWN"
```

---

## 4. Flow 3: Bidding & Review

> **Scope:** Reviewer bidding, auto/manual assignment, submit reviews, meta-review, decisions.

### 4.1 Reviewer chọn Interests

**Prerequisites:**
- Reviewer đã có role REVIEWER trong conference

**Frontend:**

1. Vào **Reviewer Console** (`/conference/{id}/reviewer`)
2. Tab **Interests / Subject Areas**
3. Chọn các subject areas quan tâm
4. Chọn expertise level: `EXPERT`, `COMPETENT`, `NOVICE`

**Expected Results:**
- ✅ Interests được lưu
- ✅ Used for auto-assignment algorithm

**API Test:**
```
POST /api/v1/reviewer-interests
Body: {"reviewerId": 5, "conferenceId": 1, "subjectAreaId": 1, "expertiseLevel": "EXPERT"}
```

### 4.2 Reviewer Bidding

**Prerequisites:**
- REVIEWER_BIDDING activity đang enabled
- Có papers đã submitted (status SUBMITTED/UNDER_REVIEW)

**Frontend:**

1. Tab **Bidding**
2. Danh sách papers hiển thị (loại trừ: WITHDRAWN, có conflict)
3. Bid cho từng paper:
   - `EAGER` - Rất muốn review
   - `WILLING` - Sẵn lòng review
   - `IN_A_PINCH` - Chỉ khi cần
   - `NOT_WILLING` - Không muốn

**Expected Results:**
- ✅ Bids được lưu đúng với reviewer + paper + bid value
- ✅ Không bid được paper có conflict
- ✅ Không bid được paper đã assigned đầy reviewers
- ✅ Thay đổi bid → Update thành công

**API Test:**
```
GET /api/v1/bidding/papers-for-bidding?reviewerId={id}&conferenceId={id}
# Response: Danh sách papers (loại trừ conflict, withdrawn)

POST /api/v1/bidding
Body: {"paperId": 1, "reviewerId": 5, "bidValue": "EAGER", "conferenceId": 1}

GET /api/v1/bidding/summary/{reviewerId}/conference/{conferenceId}
# Response: Tổng hợp bids của reviewer đó
```

### 4.3 Chair: Xem Bids Summary

**Frontend (Chair):**

1. Conference Update → **Review Management** hoặc **Bidding Overview**
2. Xem tổng hợp bids của tất cả reviewers

**Expected Results:**
- ✅ Hiển thị bids theo paper và theo reviewer
- ✅ Filter theo bid value

### 4.4 Auto-Assign Reviewers

**Prerequisites:**
- Có đủ bids từ reviewers
- Đã khai báo conflicts (nếu có)

**Frontend:**

1. Conference Update → **Reviewer Assignment** tab
2. **Overview tab:** Stats về papers, reviewers, assignments
3. **Auto-Assign tab:**
   - Config: `minReviewersPerPaper` = 2, `maxPapersPerReviewer` = 3
   - Weight: `bidWeight` = 70%, `relevanceWeight` = 30%
   - Click **Run Auto-Assign**
4. Xem **Preview**: Danh sách proposed assignments
5. Click **Confirm**

**Expected Results:**
- ✅ Assignments được tạo cho papers
- ✅ Không có assignment cho cặp có conflict
- ✅ Load balancing được áp dụng
- ✅ Reviewers không bị assign quá quota

**API Test:**
```
POST /api/v1/conferences/{conferenceId}/assignments/auto-assign
Body: {
  "minReviewersPerPaper": 2,
  "maxPapersPerReviewer": 3,
  "bidWeight": 70,
  "relevanceWeight": 30,
  "loadBalancing": true
}
# Response: Preview assignments

POST /api/v1/conferences/{conferenceId}/assignments/confirm
# Response: 200, assignments lưu vào DB
```

### 4.5 Manual Assign (Chair)

**Frontend:**

1. **Manual Assign tab**
2. Chọn Paper + Reviewer → Click Assign
3. Hoặc trong Assignments table → Remove assignment

**Expected Results:**
- ✅ Manual assign thành công
- ✅ Không thể assign reviewer có conflict → Bị block
- ✅ Remove assignment → Reviewer không còn quyền review paper đó

**API Test:**
```
POST /api/v1/conferences/{conferenceId}/assignments/manual?paperId={id}&reviewerId={id}
# Response 400 nếu có conflict

DELETE /api/v1/conferences/{conferenceId}/assignments/{reviewAssignmentId}
# Response 200
```

### 4.6 Reviewer Submit Review

**Prerequisites:**
- REVIEW_SUBMISSION activity đang enabled
- Reviewer đã được assign vào paper

**Frontend:**

1. Reviewer Console → **My Reviews** tab
2. Click vào paper được assign
3. Trả lời các review questions:
   - Rating questions (1-10)
   - Text questions
   - Multiple choice questions
4. Submit Review

**Expected Results:**
- ✅ Review status chuyển: `ASSIGNED` → `IN_PROGRESS` → `COMPLETED`
- ✅ Total score được tính tự động
- ✅ Notification REVIEW_COMPLETED gửi cho Chair
- ✅ Review có thể edit nếu setting `allowReviewUpdateDuringDiscussion = true`

**API Test:**
```
GET /api/v1/review/reviewer/{reviewerId}/conference/{conferenceId}
# Response: Danh sách reviews được assign

POST /api/v1/review-answers/bulk
Body: [
  {"questionId": 1, "answer": "8"},
  {"questionId": 2, "answer": "The paper is well-written..."}
]
# Body: reviewId trong header hoặc param

PUT /api/v1/review/status/{reviewId}
Body: {"status": "COMPLETED"}
```

### 4.7 Meta-Review & Decision (Chair/PC)

**Prerequisites:**
- Tất cả reviews cho paper đã hoàn tất
- AUTHOR_NOTIFICATION activity đang enabled

**Frontend:**

1. Chair Dashboard → Paper Details
2. Xem tất cả reviews → Overall recommendation
3. Tạo Meta-Review:
   - Đánh giá tổng quan
   - Set Decision: `APPROVE`, `REJECT`, hoặc `REVISION`

**Expected Results:**
- ✅ Paper status tự động cập nhật:
  - `APPROVE` → `ACCEPTED`
  - `REJECT` → `REJECTED`
  - `REVISION` → `REVISION`
- ✅ Author nhận notification

**API Test:**
```
POST /api/v1/meta-review
Body: {
  "paperId": 1,
  "summary": "Good paper with minor issues...",
  "decision": "APPROVE",
  "commentsToAuthors": "..."
}

# Bulk update paper status:
PUT /api/v1/paper/bulk-status
Body: [
  {"id": 1, "status": "ACCEPTED"},
  {"id": 2, "status": "REJECTED"}
]
```

### 4.8 Bulk Paper Status Update (Chair)

**Frontend:**

1. Paper Management → Select multiple papers
2. Bulk action: Update status

**Expected Results:**
- ✅ Status transitions hợp lệ:
  - `UNDER_REVIEW` → `ACCEPTED` / `REJECTED` / `REVISION`
  - `REVISION` → `UNDER_REVIEW` (re-review)
- ✅ Invalid transitions bị reject

---

## 5. Flow 4: Thảo luận (Discussion)

> **Scope:** Reviewers thảo luận về paper, Chair quản lý discussion, threaded comments.

### 5.1 Bật Discussion Activity (Chair)

**Prerequisites:**
- REVIEW_DISCUSSION activity đang enabled
- Có papers đã được review

**Frontend:**

1. Tab **Activity Timeline** → Bật **REVIEW_DISCUSSION**
2. Set deadline

### 5.2 Bật Discussion cho Paper cụ thể (Chair)

**Frontend:**

1. Paper Details → Toggle **Discussion Enabled**
2. Hoặc bulk enable: **Bulk Enable Discussion** → Chọn papers

**Expected Results:**
- ✅ Discussion được bật cho paper đó
- ✅ Reviewers có thể post comments

**API Test:**
```
PUT /api/v1/paper/{paperId}/discussion?enabled=true
PUT /api/v1/paper/bulk-discussion?enabled=true
Body: [1, 2, 3]  # paper IDs
```

### 5.3 Reviewer: Post Discussion Comment

**Frontend:**

1. Paper Details → **Discussion** tab
2. Post comment mới
3. Reply vào comment của reviewer khác (threaded)

**Expected Results:**
- ✅ Comment được lưu với đúng author
- ✅ Thread hiển thị đúng hierarchy
- ✅ Double-blind: Reviewer identities được ẩn nếu `isDoubleBlind = true`

**API Test:**
```
POST /api/v1/review-comments
Body: {
  "reviewId": 1,
  "content": "I agree with the reviewer about...",
  "title": "Comment on methodology"
}

GET /api/v1/review-comments/review/{reviewId}
# Response: Threaded comments
```

### 5.4 Review Read-Only (Chair)

**Frontend:**

1. Paper Details → Toggle **Review Read-Only**

**Expected Results:**
- ✅ Reviewers không thể edit reviews của paper đó
- ✅ Existing reviews vẫn visible

**API Test:**
```
PUT /api/v1/paper/{paperId}/review-read-only?readOnly=true
# Reviewer thử PUT review → 403 Forbidden
```

---

## 6. Flow 5: Camera-Ready Submission

> **Scope:** Accepted papers → Camera-ready submission → Final program.

### 6.1 Author Upload Camera-Ready

**Prerequisites:**
- Paper status = `ACCEPTED`
- CAMERA_READY_SUBMISSION activity đang enabled

**Frontend:**

1. Paper Details (Author) → **Camera-Ready** tab
2. Upload final paper file (PDF)
3. Upload supplementary files (optional)
4. Submit

**Expected Results:**
- ✅ Camera-ready file được upload lên Firebase Storage
- ✅ Paper status có thể chuyển → `CAMERA_READY_SUBMITTED`
- ✅ Confirmation email được gửi

**API Test:**
```
POST /api/v1/paper/{paperId}/camera-ready
Content-Type: multipart/form-data
- file: (final PDF)
- supplementaryFiles: [...]
```

### 6.2 Chair: Quản lý Camera-Ready Submissions

**Frontend:**

1. Conference Update → **Camera-Ready Management** tab
2. Xem danh sách camera-ready submissions
3. Approve/Reject từng submission

**Expected Results:**
- ✅ Tất cả camera-ready submissions hiển thị
- ✅ Chair có thể approve → Paper → `PUBLISHED` sau deadline

### 6.3 Author Notification Wizard

**Frontend (Chair):**

1. Conference Update → **Author Notification** tab
2. Chạy notification wizard:
   - Select papers (accepted/rejected/revision)
   - Compose email template
   - Preview
   - Send

**Expected Results:**
- ✅ Emails được gửi cho tất cả authors của các papers được chọn
- ✅ Notification AUTHOR_NOTIFICATION được log

---

## 7. Flow 6: Diễn ra Hội nghị

> **Scope:** Program builder, event day activities, published papers, session ratings.

### 7.1 Chair: Build Program Schedule

**Frontend:**

1. Conference Update → **Program Builder** tab
2. Tạo program:
   - Thêm sessions (Keynote, Technical, Panel, etc.)
   - Gán accepted papers vào sessions
   - Đặt thời gian, địa điểm
3. **Publish Program**

**Expected Results:**
- ✅ Program được lưu vào `conference.programSchedule`
- ✅ Public: Attendees có thể xem program
- ✅ Papers đã gán visible trong program

**API Test:**
```
PUT /api/v1/conferences/{conferenceId}/program-schedule
Body: {
  "published": true,
  "schedule": {
    "day1": [...sessions...],
    "day2": [...sessions...]
  }
}
```

### 7.2 Attendee: Xem Program

**Frontend:**

1. Conference Details → **Program** tab
2. Xem lịch trình chi tiết
3. Click vào paper → Xem abstract

**Expected Results:**
- ✅ Program hiển thị đầy đủ
- ✅ Có thể filter theo day/track/topic
- ✅ Bookmarked sessions được highlight

### 7.3 Attendee: Rate Sessions

**Prerequisites:**
- Hội nghị đang diễn ra (`ONGOING` status)
- Attendee đã check-in

**Frontend:**

1. Program → Click vào session đã attended
2. Rate session: 1-5 stars
3. Submit

**Expected Results:**
- ✅ Rating được lưu
- ✅ Attendee không thể rate lại session đã rate
- ✅ Chair có thể xem aggregate ratings

**API Test:**
```
POST /api/v1/session-ratings
Body: {"sessionId": 1, "userId": 5, "rating": 5, "comment": "Excellent session!"}
```

### 7.4 Attendee: Bookmark Sessions

**Frontend:**

1. Program → Bookmark icon trên session

**Expected Results:**
- ✅ Session bookmarked
- ✅ Xem **My Bookmarks** → Danh sách bookmarked sessions

### 7.5 Update Conference Status

**Frontend (Chair):**

1. Conference Update → **Dashboard** → Change Status
2. Manual transition:
   - `SCHEDULED` → `ONGOING` (khi hội nghị bắt đầu)
   - `ONGOING` → `COMPLETED` (khi kết thúc)

**Expected Results:**
- ✅ Status transitions đúng theo flow
- ✅ Không thể chuyển ngược

### 7.6 Publish Papers

**Frontend (Chair):**

1. Sau khi hội nghị hoàn tất → Chair publish accepted papers
2. Papers visible trong **Published Papers** section

**Expected Results:**
- ✅ Paper status → `PUBLISHED`
- ✅ Papers hiển thị public

**API Test:**
```
PUT /api/v1/paper/status/{paperId}
Body: {"status": "PUBLISHED"}

GET /api/v1/papers/published?conferenceId={id}
# Response: Danh sách published papers
```

---

## 8. Flow 7: Mua vé & Check-in

> **Scope:** Chair tạo ticket types, attendee mua vé qua VNPay, check-in, feedback.

### 8.1 Chair: Tạo Ticket Types

**Frontend:**

1. Conference Update → **Ticket Types** tab
2. Click **Add Ticket Type**
3. Điền thông tin:
   - **Name:** `Early Bird`, `Regular`, `VIP`, `Student`
   - **Category:** `ATTENDEE`, `AUTHOR`, `REVIEWER`, `VIP`, `STUDENT`, `ONLINE`
   - **Price:** 500000 VND
   - **Currency:** VND
   - **Deadline:** Ngày hết hạn mua vé
   - **Max Quantity:** 100

4. **Add**

**Expected Results:**
- ✅ Ticket type được tạo với `isActive = true`
- ✅ Bật activity `REGISTRATION` nếu chưa bật

**API Test:**
```
POST /api/v1/ticket-types
Body: {
  "conferenceId": 1,
  "name": "Early Bird",
  "category": "ATTENDEE",
  "price": 500000,
  "currency": "VND",
  "deadline": "2026-05-30T23:59:59",
  "maxQuantity": 100
}
```

### 8.2 Attendee: Mua vé

**Frontend:**

1. Conference Details → **Tickets** hoặc **Register** tab
2. Chọn ticket type
3. Click **Buy Ticket**
4. Redirect đến VNPay payment gateway
5. Complete payment → Redirect về ConfHub

**Expected Results:**
- ✅ Registration được tạo với status `PENDING`
- ✅ Redirect đến VNPay với đúng amount và order info
- ✅ Sau payment thành công:
  - Ticket status → `CONFIRMED`
  - Payment ghi nhận `COMPLETED`
  - Confirmation email được gửi
- ✅ Sau payment thất bại:
  - Ticket status → `FAILED`
  - Có thể retry payment

**API Test:**
```
# Create registration:
POST /api/v1/registration
Body: {
  "conferenceId": 1,
  "ticketTypeId": 1,
  "userId": 5,
  "firstName": "John",
  "lastName": "Doe"
}
# Response: redirectUrl (VNPay)

# VNPay callback:
GET /api/v1/registration/vnpay-return?vnp_ResponseCode=00&vnp_TxnRef=xxx

# Verify payment:
GET /api/v1/registration/my-tickets?userId=5
# Response: ticket với status = "CONFIRMED"
```

### 8.3 Retry Payment

**Frontend:**

1. **My Profile** → **Payments** → Failed payment
2. Click **Retry Payment**

**Expected Results:**
- ✅ Redirect đến VNPay lại
- ✅ Order info giữ nguyên

**API Test:**
```
POST /api/v1/registration/retry-payment?conferenceId={id}&userId={id}
```

### 8.4 Chair: Xem danh sách Attendees

**Frontend:**

1. Conference Update → **Attendees** tab
2. Danh sách tất cả attendees đã đăng ký
3. Filter theo ticket type, payment status
4. Search theo name/email

**Expected Results:**
- ✅ Hiển thị tất cả registrations
- ✅ Payment status hiển thị (CONFIRMED, PENDING, FAILED, REFUNDED)
- ✅ Export attendees list (nếu có)

**API Test:**
```
GET /api/v1/registration/attendees/{conferenceId}
GET /api/v1/registration/attendees/{conferenceId}?page=0&size=20&search=john&status=CONFIRMED
```

### 8.5 Chair: Check-in Attendees

**Prerequisites:**
- Hội nghị đang diễn ra
- EVENT_DAY activity đang enabled

**Frontend:**

1. Conference Update → **Check-in** tab
2. Quét QR code hoặc nhập ticket code
3. Confirm check-in

**Expected Results:**
- ✅ Ticket marked as `isCheckedIn = true`
- ✅ Timestamp check-in được ghi nhận
- ✅ Không check-in được ticket đã used

**API Test:**
```
POST /api/v1/registration/checkin
Body: {"code": "TICKET_CODE_12345"}
# Response: CheckInResponse { success: true, ticket: {...} }
# Nếu đã check-in rồi: Response 400 "Ticket already checked in"
```

### 8.6 Attendee: Xem vé của mình

**Frontend:**

1. **My Profile** → **My Tickets**
2. Xem danh sách vé đã mua
3. Click vào vé → QR code + details

**Expected Results:**
- ✅ Hiển thị tất cả tickets của user
- ✅ QR code hiển thị đúng
- ✅ Trạng thái: CONFIRMED / CANCELLED / REFUNDED

### 8.7 Attendee: Feedback hội nghị

**Prerequisites:**
- User đã check-in vào hội nghị

**Frontend:**

1. Conference Details → **Feedback** tab (visible sau check-in)
2. Điền feedback form:
   - Overall rating (1-5)
   - Organization rating
   - Content quality rating
   - Venue rating
   - Comments
3. Submit

**Expected Results:**
- ✅ Feedback được lưu
- ✅ User không thể submit lại feedback cho cùng conference
- ✅ Chair có thể xem aggregate feedback

**API Test:**
```
POST /api/v1/conference-feedback
Body: {
  "conferenceId": 1,
  "userId": 5,
  "overallRating": 5,
  "organizationRating": 4,
  "contentRating": 5,
  "venueRating": 4,
  "comments": "Great conference!"
}

GET /api/v1/conference-feedback/conference/{conferenceId}
# Response: Aggregate feedback stats
```

### 8.8 Chair: Refund Ticket

**Frontend:**

1. Attendees tab → Click vào attendee
2. **Refund** button

**Expected Results:**
- ✅ Ticket status → `REFUNDED`
- ✅ Payment recorded as REFUNDED

---

## 9. Tài khoản Test & Dữ liệu mẫu

### 9.1 Tài khoản cần thiết

| Role | Email | Password | Mục đích |
|---|---|---|---|
| Chair | chair@test.com | (set password) | Tạo & quản lý conference |
| Program Chair | pc@test.com | (set password) | Quản lý review, assignment |
| Reviewer 1 | reviewer1@test.com | (set password) | Bid & review papers |
| Reviewer 2 | reviewer2@test.com | (set password) | Bid & review papers |
| Reviewer 3 | reviewer3@test.com | (set password) | Bid & review papers |
| Author 1 | author1@test.com | (set password) | Submit papers |
| Author 2 | author2@test.com | (set password) | Submit papers, co-author |
| Attendee | attendee@test.com | (set password) | Mua vé, check-in |

### 9.2 Dữ liệu mẫu Conference

```
Name: International Conference on Software Engineering 2026
Acronym: ICSE 2026
Location: Ho Chi Minh City, Vietnam
Start Date: 01/06/2026
End Date: 03/06/2026
Area: Software Engineering

Tracks:
- Research Track
- Industry Track

Subject Areas:
- Software Testing
- Machine Learning
- Cloud Computing
- DevOps
- Security

Ticket Types:
- Early Bird: 500,000 VND (deadline: 15/05/2026, max: 100)
- Regular: 700,000 VND (max: 200)
- Student: 300,000 VND (max: 50)
```

### 9.3 Dữ liệu mẫu Papers

| # | Title | Track | Status |
|---|---|---|---|
| 1 | A Novel Approach to Software Testing using AI | Research | SUBMITTED |
| 2 | Optimizing Cloud Resource Allocation | Research | SUBMITTED |
| 3 | Machine Learning for Code Quality Prediction | Research | SUBMITTED |
| 4 | DevOps Pipeline Automation Best Practices | Industry | SUBMITTED |
| 5 | Security Vulnerabilities in IoT Devices | Research | SUBMITTED |

---

## Phụ lục: API Endpoints tổng hợp

### Auth
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/auth/register` | Đăng ký tài khoản |
| POST | `/api/v1/auth/login` | Đăng nhập |

### Conference
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/conferences` | Tạo conference |
| GET | `/api/v1/conferences/{id}` | Chi tiết conference |
| PUT | `/api/v1/conferences/{id}` | Cập nhật conference |
| PUT | `/api/v1/conferences/{id}/status` | Đổi status |
| DELETE | `/api/v1/conferences/{id}` | Xóa conference |
| GET | `/api/v1/conferences/user/{userId}` | Conferences của user |

### Activity
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/activities/conference/{conferenceId}` | Danh sách activities |
| PUT | `/api/v1/activities/{id}` | Bật/tắt activity |

### Paper
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/papers` | Tạo paper (multipart) |
| GET | `/api/v1/papers/conference/{id}` | Papers theo conference |
| GET | `/api/v1/papers/{id}` | Chi tiết paper |
| PUT | `/api/v1/papers/status/{id}` | Đổi status paper |
| PUT | `/api/v1/papers/bulk-status` | Bulk update status |
| PUT | `/api/v1/papers/{id}/discussion` | Toggle discussion |
| PUT | `/api/v1/papers/{id}/review-read-only` | Toggle review read-only |
| POST | `/api/v1/papers/{id}/camera-ready` | Upload camera-ready |
| GET | `/api/v1/papers/published` | Published papers |

### Bidding
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/bidding/papers-for-bidding` | Papers cho bidding |
| POST | `/api/v1/bidding` | Submit bid |
| GET | `/api/v1/bidding/summary/{reviewerId}/conference/{conferenceId}` | Bids summary |

### Assignment
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/conferences/{id}/assignments/auto-assign` | Auto-assign preview |
| POST | `/api/v1/conferences/{id}/assignments/confirm` | Confirm assignments |
| POST | `/api/v1/conferences/{id}/assignments/manual` | Manual assign |
| DELETE | `/api/v1/conferences/{id}/assignments/{reviewAssignmentId}` | Remove assignment |

### Review
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/review/reviewer/{reviewerId}/conference/{conferenceId}` | Reviews by reviewer |
| POST | `/api/v1/review-answers/bulk` | Submit review answers |
| PUT | `/api/v1/review/status/{reviewId}` | Đổi review status |

### Meta-Review
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/meta-review` | Tạo meta-review |

### Discussion
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/review-comments` | Post comment |
| GET | `/api/v1/review-comments/review/{reviewId}` | Comments by review |

### Conflict
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/paper-conflict` | Tạo conflict |
| GET | `/api/v1/paper-conflict/conference/{conferenceId}` | Conflicts theo conference |
| DELETE | `/api/v1/paper-conflict/{id}` | Xóa conflict |

### Registration / Ticket
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/registration` | Tạo registration (mua vé) |
| GET | `/api/v1/registration/my-tickets` | Vé của user |
| GET | `/api/v1/registration/attendees/{conferenceId}` | Danh sách attendees |
| POST | `/api/v1/registration/checkin` | Check-in |
| POST | `/api/v1/registration/retry-payment` | Retry payment |
| GET | `/api/v1/registration/vnpay-return` | VNPay callback |
| POST | `/api/v1/registration/refund` | Refund ticket |

### Ticket Types
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/ticket-types` | Tạo ticket type |
| GET | `/api/v1/ticket-types/conference/{conferenceId}` | Ticket types |
| PUT | `/api/v1/ticket-types/{id}` | Cập nhật |
| DELETE | `/api/v1/ticket-types/{id}` | Xóa |

### Feedback
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/conference-feedback` | Submit feedback |
| GET | `/api/v1/conference-feedback/conference/{conferenceId}` | Feedback stats |

### Session Rating
| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/api/v1/session-ratings` | Rate session |
| GET | `/api/v1/session-ratings/session/{sessionId}` | Ratings by session |
