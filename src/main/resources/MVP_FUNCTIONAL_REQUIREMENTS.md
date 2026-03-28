# 📋 MVP Functional Requirements — Checklist

> **Cập nhật:** 2026-03-27  
> **Dự án:** ConfHub — Conference Management System  
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
| 3.2 | Define important dates (start/end, deadlines) | ✅ | `Conference` entity + `ConferenceActivity` deadlines, FE `activity-timeline.tsx` |
| 3.3 | Define tracks | ✅ | `ConferenceTrackController` CRUD + FE `add-track.tsx`, `track-list.tsx` |
| 3.4 | Define subject areas / topics | ✅ | `SubjectAreaController` CRUD + FE `subject-area-manager.tsx` |
| 3.5 | Define fees / ticket types | ✅ | `TicketTypeController` CRUD + FE `ticket-types.tsx` + VNPay payment flow (`PaymentController`) fully wired |
| 3.6 | Add/remove Program Chair, Track Chair, Reviewers | ✅ | `ConferenceUserTrackController` + FE `config-members.tsx` full role management |
| 3.7 | View all submissions | ✅ | FE `paper-management.tsx` full table with filters, status, decisions, CSV export |
| 3.8 | View all reviews | ✅ | `ReviewController` + FE `review-management.tsx` table with reviewer, score, coverage |
| 3.9 | View payments / registration data | ✅ | `PaymentHistoryController` `/my-payment-history` global endpoint + FE `payment-history-view.tsx` + `/my-profile/payments` cross-conference view |
| 3.10 | View attendees | 🔧 | `attendees-management.tsx` FE exists; missing dedicated BE paginated list API |
| 3.11 | Final override on decisions | ✅ | `ReviewMetaReviewController` + FE `chair/decisions/page.tsx` decision console |
| 3.12 | Access analytics dashboard | ✅ | `GET /conferences/{id}/stats` + `ConferenceStatsDTO` + FE `chair-dashboard.tsx` with charts (revenue, acceptance rate, registrations) |
| 3.13 | Send bulk emails / notifications | 🔧 | `EmailController` + `AuthorNotificationController` + FE `author-notification-wizard.tsx`; missing batch-by-decision email grouping |
| 3.14 | Export data (proceedings, attendee list, invoices) | 🔧 | `DocumentController` PDF (ticket/letter), CSV export on paper table; no proceedings Excel yet |
| 3.15 | Manage sponsors | ❌ | No entity/API/FE |
| 3.16 | Check-in management (QR scanner dashboard) | ✅ | `checkin-inline.tsx` + `RegistrationController.checkIn()` + `checkin/page.tsx` dedicated scanner UI; check-in status tracked |

---

## 4. Program Chair / Track Chair

| # | Functional Requirement | Status | Chi tiết |
|---|---|---|---|
| 4.1 | Design/customize review form & scoring criteria | ✅ | `ReviewQuestionController` CRUD + `ReviewQuestionChoice` with values + FE `review-questions-list.tsx` |
| 4.2 | Configure review type (single/double-blind) | ✅ | `TrackReviewSetting.isDoubleBlind` + FE `review-settings.tsx` |
| 4.3 | Launch bidding & auto-assignment | ✅ | Activity timeline enabling + `ReviewerAssignmentController.autoAssign()` + FE `reviewer-assignment.tsx` |
| 4.4 | Monitor review progress (coverage, overdue) | ✅ | `review-management.tsx` shows reviewCount/completedReviewCount per paper; overdue info via `ReviewerReminderSchedulerService` |
| 4.5 | Perform meta-reviews and make final decisions | ✅ | `ReviewMetaReviewController` + FE Decision Console with APPROVE/REJECT/REVISION |
| 4.6 | Build conference program (sessions, rooms, time slots) | ✅ | Program Builder FE with CSS Grid (`grid-builder-panel.tsx`, `program-builder.tsx`), public viewer + session ratings |
| 4.7 | Approve camera-ready versions | ✅ | `PaperFileController.approveCameraReady()` + FE `camera-ready-management.tsx` |
| 4.8 | Send decision emails & notification batches | 🔧 | `AuthorNotificationController` + FE wizard; missing auto-batch per decision outcome |
| 4.9 | View papers in own track(s) only | 🔧 | Track filter on `PaperController` exists; no dedicated Track Chair filtered view page |
| 4.10 | Copy review questions between tracks | ✅ | `review-questions-copy-dialog.tsx` |

---

## 5. Reviewer

