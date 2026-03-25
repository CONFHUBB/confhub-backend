# 📋 MVP Functional Requirements — Checklist

> **Cập nhật:** 2026-03-25  
> **Dự án:** ConfMS — Conference Management System  
> **Nguồn gốc:** Đề cương đăng ký đồ án  
> **Legend:**  
> ✅ = Đã implement (BE + FE)  
> 🔧 = Có BE, chưa có FE  
> 📦 = Có entity/schema, chưa có logic  
> ❌ = Chưa implement  
> 🚫 = Ngoài phạm vi MVP  

---

## 1. Admin

| # | Functional Requirement | Status | Chi tiết |
|---|---|---|---|
| 1.1 | Create/edit/delete Staff accounts, assign permissions | 🚫 | Post-MVP — chưa có admin role entity |
| 1.2 | Master Dashboard: global stats (revenue, conferences, user growth) | 🚫 | Post-MVP |
| 1.3 | View and export platform-wide financial reports | 🚫 | Post-MVP |

---

## 2. Staff

| # | Functional Requirement | Status | Chi tiết |
|---|---|---|---|
| 2.1 | View pending conference creation requests | 🚫 | Post-MVP |
| 2.2 | Validate organizer details & Approve/Reject requests | 🚫 | Post-MVP |
| 2.3 | Monitor active conferences & suspend non-compliant events | 🚫 | Post-MVP |

---

## 3. Conference Chair / Organizing Committee

| # | Functional Requirement | Status | Chi tiết |
|---|---|---|---|
| 3.1 | Create/edit/delete conference | ✅ | `ConferenceController` CRUD + FE `conference/create` + `update/page.tsx` |
| 3.2 | Define important dates (start/end, deadlines) | ✅ | `Conference` entity + `ConferenceActivity` deadlines, FE activity timeline |
| 3.3 | Define tracks | ✅ | `ConferenceTrackController` CRUD + FE `add-track.tsx`, `track-list.tsx` |
| 3.4 | Define subject areas / topics | ✅ | `SubjectAreaController` CRUD + FE `subject-area-manager.tsx` |
| 3.5 | Define fees / ticket types | 🔧 | `TicketTypeController` + `ticket-types.tsx` (FE done), payment flow incomplete |
| 3.6 | Add/remove Program Chair, Track Chair, Reviewers | ✅ | `ConferenceUserTrackController` + FE `config-members.tsx` full role management |
| 3.7 | View all submissions | ✅ | FE `paper-management.tsx` full table with filters, status, decisions |
| 3.8 | View all reviews | 🔧 | BE `ReviewController` has list API; no dedicated FE page for all-reviews view |
| 3.9 | View payments / registration data | 🔧 | `PaymentHistoryController`, `RegistrationController` exist; FE `payment-history-view.tsx` basic, no registration table |
| 3.10 | View attendees | 🔧 | `attendees-management.tsx` FE exists; missing dedicated BE list API |
| 3.11 | Final override on decisions | ✅ | `ReviewMetaReviewController` + FE `chair/decisions/page.tsx` decision console |
| 3.12 | Access analytics dashboard | 🔧 | `GET /conferences/{id}/stats` BE done; `ConferenceStatsDTO`; FE chair-dashboard already has charts |
| 3.13 | Send bulk emails / notifications | 🔧 | `EmailController` + `AuthorNotificationController` + FE `author-notification-wizard.tsx`; no bulk seat send |
| 3.14 | Export data (proceedings, attendee list, invoices) | 🔧 | `DocumentController` PDF download (ticket/letter), no Excel/CSV export for proceeds |
| 3.15 | Manage sponsors | ❌ | No entity/API/FE |
| 3.16 | Check-in management (QR scanner dashboard) | 📦 | `checkin-inline.tsx` exists; backend `RegistrationController.checkIn()` exists; no dedicated scanner UI |

---

## 4. Program Chair / Track Chair

| # | Functional Requirement | Status | Chi tiết |
|---|---|---|---|
| 4.1 | Design/customize review form & scoring criteria | ✅ | `ReviewQuestionController` CRUD + `ReviewQuestionChoice` with values + FE `review-questions-list.tsx` |
| 4.2 | Configure review type (single/double-blind) | ✅ | `TrackReviewSetting.isDoubleBlind` + FE `review-settings.tsx` |
| 4.3 | Launch bidding & auto-assignment | ✅ | Activity timeline enabling + `ReviewerAssignmentController.autoAssign()` + FE `reviewer-assignment.tsx` |
| 4.4 | Monitor review progress (coverage, overdue) | ❌ | No progress stats API; no FE coverage/overdue dashboard |
| 4.5 | Perform meta-reviews and make final decisions | ✅ | `ReviewMetaReviewController` + FE Decision Console |
| 4.6 | Build conference program (sessions, rooms, time slots) | ✅ | Program Builder FE rewritten (simplified CSS Grid), public viewer + session ratings |
| 4.7 | Approve camera-ready versions | ✅ | `PaperFileController.approveCameraReady()` + FE `camera-ready-management.tsx` |
| 4.8 | Send decision emails & notification batches | 🔧 | `AuthorNotificationController` + FE wizard, missing batch decision send |
| 4.9 | View papers in own track(s) only | 🔧 | Track filter on `PaperController` exists; no dedicated Track Chair filtered view page |
| 4.10 | Copy review questions between tracks | ✅ | `review-questions-copy-dialog.tsx` |

---

## 5. Reviewer

