# 📋 MVP Functional Requirements — Checklist

> **Ngày cập nhật:** 2026-03-17  
> **Dự án:** ConfMS — Conference Management System  
> **Legend:**  
> ✅ = Đã implement (BE + FE)  
> 🔧 = Có BE, chưa có FE  
> 📦 = Có entity/schema, chưa có logic  
> ❌ = Chưa implement  
> 🚫 = Không thuộc MVP hiện tại  

---

## 1. Conference Chair / Organizing Committee

| # | Functional Requirement | Status | Chi tiết |
|---|---|---|---|
| 1.1 | Create conference | ✅ | `ConferenceController.create` + FE `conference/create/page.tsx` |
| 1.2 | Edit conference | ✅ | `ConferenceController.update` + FE `conference/[id]/update/page.tsx` |
| 1.3 | Delete conference | ❌ | Chưa có API delete conference |
| 1.4 | Define important dates (start/end date) | ✅ | Trong `Conference` entity, FE conference form |
| 1.5 | Define tracks | ✅ | `ConferenceTrackController` CRUD + FE track manager |
| 1.6 | Define subject areas (topics) | ✅ | `SubjectAreaController` CRUD + FE subject area manager |
| 1.7 | Define fees | 📦 | `Ticket.price` entity tồn tại, chưa có CRUD API/FE cho fee config |
| 1.8 | Add/remove Program Chair, Track Chair | ✅ | `ConferenceUserTrackController` + role assignment |
| 1.9 | Add/remove Reviewers | ✅ | `ConferenceUserTrackController` + validation BR-1.9 |
| 1.10 | Add/remove Admins | 🔧 | `ConferenceUserTrack` hỗ trợ role, chưa có FE riêng |
| 1.11 | View all submissions | 🔧 | `PaperController` có API list, FE chỉ có "My Submissions" |
| 1.12 | View all reviews | 🔧 | `ReviewController` có API, chưa có FE page |
| 1.13 | View payments | 📦 | `Payment` entity + VNPay basic integration, chưa có dashboard FE |
| 1.14 | View attendees | 📦 | `Ticket` entity có user+conference, chưa có list API/FE |
| 1.15 | Final override on decisions | 🔧 | `ReviewMetaReviewController` có tạo meta-review, chưa có FE |
| 1.16 | Access analytics dashboard | ❌ | Chưa có statistics/analytics API hoặc FE |
| 1.17 | Manage sponsors | ❌ | Chưa có entity/API/FE |
| 1.18 | Send bulk emails/notifications | 🔧 | `EmailController` có API gửi email, `ConferenceTemplate` cho templates, chưa có FE bulk send |
| 1.19 | Export data (proceedings, attendee list, invoices) | ❌ | Chưa có export API |
| 1.20 | Conference status management | 🔧 | BE có `approveConference`, `completeConference`, `cancelConference`. FE chỉ có approve |

---

## 2. Program Chair / Track Chair

| # | Functional Requirement | Status | Chi tiết |
|---|---|---|---|
| 2.1 | Design/customize review form | ✅ | `ReviewQuestionController` CRUD + FE `review-questions-list.tsx`, `review-question-dialog.tsx` |
| 2.2 | Design scoring criteria (choices with values) | ✅ | `ReviewQuestionChoice` entity với `value` field + FE editor |
| 2.3 | Configure review type (single/double-blind) | ✅ | `TrackReviewSetting.isDoubleBlind` + FE `review-settings.tsx` |
| 2.4 | Launch bidding | 🔧 | `ConferenceActivity` REVIEWER_BIDDING enable/disable. FE có activity timeline UI |
| 2.5 | Auto-assignment of reviewers | ✅ | `ReviewerAssignmentController.autoAssign()` API + FE `reviewer-assignment.tsx` (auto/manual assign, preview, confirm) (2026-03-17) |
| 2.6 | Monitor review progress | ❌ | Chưa có progress tracking API (coverage, overdue) |
| 2.7 | Perform meta-reviews & final decisions | 🔧 | `ReviewMetaReviewController` CRUD + auto paper status update (BR-3.21), chưa có FE |
| 2.8 | Build conference program (sessions, rooms, time slots) | ❌ | Chưa có entities/API |
| 2.9 | Approve camera-ready versions | ❌ | `PaperStatus.CAMERA_READY` có enum, chưa có approval flow |
| 2.10 | Send decision emails | 🔧 | `EmailController` + `ConferenceTemplate` sẵn, chưa có batch decision email logic |
| 2.11 | View papers in own track(s) | 🔧 | API filter by track tồn tại, chưa có FE page riêng cho Track Chair |
| 2.12 | Copy review questions giữa tracks | ✅ | FE có `review-questions-copy-dialog.tsx` |

---

## 3. Reviewer

| # | Functional Requirement | Status | Chi tiết |
|---|---|---|---|
| 3.1 | Bid on papers (Yes/Maybe/No) | ✅ | `BiddingController` full CRUD + FE `/conference/[id]/reviewer/bidding` |
| 3.2 | Download assigned papers | ❌ | `PaperFile` entity tồn tại, chưa có download API for reviewers |
| 3.3 | Submit reviews (scores + comments) | ✅ | `ReviewController` + `ReviewAnswerController` + FE `/conference/[id]/reviewer/review/[reviewId]` |
| 3.4 | Discussion phase with other reviewers | 🔧 | `ReviewComment` entity có, chưa có discussion thread FE |
| 3.5 | Edit own reviews before deadline | ✅ | `ReviewController.updateReview()` + FE review page |
| 3.6 | See only assigned papers (double-blind) | ✅ | `PaperForBiddingDTO.isDoubleBlind` + FE reviewer console |
| 3.7 | Receive automatic reminders | ❌ | Chưa có scheduled reminder system |
| 3.8 | Mobile app: read offline | ❌ | Chưa có mobile app |

