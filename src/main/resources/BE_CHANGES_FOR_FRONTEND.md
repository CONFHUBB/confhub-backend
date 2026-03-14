# 📋 Backend Changes — Hướng Dẫn Cho Frontend

> **Ngày cập nhật:** 2026-03-14  
> **Commit:** `feat(business-rules): implement business rules for Flow 1, 2, 3`  
> **Branch:** `feature/conference` → merged vào `develop`

---

## Mục lục

1. [🔴 BREAKING CHANGES — Phải sửa ngay](#1--breaking-changes--phải-sửa-ngay)
2. [🟡 API MỚI — Cần tích hợp](#2--api-mới--cần-tích-hợp)
3. [🟢 API ĐÃ XÓA — Cần dọn code](#3--api-đã-xóa--cần-dọn-code)
4. [📦 DTO Changes — Cập nhật Types](#4--dto-changes--cập-nhật-types)
5. [⚙️ Business Logic Changes — Cần xử lý errors mới](#5--business-logic-changes--cần-xử-lý-errors-mới)
6. [🆕 Flow 3 APIs — Bidding & Review (chưa có FE)](#6--flow-3-apis--bidding--review-chưa-có-fe)

---

## 1. 🔴 BREAKING CHANGES — Phải sửa ngay

### 1.1. Paper Keywords: `keyword1-4` → `keywords: string[]`

**BE đã thay đổi:** `Paper` entity không còn `keyword1`, `keyword2`, `keyword3`, `keyword4`. Thay bằng field `keywordsJson` (JSON array).

**Response trả về:**
```json
// CŨ (không còn hoạt động)
{ "keyword1": "AI", "keyword2": "ML", "keyword3": "", "keyword4": "" }

// MỚI
{ "keywords": ["AI", "ML"] }
```

**Request gửi lên:**
```json
// CŨ
{ "keyword1": "AI", "keyword2": "ML", "keyword3": "", "keyword4": "" }

// MỚI
{ "keywords": ["AI", "ML", "Deep Learning"] }
```

**Files FE cần sửa:**

| File | Thay đổi |
|---|---|
| `types/paper.ts` → `PaperResponse` | Xóa `keyword1-4`, thêm `keywords: string[]` |
| `types/paper.ts` → `CreatePaperRequest` | Xóa `keyword1-4`, thêm `keywords: string[]` |
| `types/submission-form.ts` → `PaperSubmissionRequest` | Xóa `keyword1-4`, thêm `keywords: string[]` |
| `app/(main)/paper/[paperId]/page.tsx` | Thay 4 input fields → dynamic keyword list/tags input |
| `app/(main)/paper/page.tsx` (L197) | Thay `[paper.keyword1, ...]` → `paper.keywords` |
| `app/(main)/track/[trackId]/submit/page.tsx` (L455) | Thay `keyword1: fixedData.keyword1` → `keywords: fixedData.keywords` |
| `app/(main)/conference/[conferenceId]/submission-form/form-renderer.tsx` | Thay keyword1-4 schema → keywords array schema |

**Gợi ý UI:** Sử dụng tag input component (nhập keyword, nhấn Enter, hiện chip/badge) thay vì 4 ô input cố định.

---

### 1.2. PaperStatus mới

**BE đã thêm status mới cho Paper:**

```typescript
// CŨ
type PaperStatus = "SUBMITTED" | "UNDER_REVIEW" | "ACCEPTED" | "REJECTED"

// MỚI
type PaperStatus = "DRAFT" | "SUBMITTED" | "UNDER_REVIEW" | "ACCEPTED" | "REJECTED" | "WITHDRAWN" | "CAMERA_READY" | "PUBLISHED"
```

**FE cần update:**
- Trang "My Submissions" (`paper/page.tsx`): hiện badge cho các status mới (DRAFT, WITHDRAWN, CAMERA_READY, PUBLISHED)
- Logic filter/sort papers cần handle DRAFT + WITHDRAWN

---

### 1.3. PaperAuthor có thêm fields

**Response `PaperAuthor` giờ trả thêm:**
```json
{
  "id": 1,
  "paperId": 10,
  "userId": 5,
  "orderIndex": 0,
  "isPrimaryContact": true
}
```

**FE cần update:** Nếu hiện danh sách authors, nên sort theo `orderIndex` và đánh dấu `isPrimaryContact`.

---

## 2. 🟡 API MỚI — Cần tích hợp

### 2.1. Conference Management (Flow 1)

| Method | Endpoint | Mô tả |
|---|---|---|
| `PUT` | `/api/v1/conferences/{id}/complete` | Chuyển conference → COMPLETED |
| `PUT` | `/api/v1/conferences/{id}/cancel` | Chuyển conference → CANCELLED |

**FE cần thêm:** 2 buttons trong conference dashboard ("Complete Conference", "Cancel Conference").

**Thêm vào `conference.api.ts`:**
```typescript
export const completeConference = async (id: number): Promise<any> => {
    const response = await http.put(`/conferences/${id}/complete`)
    return response.data
}

export const cancelConference = async (id: number): Promise<any> => {
    const response = await http.put(`/conferences/${id}/cancel`)
    return response.data
}
```

---

### 2.2. Paper Withdraw/Restore (Flow 2)

| Method | Endpoint | Mô tả |
|---|---|---|
| `PUT` | `/api/v1/paper/{id}/withdraw` | Withdraw paper (SUBMITTED/UNDER_REVIEW → WITHDRAWN) |
| `PUT` | `/api/v1/paper/{id}/restore` | Restore paper (WITHDRAWN → SUBMITTED) |

**FE cần thêm:**
- Button "Withdraw" trong paper detail page (khi status = SUBMITTED hoặc UNDER_REVIEW)
- Button "Restore" trong paper detail page (khi status = WITHDRAWN)

**Thêm vào `paper.api.ts`:**
```typescript
export const withdrawPaper = async (paperId: number): Promise<PaperResponse> => {
    const response = await http.put<PaperResponse>(`/paper/${paperId}/withdraw`)
    return response.data
}

export const restorePaper = async (paperId: number): Promise<PaperResponse> => {
    const response = await http.put<PaperResponse>(`/paper/${paperId}/restore`)
    return response.data
}
```

---

## 3. 🟢 API ĐÃ XÓA — Cần dọn code

Các API sau **đã bị xóa hoàn toàn** trên BE. FE cần xóa code liên quan:

| API đã xóa | FE files cần dọn |
|---|---|
| `/api/v1/review-type` (CRUD) | `types/review-type.ts` → **XÓA FILE** |
| | `app/(main)/conference/[conferenceId]/update/review-type.tsx` → **XÓA FILE** |
| | `types/conference-form.ts` → xóa `ReviewTypeData` interface nếu có |
| `/api/v1/review-criterion` (CRUD) | Không thấy FE reference → OK |
| `/api/v1/review-score` (CRUD) | Không thấy FE reference → OK |
| `/api/v1/conference-review-form` (CRUD) | Không thấy FE reference → OK |
| `/api/v1/paper-check-log` (CRUD) | Không thấy FE reference → OK |
| `/api/v1/paper-rebuttal` (CRUD) | Không thấy FE reference → OK |
| `/api/v1/conference-bookmark` (CRUD) | Không thấy FE reference → OK |

> **Lưu ý:** Review Type (Single/Double Blind) giờ được cấu hình tại `TrackReviewSetting.isDoubleBlind` trong tab "Review Settings" (đã có UI `review-settings.tsx`). Không cần `review-type.tsx` nữa.

---

## 4. 📦 DTO Changes — Cập nhật Types

### 4.1. Cập nhật `types/paper.ts`

```typescript
// ✅ MỚI
export interface PaperResponse {
    id: number
    trackId: number
    track: {
        id: number
        name: string
        description: string
        conference: {
            id: number
            name: string
            acronym: string
            status: string
            // ...
        }
        maxSubmissions: number
    }
    primarySubjectAreaId: number
    secondarySubjectAreaIds: number[]
    title: string
    abstractField: string
    keywords: string[]          // ← THAY keyword1-4
    submissionTime: string
    isPassedPlagiarism: boolean
    status: PaperStatus         // ← Dùng union type
}

export type PaperStatus = 
    | "DRAFT" 
    | "SUBMITTED" 
    | "UNDER_REVIEW" 
    | "ACCEPTED" 
    | "REJECTED" 
    | "WITHDRAWN" 
    | "CAMERA_READY" 
    | "PUBLISHED"

export interface CreatePaperRequest {
    conferenceTrackId: number
    primarySubjectAreaId: number
    secondarySubjectAreaIds: number[]
    title: string
    abstractField: string
    keywords: string[]          // ← THAY keyword1-4
    submissionTime: string
    isPassedPlagiarism: boolean
    status: string
}
```

### 4.2. Cập nhật `types/submission-form.ts` → `PaperSubmissionRequest`

```typescript
export interface PaperSubmissionRequest {
    conferenceTrackId: number
    primarySubjectAreaId: number
    secondarySubjectAreaIds: number[]
    submissionFormId?: number
    title: string
    abstractField: string
    keywords: string[]          // ← THAY keyword1-4
    extraAnswersJson: string
    submissionTime: string
    isPassedPlagiarism?: boolean
    status?: string
}
```

---

## 5. ⚙️ Business Logic Changes — Cần xử lý errors mới

BE giờ trả **400 Bad Request** trong nhiều trường hợp mới. FE cần hiện thông báo lỗi phù hợp:

### 5.1. Paper Submission (Flow 2)

| Khi nào | Error message từ BE |
|---|---|
| Submit paper khi activity chưa bật | `"Paper submission is not currently open for this conference"` |
| Submit paper khi hết deadline | `"Paper submission deadline has passed"` |
| Edit paper đang UNDER_REVIEW | `"Cannot edit paper that is UNDER_REVIEW"` |
| Edit paper đã WITHDRAWN | `"Cannot edit paper that is WITHDRAWN"` |
| Xóa paper đã có reviews | `"Cannot delete paper that has reviews. Consider withdrawing instead."` |
| Withdraw paper không hợp lệ | `"Cannot withdraw paper with status: ..."` |

### 5.2. Conference Management (Flow 1)

| Khi nào | Error message từ BE |
|---|---|
| Bật PAPER_SUBMISSION khi chưa có subject areas | `"Cannot enable PAPER_SUBMISSION: track has no subject areas"` |
| Bật REVIEWER_BIDDING khi chưa có papers | `"Cannot enable REVIEWER_BIDDING: track has no submitted papers"` |
| Xóa Conference Chair cuối cùng | `"Cannot remove the last CONFERENCE_CHAIR"` |
| Gỡ Reviewer đang có assignments | `"Cannot remove REVIEWER role: user has active review assignments"` |

**FE recommendation:** Catch 400 errors và hiện toast/alert với `error.response.data.message`.

---

## 6. 🆕 Flow 3 APIs — Bidding & Review (chưa có FE)

Đây là các API mới, FE **chưa có pages tương ứng**, cần tạo mới.

### 6.1. Bidding APIs

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/v1/bidding` | Submit/update một bid |
| `GET` | `/api/v1/bidding/reviewer/{reviewerId}/conference/{conferenceId}` | Lấy tất cả bids của reviewer |
| `GET` | `/api/v1/bidding/paper/{paperId}` | Lấy tất cả bids cho paper |
| `GET` | `/api/v1/bidding/summary/{reviewerId}/{conferenceId}` | Summary bids |
| `GET` | `/api/v1/bidding/papers-for-bidding/{reviewerId}/{conferenceId}` | Danh sách papers để bid |
| `DELETE` | `/api/v1/bidding/{bidId}` | Xóa bid |

**DTOs cần tạo:**

```typescript
// types/bidding.ts
export type BidValue = "EAGER" | "WILLING" | "IN_A_PINCH" | "NOT_WILLING"

export interface BiddingRequest {
    paperId: number
    reviewerId: number
    bidValue: BidValue
}

export interface BiddingResponse {
    id: number
    paperId: number
    paperTitle: string
    reviewerId: number
    reviewerName: string
    bidValue: BidValue
    createdAt: string
    updatedAt: string
}

export interface PaperForBiddingDTO {
    paperId: number
    title: string
    abstractText: string | null   // null khi isDoubleBlind = true
    primarySubjectArea: string
    secondarySubjectAreas: string[]
    relevanceScore: number        // 0.0 - 1.0
    currentBid: BidValue | null   // null = chưa bid
    isDoubleBlind: boolean
}

export interface BidsSummaryDTO {
    reviewerId: number
    conferenceId: number
    bidCounts: Record<string, number>
    totalBids: number
    totalPapers: number
}
```

### 6.2. Review Assignment APIs

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/v1/reviewer-assignments/auto-assign` | Auto-assign reviewers |
| `POST` | `/api/v1/reviewer-assignments/confirm` | Confirm assignments |
| `POST` | `/api/v1/reviewer-assignments/manual-assign` | Manual assign |
| `DELETE` | `/api/v1/reviewer-assignments/{reviewId}` | Remove assignment |
| `GET` | `/api/v1/reviewer-assignments/conference/{conferenceId}` | Current assignments |

### 6.3. Review APIs

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/v1/reviews` | Tạo review |
| `PUT` | `/api/v1/reviews/{id}` | Update review (includes status transition) |
| `GET` | `/api/v1/reviews/{id}` | Get review by ID |
| `DELETE` | `/api/v1/reviews/{id}` | Delete review |

**Review Status Flow:**
```
ASSIGNED → IN_PROGRESS → COMPLETED
    ↓
  DECLINED
```

### 6.4. Review Answer APIs

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/v1/review-answer` | Submit/update answer |
| `GET` | `/api/v1/review-answer/review/{reviewId}` | Get all answers for review |

### 6.5. Meta-Review APIs

| Method | Endpoint | Mô tả |
|---|---|---|
| `POST` | `/api/v1/review-meta-reviews` | Tạo meta-review (auto-updates paper status) |
| `PUT` | `/api/v1/review-meta-reviews/{id}` | Update meta-review |
| `GET` | `/api/v1/review-meta-reviews/{id}` | Get by ID |
| `DELETE` | `/api/v1/review-meta-reviews/{id}` | Delete |

**Decision → Paper Status mapping:**
| Decision | Paper Status |
|---|---|
| `APPROVE` | → `ACCEPTED` |
| `REJECT` | → `REJECTED` |
| `REVISION` | Giữ nguyên (post-MVP) |

---

## 📝 Checklist cho FE team

### Ưu tiên cao (BREAKING — sẽ lỗi ngay)
- [x] Sửa `keyword1-4` → `keywords: string[]` ở tất cả files ✅
- [x] Update `PaperStatus` type (thêm DRAFT, WITHDRAWN, CAMERA_READY, PUBLISHED) ✅
- [x] Xóa `types/review-type.ts` và `review-type.tsx` ✅

### Ưu tiên trung bình (Missing features)
- [x] Thêm API functions: `withdrawPaper`, `restorePaper` ✅
- [x] Thêm API functions: `completeConference`, `cancelConference` ✅
- [x] Thêm buttons Withdraw/Restore trên paper detail page ✅
- [x] Handle new 400 error messages (toast notifications) ✅

### Ưu tiên thấp (New features — Flow 3)
- [ ] Tạo `types/bidding.ts`
- [ ] Tạo `app/api/bidding.api.ts`
- [ ] Tạo pages: Bidding list, Review form, Assignment dashboard, Meta-review