| # | Functional Requirement | Status | Chi tiết |
|---|---|---|---|
| 5.1 | Bid on papers (Yes / Maybe / No) | ✅ | `BiddingController` CRUD + FE `reviewer/bidding/page.tsx` |
| 5.2 | Download assigned papers & supplementary material | 🔧 | `PaperFile` entity + upload exists; no dedicated reviewer download endpoint |
| 5.3 | Submit reviews (scores + comments + confidence) | ✅ | `ReviewController` + `ReviewAnswerController` + FE `reviewer/review/[id]` |
| 5.4 | Discussion phase with other reviewers | ✅ | `ReviewCommentController` + FE discussion panel in Decision Console |
| 5.5 | Edit own reviews before deadline | ✅ | `ReviewController.updateReview()` + FE review page |
| 5.6 | See only assigned papers (double-blind: no author names) | ✅ | `PaperForBiddingDTO.isDoubleBlind` hides authors + FE reviewer console |
| 5.7 | Receive automatic reminders / overdue warnings | ✅ | `ReviewerReminderSchedulerService` `@Scheduled` daily job sends notifications to reviewers with pending reviews |
| 5.8 | Declare subject area interests | ✅ | `ReviewerInterestController` + FE `reviewer/interests/page.tsx` |

---

## 6. Author

| # | Functional Requirement | Status | Chi tiết |
|---|---|---|---|
| 6.1 | Register account & complete profile (affiliation, ORCID) | ✅ | `AuthController.register()` + `UserProfileController` + FE `auth/register` + `my-profile` |
| 6.2 | Submit new paper + upload PDF/supplementary | ✅ | `PaperController.create` + `PaperFileController.upload` + FE `track/[id]/submit` |
| 6.3 | Add co-authors to submission | ✅ | `PaperAuthorController` + FE multi-step submit |
| 6.4 | Edit/withdraw submission before deadline | ✅ | `PaperController.update()` + `withdrawPaper()` + FE `paper/[id]/page.tsx` |
| 6.5 | View real-time submission status | ✅ | FE `paper/page.tsx` with status badges, review summary, decision cards |
| 6.7 | Submit camera-ready version after acceptance | ✅ | `PaperFileController.uploadCameraReady()` + FE `author/camera-ready/page.tsx` |
| 6.8 | Register for conference & pay fee | 📦 | `Ticket` + `Payment` + `RegistrationController` exist; VNPay basic integration; FE `register/page.tsx` + `my-ticket/page.tsx` scaffolded but not wired |
| 6.9 | Receive notifications (in-app + email) | ✅ | `NotificationController` + FE `notification-bell.tsx` real-time polling + `EmailController` sends on events |
| 6.10 | Download acceptance letter, invoice, visa support letter | 🔧 | `DocumentController` PDF endpoints exist; FE download buttons on `my-ticket/page.tsx`; visa letter not yet implemented |

---

## 7. Attendee

| # | Functional Requirement | Status | Chi tiết |
|---|---|---|---|
| 7.1 | Register and pay conference fee online (multiple rates) | 📦 | `Ticket` + `Payment` + `RegistrationController` scaffolded; VNPay not fully wired; no multi-rate pricing |
| 7.2 | Receive QR code for on-site check-in | 📦 | `DocumentController` can generate PDF; no QR code generation yet |
| 7.3 | View full program & personal schedule | 🔧 | `program/page.tsx` public view exists; reads from published JSON blob |
| 7.4 | Bookmark favorite sessions | 🔧 | `localStorage` bookmarks in `program/page.tsx`; no server-side persistence |
| 7.5 | Receive push notifications before sessions | ❌ | Requires server-side session bookmarks + cron per session; deferred post-MVP |
| 7.6 | Rate sessions & give feedback | ✅ | `SessionRating` entity + `SessionRatingController` (BE) + star widget on public `program/page.tsx` (FE) |
| 7.7 | Download certificate of attendance | ✅ | `DocumentController.getCertificate()` endpoint + `DocumentGenerationService.generateCertificate()` (BE done) |

---

## 📊 Tổng kết

| Status | Số lượng | % |
|---|---|---|
| ✅ Đã implement (BE + FE) | 22 | 46% |
| 🔧 Có BE, chưa hoàn chỉnh FE | 13 | 27% |
| 📦 Có entity, chưa có logic | 4 | 8% |
| ❌ Chưa implement | 7 | 15% |
| 🚫 Ngoài phạm vi MVP | 6 | — (không tính) |
| **Tổng MVP** | **46** | **100%** |

---

## 🎯 Ưu tiên hoàn thành (theo đăng ký đồ án)

### Cao nhất — Cần hoàn thiện để demo
1. **Program Builder (4.6)** — Đơn giản hóa UI grid, xem plan `program_builder_simplified_plan.md`
2. **Registration + Payment (6.8 / 7.1)** — Wire up `RegistrationController` + VNPay callback vào FE
3. **QR Code check-in (7.2 / 3.16)** — Sinh QR từ `DocumentController`, tích hợp scanner UI

### Trung bình — Có BE rồi, chỉ cần FE
4. **Download tài liệu đầy đủ (6.10)** — Thêm visa support letter vào `DocumentController`
5. **Review progress monitoring (4.4)** — Thêm stats endpoint `/conferences/{id}/review-stats`

### Thấp hơn — Nice to have
6. **Analytics dashboard (3.12)** — API thống kê cơ bản + chart FE
7. **Bulk email (3.13)** — Nhóm lại decision email cùng batch