| # | Functional Requirement | Status | Chi tiết |
|---|---|---|---|
| 5.1 | Bid on papers (Yes / Maybe / No) | ✅ | `BiddingController` CRUD + FE `reviewer/bidding/page.tsx` |
| 5.2 | Download assigned papers & supplementary material | 🔧 | `PaperFile` entity + upload exists; `PaperFileController` has URLs; no dedicated reviewer secure-download endpoint |
| 5.3 | Submit reviews (scores + comments + confidence) | ✅ | `ReviewController` + `ReviewAnswerController` + FE `reviewer/review/[id]` |
| 5.4 | Discussion phase with other reviewers | ✅ | `ReviewCommentController` + FE `discussion-panel.tsx` in Decision Console |
| 5.5 | Edit own reviews before deadline | ✅ | `ReviewController.updateReview()` + FE review page |
| 5.6 | See only assigned papers (double-blind: no author names) | ✅ | `PaperForBiddingDTO.isDoubleBlind` hides authors + FE reviewer console |
| 5.7 | Receive automatic reminders / overdue warnings | ✅ | `ReviewerReminderSchedulerService` `@Scheduled` daily job sends notifications to reviewers with pending reviews |
| 5.8 | Declare subject area interests | ✅ | `ReviewerInterestController` + FE `reviewer/interests/page.tsx` |
| 5.9 | Declare conflicts of interest | ✅ | `UserConflictController` + `PaperConflictController` + FE `conflict-management.tsx` + `review-conflict-config.tsx` |

---

## 6. Author

| # | Functional Requirement | Status | Chi tiết |
|---|---|---|---|
| 6.1 | Register account & complete profile (affiliation, ORCID) | ✅ | `AuthController.register()` + `UserProfileController` + FE `auth/register` + `/my-profile` sidebar (Profile, Tickets, Payments, Papers) |
| 6.2 | Submit new paper + upload PDF/supplementary | ✅ | `PaperController.create` + `PaperFileController.upload` + FE multi-step `submission-form` |
| 6.3 | Add co-authors to submission | ✅ | `PaperAuthorController` + FE multi-step submit |
| 6.4 | Edit/withdraw submission before deadline | ✅ | `PaperController.update()` + `withdrawPaper()` + FE `paper/[id]/page.tsx` |
| 6.5 | View real-time submission status | ✅ | FE Author Workspace `author/page.tsx` — Status Tracker + My Papers table (score, decision, reviews) |
| 6.6 | Upload rebuttal (if enabled) | ❌ | No rebuttal entity/API/FE |
| 6.7 | Submit camera-ready version after acceptance | ✅ | `PaperFileController.uploadCameraReady()` + FE `author/camera-ready/page.tsx` |
| 6.8 | Register for conference & pay fee | ✅ | `RegistrationController` + `PaymentController` + VNPay callback + FE `register/page.tsx` → `my-ticket/page.tsx`; ticket & payment status persisted |
| 6.9 | Receive notifications (in-app + email) | ✅ | `NotificationController` + FE `notification-bell.tsx` real-time polling + `EmailController` sends on events |
| 6.10 | Download acceptance letter, invoice, visa support letter, certificate | ✅ | `DocumentController`: 4 PDF endpoints (acceptance, invoice, visa, certificate) + FE download buttons in Author Workspace My Ticket tab + `/my-profile/tickets` |

---

## 7. Attendee

| # | Functional Requirement | Status | Chi tiết |
|---|---|---|---|
| 7.1 | Register and pay conference fee online (multiple rates) | ✅ | `TicketTypeController` multi-type pricing + `RegistrationController` + VNPay + FE `register/page.tsx` |
| 7.2 | Receive QR code for on-site check-in | ✅ | `qrcode` library generates QR in FE from `ticket.qrCode`; displayed in My Ticket & `/my-profile/tickets` |
| 7.3 | View full program & personal schedule | ✅ | `program/page.tsx` public view reads published JSON blob; session list with time/room |
| 7.4 | Bookmark favorite sessions | 🔧 | `localStorage` bookmarks in `program/page.tsx`; no server-side persistence |
| 7.5 | Receive push notifications before sessions | ❌ | Requires server-side session bookmarks + cron per session; deferred post-MVP |
| 7.6 | Rate sessions & give feedback | ✅ | `SessionRating` entity + `SessionRatingController` (BE) + star widget on public `program/page.tsx` (FE) |
| 7.7 | Download certificate of attendance | ✅ | `DocumentController.getCertificate()` + `DocumentGenerationService.generateCertificate()` + FE download button in My Ticket |

---

## 📊 Tổng kết

| Status | Số lượng | % |
|---|---|---|
| ✅ Đã implement (BE + FE) | 33 | 69% |
| 🔧 Có BE, chưa hoàn chỉnh FE | 7 | 15% |
| 📦 Có entity, chưa có logic | 0 | 0% |
| ❌ Chưa implement | 5 | 10% |
| 🚫 Ngoài phạm vi MVP | 6 | — (không tính) |
| **Tổng MVP** | **48** | **100%** |

---

## 🎯 Còn lại cần hoàn thiện

### Trung bình — Có BE rồi, chỉ cần FE
1. **Attendees list (3.10)** — Thêm BE pagination API `/conferences/{id}/attendees` + FE table
2. **Reviewer download (5.2)** — Endpoint secure download file PDF cho reviewer được assign
3. **Batch decision email (4.8)** — Gom các decision notifications vào 1 batch send khi chair chốt

### Thấp hơn — Nice to have
4. **Bookmark server-side (7.4)** — Persist bookmarks vào DB per user per session
5. **Rebuttal (6.6)** — Entity + API + FE rebuttal upload phase

### Ngoài scope MVP
6. **Sponsor management (3.15)** — Deferred post-MVP
7. **Track Chair filtered view (4.9)** — Track Chair sees own track only; currently all tracks visible