---

## 4. Author

| # | Functional Requirement | Status | Chi tiết |
|---|---|---|---|
| 4.1 | Register account | ✅ | `AuthController.register()` + FE `auth/register/page.tsx` |
| 4.2 | Complete profile (affiliation, ORCID) | ✅ | `UserProfileController` CRUD + FE `my-profile/page.tsx` |
| 4.3 | Submit new paper + upload PDF | ✅ | `PaperController.create` + `PaperFileController.upload` + FE `track/[trackId]/submit/page.tsx` |
| 4.4 | Add co-authors | ✅ | `PaperAuthorController` + FE "Add Authors" step |
| 4.5 | Edit submission before deadline | ✅ | `PaperController.update()` + validation BR-2.13 + FE `paper/[paperId]/page.tsx` |
| 4.6 | Withdraw submission | ✅ | `PaperController.withdrawPaper()` + FE button trong `paper/[paperId]/page.tsx` |
| 4.7 | ~~Plagiarism & format-check~~ | 🚫 | Đã xóa `isPassedPlagiarism` — không thuộc MVP (2026-03-15) |
| 4.8 | View real-time submission status | ✅ | FE `paper/page.tsx` redesigned: summary cards + card layout + status badges |
| 4.9 | Upload rebuttal (if enabled) | ❌ | `PaperRebuttal` entity đã bị xóa. Cần tạo lại nếu enable |
| 4.10 | Submit camera-ready version | ❌ | `PaperStatus.CAMERA_READY` có enum, chưa có upload flow riêng |
| 4.11 | Register for conference & pay fee | 📦 | `Ticket` + `Payment` entities, VNPay basic. Chưa có FE registration flow |
| 4.12 | Receive notifications (desktop + email + push) | ✅ | `Notification` entity hoàn chỉnh + FE `notification-bell.tsx` real-time polling |
| 4.13 | Download acceptance letter, invoice, visa support letter | ❌ | Chưa có document generation |

---

## 5. Attendee

| # | Functional Requirement | Status | Chi tiết |
|---|---|---|---|
| 5.1 | Register and pay conference fee online | 📦 | `Ticket` + `Payment` + VNPay integration cơ bản, chưa có FE flow |
| 5.2 | Receive QR code for check-in | ❌ | Chưa implement |
| 5.3 | View full program & personal schedule | ❌ | Chưa có program/session entities |
| 5.4 | Bookmark favorite sessions | ❌ | `ConferenceBookmark` entity đã bị xóa. Cần tạo `SessionBookmark` |
| 5.5 | Receive push notifications before sessions | ❌ | Chưa có push notification system |
| 5.6 | Rate sessions & give feedback | ❌ | Chưa có rating entity/API |
| 5.7 | Download certificate of attendance | ❌ | Chưa có certificate generation |

---

## 6. Admin / Staff (Chưa cần MVP)

| # | Functional Requirement | Status | Ghi chú |
|---|---|---|---|
| 6.1 | Create/edit/delete Staff accounts | 🚫 | Post-MVP |
| 6.2 | Master Dashboard (global stats) | 🚫 | Post-MVP |
| 6.3 | Platform-wide financial reports | 🚫 | Post-MVP |
| 6.4 | View pending conference requests | 🚫 | Post-MVP |
| 6.5 | Validate organizer details | 🚫 | Post-MVP |
| 6.6 | Monitor/suspend non-compliant events | 🚫 | Post-MVP |

---

## 📊 Tổng kết

| Status | Số lượng | % |
|---|---|---|
| ✅ Đã implement (BE + FE) | 20 | 43% |
| 🔧 Có BE, chưa có FE | 9 | 20% |
| 📦 Có entity, chưa có logic | 5 | 11% |
| ❌ Chưa implement | 11 | 24% |
| 🚫 Không thuộc MVP | 1 | 2% |
| **Tổng (MVP)** | **46** | **100%** |

> **2026-03-17:** 2.5 Auto-assignment (🔧→✅), FE reviewer assignment UI hoàn chỉnh.

### Khu vực cần ưu tiên

**1. FE cho features đã có BE (🔧 → ✅):**
- ~~Reviewer Assignment dashboard cho Program Chair~~ ✅ (2026-03-17)
- Meta-review page cho Program Chair
- Conference complete/cancel cho Chair
- Papers-in-track view cho Track Chair
- Discussion thread cho Reviewers

**2. Logic cần hoàn thiện (📦 → 🔧):**
- Ticket/Payment CRUD + FE registration flow
- Conference fee configuration

**3. Features hoàn toàn mới (❌):**
- Conference program builder (sessions, rooms, time slots)
- Camera-ready submission flow
- Analytics dashboard
- Export data (PDF, Excel)
- Reminder/scheduling system
- QR check-in
- Certificate generation
