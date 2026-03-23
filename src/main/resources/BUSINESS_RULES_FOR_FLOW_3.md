# BUSINESS RULES FOR FLOW 3: Reviewer Bid Bài / Review Bài

> Tham khảo: [CMT3 Review Settings](https://cmt3.research.microsoft.com/docs/help/chair/review-settings.html),
> [CMT3 Author Submission](https://cmt3.research.microsoft.com/docs/help/author/author-submission-form.html)  
> So sánh với: confms-backend hiện tại

---

## 1. REVIEWER SUBJECT AREAS (Trước khi bid)

### BR-3.1: Đăng ký Subject Areas
- Reviewer chọn subject areas từ danh sách conference + expertise level
- Expertise levels: EXPERT, KNOWLEDGEABLE, INTERESTED
- Nếu `TrackReviewSetting.requireSubjectAreas = true` → bắt buộc chọn trước khi bid
- ReviewerInterest entity: reviewer + subjectArea + expertise

### BR-3.2: Dùng để tính relevance
- Subject areas reviewer chọn → so sánh với paper subject areas → relevance score
- Expertise level ảnh hưởng score (EXPERT > KNOWLEDGEABLE > INTERESTED)

---

## 2. BIDDING

### BR-3.3: Điều kiện bidding
- chỉ set `REVIEWER_BIDDING` `isEnabled = true` khi đã setup review form cho từng track 
- `ConferenceActivity` loại `REVIEWER_BIDDING` phải `isEnabled = true`
- Chỉ users có role `REVIEWER` trong conference mới bid được
- Chỉ bid trước deadline

### BR-3.4: Papers hiển thị cho bidding
- Reviewer thấy **tất cả papers** trong conference
- **LOẠI TRỪ**: papers mà reviewer có conflict of interest (PaperConflict)
- **LOẠI TRỪ**: papers có status `WITHDRAWN`
- **LOẠI TRỪ**: papers có status `REVISION` (đang trong revision cycle)
- Hiển thị: title, abstract, subject areas, relevance score
- **Double Blind**: nếu `isDoubleBlind = true` → ẩn tên authors

### BR-3.5: Bid values
| Giá trị | Ý nghĩa | Score |
|---|---|---|
| EAGER | Rất muốn review | 1.0 |
| WILLING | Sẵn sàng review | 0.75 |
| IN_A_PINCH | Chỉ khi thật cần | 0.25 |
| NOT_WILLING | Không muốn | 0.0 |
| (Not Entered) | Chưa bid | 0.25 (neutral) |

### BR-3.6: Bid rules
- 1 reviewer chỉ có 1 bid per paper (upsert)
- Reviewer có thể thay đổi bid bất cứ lúc nào (trước deadline)
- Bid cho paper mà mình có conflict → hệ thống REJECT (không cho)
- Xóa bid → quay về trạng thái "Not Entered"

---

## 3. RELEVANCE SCORE

### BR-3.7: Cách tính
```
relevanceScore = primaryMatch * 0.6 + secondaryMatch * 0.4

primaryMatch  = 1.0 nếu reviewer interest chứa paper.primarySubjectArea, else 0.0
secondaryMatch = matchCount / totalSecondary (tỷ lệ SA secondary trùng)
```

### BR-3.8: Score range
- Kết quả: 0.0 → 1.0
- 0.0 = không liên quan
- 1.0 = match hoàn hảo (cả primary + tất cả secondary)

---

## 4. AUTO-ASSIGN REVIEWERS

### BR-3.9: Config auto-assign
| Param | Default | Mô tả |
|---|---|---|
| conferenceId | required | Conference để assign |
| minReviewersPerPaper | 3 | Số reviewer tối thiểu/paper |
| maxPapersPerReviewer | 5 | Số paper tối đa/reviewer |
| bidWeight | 0.6 | Trọng số bid score |
| relevanceWeight | 0.4 | Trọng số relevance |
| loadBalancing | false | Ưu tiên phân bổ đều papers cho reviewers |

### BR-3.10: Thuật toán (Greedy Weighted Scoring)
1. Tính `combinedScore = bidScore * bidWeight + relevanceScore * relevanceWeight` cho mỗi cặp (paper, reviewer)
2. Lọc: bỏ conflicts (bao gồm domain conflicts), bỏ pairs đã assigned
3. **Domain Conflict Detection (2026-03-17):** So sánh email domain reviewer vs authors → block nếu cùng institutional domain (loại trừ public domains: gmail.com, yahoo.com, hotmail.com, outlook.com)
4. Sắp xếp theo score giảm dần
5. Greedy assign: chọn top candidate, kiểm tra constraints:
   - Paper chưa đến `minReviewersPerPaper`?
   - Reviewer chưa đạt `maxPapersPerReviewer`?
6. **Load Balancing (2026-03-17):** Nếu `loadBalancing = true`, ưu tiên reviewers có ít papers hơn (dùng penalty factor)
7. Trả preview (KHÔNG lưu DB ngay)

### BR-3.11: Preview → Confirm flow
1. **POST /auto-assign** → trả preview (danh sách assignments + stats)
2. Chair xem preview: unassigned papers, overloaded reviewers
3. Chair có thể manual adjust (add/remove)
4. **POST /confirm** → lưu vào DB (tạo Review records)

### BR-3.12: Manual assign rules
- Chair có thể manual assign bất kỳ reviewer cho paper
- Không cho assign nếu có conflict
- Không cho assign trùng (reviewer đã assigned cho paper đó)
- Manual assign tạo Review record ngay (không cần confirm)

### BR-3.13: Remove assignment
- Chair xóa assignment = delete Review record
- Chỉ xóa được nếu review chưa COMPLETED

---

## 5. REVIEW PROCESS

### BR-3.14: Review status flow
```
ASSIGNED → IN_PROGRESS → COMPLETED
    ↓
  DECLINED
```

| Từ | Đến | Khi nào |
|---|---|---|
| ASSIGNED → IN_PROGRESS | Reviewer bắt đầu trả lời review questions |
| ASSIGNED → DECLINED | Reviewer từ chối review |
| IN_PROGRESS → COMPLETED | Reviewer submit tất cả required answers |
| COMPLETED → IN_PROGRESS | Chair allow edit / reviewer sửa answer |

### BR-3.15: Điều kiện review
- `ConferenceActivity` loại `REVIEW_SUBMISSION` phải `isEnabled = true`
- Reviewer chỉ review papers mà mình đã ASSIGNED
- Double Blind: reviewer không thấy tên authors

### BR-3.16: Review Questions
- Chair tạo questions per track (`ReviewQuestion` entity)
- Question types: COMMENT (text), AGREEMENT (yes/no), OPTIONS (radio), OPTIONS_WITH_VALUE (radio + numeric value)
- Mỗi question có: text, note, orderIndex, isRequired, visibility settings
- Questions có choices (ReviewQuestionChoice) cho loại OPTIONS/OPTIONS_WITH_VALUE

### BR-3.17: Review Answers
- Reviewer trả lời questions → tạo `ReviewAnswer` records
- 1 ReviewAnswer per (review, question) — upsert logic
- Answer lưu: answerValue (text/numeric) + selectedChoice (nếu là OPTIONS)
- Required questions phải trả lời trước khi submit

### BR-3.18: Review completion
- Review COMPLETED khi tất cả required questions đã trả lời
- Tính totalScore từ OPTIONS_WITH_VALUE answers (weighted average)
- `Review.totalScore` = tổng/trung bình scores

---

## 6. REVIEW DISCUSSION

### BR-3.19: Discussion phase
- `ConferenceActivity` loại `REVIEW_DISCUSSION` phải `isEnabled = true`
- Reviewers assigned cho cùng paper có thể thảo luận
- `ReviewComment` entity: review + content + isVisibleToAuthor
- Chair config: `allowReviewUpdateDuringDiscussion` → reviewer có thể sửa answers

---

## 7. META-REVIEW (Quyết định cuối)

### BR-3.20: Meta-review
- Program Chair / Conference Chair đưa ra quyết định cuối cho paper
- `ReviewMetaReview` entity: paper + user + finalDecision + reason
- Decision values: APPROVE, REJECT, REVISION
- 1 meta-review per paper (final decision)

### BR-3.21: Khi có meta-review
- APPROVE → Paper.status = ACCEPTED
- REJECT → Paper.status = REJECTED
- REVISION → Mở revision cycle (post-MVP)

---

## 8. REVIEW SETTINGS CHI TIẾT (per Track)

> Tham khảo: [CMT3 Review Settings](https://cmt3.research.microsoft.com/docs/help/chair/review-settings.html)

### BR-3.22: Bảng settings đầy đủ

| Setting | Type | Default | Mô tả | Đã có? |
|---|---|---|---|---|
| `isDoubleBlind` | boolean | true | Ẩn danh reviewer + author. **Lưu ý**: chỉ ẩn trong hệ thống, KHÔNG sửa nội dung file PDF | ✅ |
| `requireSubjectAreas` | boolean | false | Bắt reviewer chọn Subject Areas trước khi bid | ✅ |
| `allowReviewerQuota` | boolean | false | Reviewer tự đặt quota papers khi accept invite | ✅ |
| `reviewerInviteExpirationDays` | int | 7 | Thời hạn lời mời reviewer (ngày) | ✅ |
| `allowReviewUpdateDuringDiscussion` | boolean | false | Reviewer có thể sửa review trong Discussion phase | ✅ |
| `allowOthersReviewAccessAfterSubmit` | boolean | false | Reviewer phải submit review trước khi xem review người khác | ✅ |
| `reviewerInstructions` | text | null | Hướng dẫn cho reviewer hiển thị trong Reviewer Console | ✅ |
| `showReviewerIdentityToOtherReviewer` | boolean | false | Reviewer thấy tên reviewer khác trong Discussion | ❌ |
| `showAggregateColumns` | boolean | false | Hiện cột aggregate (avg score) trên Chair Console | ❌ |
| `allowReviewerSeeStatusBeforeNotification` | boolean | false | Reviewer thấy paper status trước khi Author Notification | ❌ |
| `enableAllPapersForDiscussion` | boolean | false | Tự enable tất cả papers khi bật Discussion activity | ❌ |
| `allowDiscussNonAssignedPapers` | boolean | false | Reviewer discuss papers không assigned (nếu không conflict) | ❌ |
| `allowAuthorDiscuss` | boolean | false | Author tham gia discussion (chỉ sau khi PC phát discussion đầu tiên) | ❌ |
| `notifyReviewerOnReviewUpdateDuringDiscussion` | boolean | false | Notify reviewers khi review bị update trong Discussion | ❌ |
| `notifyOnManualAssignment` | boolean | false | Gửi email cho reviewer khi Chair manual assign paper | ❌ |
| `doNotShowWithdrawnPapers` | boolean | false | Ẩn papers bị Withdrawn khỏi Reviewer Console | ❌ |

### BR-3.23: Rules khi thay đổi settings
- Thay đổi settings **KHÔNG ảnh hưởng** invitations đã gửi (VD: đổi expirationDays chỉ apply cho invites mới)
- Bật `isDoubleBlind` + `REVIEWER_BIDDING` cùng lúc → yêu cầu phải lock conflicts (không cho edit)
- Settings áp dụng **per track** → multi-track conference có thể khác settings mỗi track
- Copy settings giữa tracks được (CMT3 có "Copy Settings to Another Track")

---

## 9. CONFLICT MANAGEMENT

> Tham khảo: [CMT3 Manage Conflicts](https://cmt3.research.microsoft.com/docs/help/chair/conflicts.html),
> [CMT3 Dispute Conflicts](https://cmt3.research.microsoft.com/docs/help/chair/dispute-conflicts.html)

### BR-3.24: Hai loại conflict

| Loại | Mô tả | Cách phát hiện | BE Status |
|---|---|---|---|
| **Individual Conflict** | Conflict giữa 2 người cụ thể (per-submission hoặc per-user) | User tự khai báo, Chair import, hoặc Chair set thủ công | ✅ `PaperConflict` + `UserConflict` |
| **Domain Conflict** | Conflict dựa trên institutional email domain | So sánh domain email reviewer vs authors (auto-detect) | ✅ `DomainConflictUtil` |

### BR-3.25: Individual Conflict types (5 loại theo CMT3)

| ConflictType | Mô tả | Thời hạn |
|---|---|---|
| `CO_AUTHOR` | Đồng tác giả | Bất kỳ lúc nào |
| `COLLEAGUE` | Đồng nghiệp | Trong 2 năm gần nhất |
| `COLLABORATOR` | Cộng tác nghiên cứu | Trong 2 năm gần nhất |
| `THESIS_ADVISOR` | Người hướng dẫn luận văn | Mọi thời điểm |
| `RELATIVE_FRIEND` | Người thân hoặc bạn bè | Mọi thời điểm |

> **Mapping với backend**: `ConflictType` enum đã có đủ 7 values (CO_AUTHOR, PERSONAL, DOMAIN, COLLEAGUE, COLLABORATOR, THESIS_ADVISOR, RELATIVE_FRIEND). Trong đó PERSONAL = general, DOMAIN = auto-detected.

### BR-3.25b: Ai set conflict?

| Ai | Cách set | Khi nào |
|---|---|---|
| **Chair** | Set thủ công qua FE Conflict Management hoặc import file | Bất kỳ lúc nào |
| **Author** | Khai báo conflict khi submit paper (per-submission) | Khi `enableAuthorMarkSubmissionConflicts = true` |
| **Reviewer / PC Member** | Khai báo individual conflicts qua profile | Khi `enablePCMemberMarkConflicts = true` |

> **MVP Scope**: Hiện tại chỉ Chair set conflict qua FE (`conflict-management.tsx`). Author/Reviewer tự khai báo là post-MVP.

### BR-3.26: Domain Conflict rules
- So sánh domain email reviewer vs author emails (dùng `DomainConflictUtil`)
- **Loại trừ public domains**: gmail.com, yahoo.com, hotmail.com, outlook.com, aol.com, icloud.com
- Domain conflict **tự động được phát hiện** khi auto-assign (trong `ReviewerAssignmentServiceImpl`)
- Domain conflict **không thể dispute** (theo CMT3)
- **QUAN TRỌNG**: Nếu domain conflict phát sinh giữa review phase → reviewer bị remove khỏi paper
- Khuyến nghị: Lock domain conflicts trong review phase ("Do not allow editing personal domain conflicts")

### BR-3.26b: Conflict Settings (per Track) — CMT3 Features > Conflicts

| Setting | Type | Default | Mô tả | MVP? |
|---|---|---|---|---|
| `enableAuthorMarkSubmissionConflicts` | boolean | false | Author có thể đánh dấu conflict với PC members khi submit paper | ❌ Post-MVP |
| `enablePCMemberMarkConflicts` | boolean | false | PC Members/Reviewers tự khai báo individual conflicts | ❌ Post-MVP |
| `doNotAllowEditConflicts` | boolean | false | Lock editing individual conflicts (Chair enable sau khi tất cả đã nhập) | ❌ Post-MVP |
| `allowEditConflictsIfNoneEntered` | boolean | false | Cho phép nhập conflicts nếu chưa nhập gì (dù đã lock) | ❌ Post-MVP |
| `enableDisputeSubmissionConflicts` | boolean | false | Cho phép PC members dispute author-entered conflicts | ❌ Post-MVP |
| `doNotAllowEditDomainConflicts` | boolean | false | Lock editing domain conflicts | ❌ Post-MVP |

> **MVP**: Chair trực tiếp quản lý conflicts qua Conflict Management UI. Các settings trên là post-MVP.

### BR-3.27: Conflict enforcement trong bidding/assignment
- Papers mà reviewer có conflict → **ẩn** khỏi bidding list
- Auto-assign **skip** reviewer-paper pairs có conflict (cả individual + domain)
- Manual assign **block** nếu phát hiện conflict
- Nếu Double Blind + Bidding → phải lock conflicts (không cho edit) để tránh author identification

### BR-3.27b: Dispute Conflicts (Post-MVP)

> Tham khảo: [CMT3 Dispute Conflicts](https://cmt3.research.microsoft.com/docs/help/chair/dispute-conflicts.html)

**Flow chi tiết (khi enable):**
1. Author khai báo conflict với Reviewer A khi submit paper
2. Reviewer A thấy conflict → Click "Dispute" + nhập lý do
3. Chair nhận email notification về dispute
4. Chair xem disputes trên Chair Console → Quyết định: **Keep** (giữ conflict) hoặc **Delete** (xóa conflict)
5. Reviewer A có thể **Withdraw** dispute trước khi Chair quyết định

**Status flow của dispute:**
```
NOT_DISPUTED → PENDING → KEPT (Chair giữ)
                       → DELETED (Chair xóa)
             → WITHDRAWN (Reviewer rút dispute)
```

> **MVP**: KHÔNG implement dispute. Chair trực tiếp manage conflicts. Dispute là post-MVP feature.

---

## 10. REVIEW READ-ONLY

> Tham khảo: [CMT3 Enable Review](https://cmt3.research.microsoft.com/docs/help/chair/enable-review.html)

### BR-3.28: Review Read-Only mode
- Chair có thể set từng paper hoặc bulk papers thành **Read-Only**
- Read-Only = reviewer không thể chỉnh sửa review
- Use case: Lock reviews trước Discussion, lock reviews đã hoàn tất
- `Paper.isReviewReadOnly` (boolean, default false)

---

## 11. DISCUSSION CHI TIẾT

> Tham khảo: [CMT3 Discussion](https://cmt3.research.microsoft.com/docs/help/chair/enable-discussion.html)

### BR-3.30: Enable Discussion per paper
- Discussion KHÔNG tự enable tất cả papers (trừ khi setting `enableAllPapersForDiscussion = true`)
- Chair enable discussion cho từng paper (hoặc bulk enable) khi reviews có divergent recommendations
- `Paper.isDiscussionEnabled` (boolean, default false)

### BR-3.31: Discussion flow chi tiết
```
Activity REVIEW_DISCUSSION enabled
    → Chair bulk/individual enable papers cho discussion
    → Reviewer thấy Discussion tab cho papers được enable
    → Reviewer/Chair tạo discussion topic (title + description)
    → Các reviewers assigned cho paper reply
    → Chair có thể tham gia discussion
```

### BR-3.32: Discussion visibility rules

| Ai | Thấy gì | Điều kiện |
|---|---|---|
| Assigned Reviewer | Tất cả discussion threads của paper | Mặc định |
| Non-assigned Reviewer | Discussion threads nếu `allowDiscussNonAssignedPapers = true` | Phải không có conflict |
| Author | Discussion threads nếu `allowAuthorDiscuss = true` | Chỉ sau khi PC post đầu tiên |
| Chair | Tất cả | Luôn luôn |

### BR-3.33: Discussion constraints
- Reviewer **PHẢI submit review trước** khi tham gia discussion (nếu `allowOthersReviewAccessAfterSubmit = true`)
- Reviewer **không thể post message mới** nếu Chair tắt quyền post
- Reviewer **có thể update review** trong discussion nếu `allowReviewUpdateDuringDiscussion = true`
- `ReviewComment` entity mở rộng: thêm `parentCommentId` (thread replies), `isDiscussionPost` flag

---

## 12. REVIEW AGGREGATES

> Tham khảo: [CMT3 Review Aggregates](https://cmt3.research.microsoft.com/docs/help/chair/review-aggregate.html)

### BR-3.34: Aggregate cho Chair Console
- Khi `showAggregateColumns = true`: Chair Console hiển thị cột aggregate cho mỗi ReviewQuestion có giá trị (OPTIONS_WITH_VALUE)
- Aggregate tính: **Average** của tất cả completed reviews cho paper đó
- Chỉ hiển thị khi có ≥ 1 completed review

### BR-3.35: Cách tính aggregate
```
Cho mỗi paper P, mỗi ReviewQuestion Q (loại OPTIONS_WITH_VALUE):
  aggregateScore(P, Q) = AVG(review_answers[Q].numericValue) cho tất cả COMPLETED reviews của P
```

### BR-3.36: Giới hạn
- Tối đa 8 cột aggregate hiển thị trên Chair Console
- Sắp xếp theo tên question (alphabetical)
- Chair export được aggregate data

---

## 13. EMERGENCY REVIEWER

> Tham khảo: [CMT3 Emergency Reviewer](https://cmt3.research.microsoft.com/docs/help/chair/emergency-reviewer.html)

### BR-3.37: Khi nào cần
- Deadline review sắp đến, reviewer drop out hoặc không phản hồi
- Chair designate một số reviewers là **Emergency Reviewer**

### BR-3.38: Emergency Reviewer flow
1. Chair enable "Emergency Designation" trong Review Settings
2. Chair đánh dấu reviewers cụ thể là Emergency (manual hoặc bulk)
3. Emergency reviewers **chỉ** được assign paper khi có nhu cầu khẩn cấp
4. Khi assign → system gửi email thông báo cho emergency reviewer

### BR-3.39: Rules
- Emergency reviewer = reviewer bình thường nhưng có flag `isEmergency = true`
- Program Chair có thể assign papers cho emergency reviewers
- Emergency reviewer nhận **email notification** khi được assign
- Quota riêng hoặc dùng quota chung tùy config

---

## 14. EMAIL TRONG REVIEW FLOW

> Tham khảo: [CMT3 Email Reviewers](https://cmt3.research.microsoft.com/docs/help/chair/email-reviewers.html)

### BR-3.40: Các email tự động trong review flow

| Trigger | Người nhận | Nội dung |
|---|---|---|
| Manual assign paper (nếu setting bật) | Reviewer | Thông báo paper mới được assign |
| Review update during discussion | Other Reviewers (nếu setting bật) | Thông báo review đã thay đổi |
| Review update during discussion | Meta-Reviewers (nếu setting bật) | Thông báo review đã thay đổi |
| Emergency reviewer assignment | Emergency Reviewer | Thông báo assign khẩn cấp |

### BR-3.41: Bulk email reviewers
- Chair gửi email hàng loạt cho reviewers từ Manage Reviewers page
- Filter: assigned > 0, review not submitted, specific tracks, etc.
- Template + Placeholders: `{Review.Assigned}`, `{Review.NotSubmitted}`, `{Recipient.Name}`, `{Conference.Name}`
- Email gửi **1 email per reviewer** (chứa tất cả papers assigned)

### BR-3.42: Deadline reminder email
- Chair gửi reminder cho reviewers chưa submit review
- Filter: reviewers có `Review.status != COMPLETED`
- Gửi từ Submissions page hoặc Manage Reviewers page

---

## 15. PAPER STATUS & AUTHOR NOTIFICATION

> Tham khảo: [CMT3 Author Notification](https://cmt3.research.microsoft.com/docs/help/chair/author-notification.html)

### BR-3.43: Paper status values

| Status | Mô tả | Set bởi |
|---|---|---|
| DRAFT | Paper đã đăng ký nhưng chưa upload manuscript file | Tự động khi author register paper |
| SUBMITTED | Paper đã nộp chính thức (đã có manuscript) | Tự động khi author upload manuscript file |
| UNDER_REVIEW | Đang trong quá trình review | Tự động khi assign reviewer |
| ACCEPTED | Được chấp nhận | Chair/Meta-review |
| REJECTED | Bị từ chối | Chair/Meta-review |
| REVISION | Cần sửa và nộp lại → bắt đầu revision cycle | Chair/Meta-review |
| WITHDRAWN | Author tự rút bài | Author |
| PUBLISHED | Đã hoàn tất camera-ready | Chair |

### BR-3.43b: Paper status flow
```
DRAFT → (upload manuscript) → SUBMITTED → UNDER_REVIEW → ACCEPTED → PUBLISHED
                                                ↓              
                                            REJECTED           
                                                ↓              
                                            REVISION → (re-submit) → UNDER_REVIEW (再review)
                   
Bất kỳ trạng thái nào (trừ PUBLISHED) → WITHDRAWN (author rút)
DRAFT: Chỉ hiển thị cho author, KHÔNG hiển thị cho reviewer/chair trong bidding/assignment
```

### BR-3.44: Author Notification flow
1. Chair set paper status cho tất cả papers (sau khi reviews hoàn tất)
2. Chair vào Author Notification Wizard
3. Tạo email template cho **mỗi status** (Accept, Reject, Revision)
4. Chọn recipients: "Primary Contact Only" hoặc "All Authors"
5. Preview emails → Send
6. Sau khi gửi: Activity `AUTHOR_NOTIFICATION` tự động mark `COMPLETED`

### BR-3.45: Notification rules
- **Email per paper**: 1 author có 3 papers = nhận 3 emails riêng biệt
- **Chỉ registered users** nhận email
- Template sử dụng placeholders: `{Paper.Title}`, `{Paper.Id}`, `{Paper.Status}`, `{Recipient.Name}`
- Chair có thể set `allowReviewerSeeStatusBeforeNotification = true` để reviewers thấy status trước

---

## 16. REVIEWER DATA VISIBILITY

> Tham khảo: [CMT3 Data Visibility](https://cmt3.research.microsoft.com/docs/help/chair/reviews-visible-to-author.html)

### BR-3.46: Ai thấy gì

| Data | Author | Reviewer (assigned) | Reviewer (other) | Chair |
|---|---|---|---|---|
| Paper nội dung | ✅ | ✅ | ❌ | ✅ |
| Author identity | ✅ | ❌ (double blind) | ❌ | ✅ |
| Review content | ❌ (trước notification) | ✅ (own) | Tùy setting | ✅ |
| Other reviews | ❌ (trước notification) | Tùy setting | ❌ | ✅ |
| Discussion threads | Tùy setting | ✅ (assigned) | Tùy setting | ✅ |
| Paper status | ❌ (trước notification) | Tùy setting | ❌ | ✅ |
| Review aggregates | ❌ | ❌ | ❌ | ✅ |
| Reviewer identity | ❌ | Tùy setting | ❌ | ✅ |

### BR-3.47: Thời điểm mở visibility cho Author
1. **Trước Author Notification**: Author không thấy reviews, status, discussion
2. **Sau Author Notification**: Chair quyết định mở:
   - Reviews visible to Author → Author thấy nội dung reviews
   - Meta-reviews visible to Author → Author thấy meta-review
   - Status visible to Author → Author thấy paper status

---

## 17. REVISION CYCLE

> Tham khảo: [CMT3 Revision](https://cmt3.research.microsoft.com/docs/help/chair/revision.html)

### BR-3.48: Revision flow
```
1. Chair/Meta-review set Paper.status = REVISION
2. Chair gửi Author Notification cho papers có status REVISION
3. Author thấy reviews (nếu visibility bật) → biết cần sửa gì
4. Chair enable "Revision Submission" trong Activity Timeline + set deadline
5. Author upload revised paper qua revision upload link
6. Deadline hết → Chair enable review lại (chỉ revision papers)
7. Reviewers review lại revision papers
8. Chair/Meta-review set final status: ACCEPTED hoặc REJECTED
9. Chair gửi final Author Notification
```

### BR-3.49: Revision rules
- **Review visibility trước revision**: Phải tắt "Review Visible to Author" cho status REVISION trước khi Author Notification → tránh author thấy review updates trong revision
- **Review Settings trong revision**: Bật "Allow only revision papers for reviewing" → chỉ hiển thị revision papers cho reviewers
- **Activity Timeline**: Author Notification phải `COMPLETED` trước khi enable Revision Submission
- **1 revision status duy nhất**: Chỉ có 1 status REVISION gắn với revision upload functionality
- Author chỉ thấy revision upload link khi paper có status REVISION

### BR-3.50: Review trong revision
- Chair có thể tạo review/meta-review questions riêng cho revision round
- Reviewers được assign lại (có thể giữ nguyên hoặc thay đổi reviewers)
- Review process giống hệt round 1 (submit review → discussion → meta-review → final decision)

---

## 18. CAMERA-READY SUBMISSION

> Tham khảo: [CMT3 Camera-Ready](https://cmt3.research.microsoft.com/docs/help/chair/camera-ready.html)

### BR-3.51: Camera-Ready flow
1. Chair set paper status = ACCEPTED
2. Auto-enable camera-ready (nếu paper status config có "Auto-Enable Camera-Ready")
   hoặc Chair manual enable camera-ready per paper
3. Chair enable "Camera Ready Submission" trong Activity Timeline + set deadline
4. Author upload final version (camera-ready files)
5. Deadline hết → Paper.status chuyển sang PUBLISHED

### BR-3.52: Camera-Ready rules
- Chỉ papers có status ACCEPTED mới eligible cho camera-ready
- Camera-ready có file format/size restrictions riêng (config bởi Chair)
- Author có thể edit camera-ready files trước deadline
- Sau deadline → locked, không sửa được

---

## 19. REVIEW QUESTION VISIBILITY

> Tham khảo: [CMT3 Review Questions](https://cmt3.research.microsoft.com/docs/help/chair/manage-review-questions.html)

### BR-3.53: Visibility settings per question
- Mỗi ReviewQuestion có các visibility flags:

| Flag | Mô tả |
|---|---|
| `visibleToAuthorAfterNotification` | Author thấy answer sau Author Notification |
| `visibleToOtherReviewers` | Reviewers khác thấy answer (trong Discussion) |
| `isConfidential` | Chỉ Chair thấy (ẩn khỏi authors + reviewers khác) |

- **Không nên thay đổi questions sau khi reviewers đã trả lời** → completion % không update, reviewers không biết có question mới
- Copy questions giữa tracks được (CMT3 có "Copy Questions to Another Track")

---

## ⚠️ MIS/MATCH VỚI CODE HIỆN TẠI

### ✅ ĐÃ IMPLEMENT
| Business Rule | File |
|---|---|
| BR-3.1: `requireSubjectAreas` check trước khi bid | `BiddingServiceImpl.validateRequireSubjectAreas()` |
| BR-3.2: Expertise weight trong relevance score | `BiddingServiceImpl.calculateRelevanceScoreWithExpertise()` (EXPERT=1.0, KNOWLEDGEABLE=0.7, INTERESTED=0.4) |
| BR-3.3: Check REVIEWER_BIDDING activity enabled + deadline | `BiddingServiceImpl.validateBiddingPhaseOpen()` |
| BR-3.4: Lọc WITHDRAWN papers | `BiddingServiceImpl.getPapersForBidding()` |
| BR-3.4: Double Blind ẩn abstract khi bidding | `BiddingServiceImpl` + `PaperForBiddingDTO.isDoubleBlind` |
| BR-3.5: BidValue enum 4 giá trị | `BidValue.java` |
| BR-3.6: Upsert bid logic | `BiddingServiceImpl.submitOrUpdateBid()` |
| BR-3.7: Relevance score algorithm | `BiddingServiceImpl.calculateRelevanceScoreWithExpertise()` |
| BR-3.9-3.12: Auto/manual assign + preview | `ReviewerAssignmentServiceImpl.java` |
| BR-3.9: Load balancing trong auto-assign | `ReviewerAssignmentServiceImpl.runAutoAssign()` (2026-03-17) |
| BR-3.10: Domain conflict detection | `DomainConflictUtil.java` + `ReviewerAssignmentServiceImpl.runAutoAssign()` (2026-03-17) |
| BR-3.12: Manual assign populate reviewId | `ReviewerAssignmentServiceImpl.manualAssign()` (2026-03-17) |
| BR-3.13: Check review chưa COMPLETED trước khi remove | `ReviewerAssignmentServiceImpl.removeAssignment()` |
| BR-3.14: ASSIGNED→IN_PROGRESS→COMPLETED transitions | `ReviewServiceImpl.VALID_TRANSITIONS` map |
| BR-3.14: ReviewResponseDTO flat DTOs (no circular ref) | `ReviewResponseDTO.PaperInfo` + `ReviewResponseDTO.ReviewerInfo` (2026-03-17) |
| BR-3.14: Reviews scoped by conference | `ReviewController.getReviewsByReviewerAndConference()` (2026-03-17) |
| BR-3.15: Check REVIEW_SUBMISSION activity enabled | `ReviewServiceImpl.validateReviewActivity()` |
| BR-3.16: ReviewQuestion + choices | `ReviewQuestion.java`, `ReviewQuestionChoice.java` |
| BR-3.17: Validate required questions before complete | `ReviewServiceImpl.validateRequiredQuestionsCompleted()` |
| BR-3.18: Tính totalScore từ ReviewAnswer | `ReviewServiceImpl.calculateTotalScore()` |
| BR-3.19: ReviewComment entity | `ReviewComment.java` |
| BR-3.20: ReviewMetaReview entity | `ReviewMetaReview.java` |
| BR-3.21: Meta-review → update paper status | `ReviewMetaReviewServiceImpl.updatePaperStatusFromDecision()` |
| BR-3.21: REVISION → PaperStatus.REVISION fix | `ReviewMetaReviewServiceImpl` (2026-03-23) |
| BR-3.21: Meta-review role auth (PROGRAM_CHAIR/CONFERENCE_CHAIR) | `ReviewMetaReviewServiceImpl.validateChairRole()` (2026-03-23) |
| BR-3.21: Unique meta-review per paper | `ReviewMetaReviewServiceImpl.createReviewMetaReview()` (2026-03-23) |
| BR-3.21: Flat ResponseDTO (avoid circular ref) | `ReviewMetaReviewResponseDTO.PaperInfo` + `UserInfo` (2026-03-23) |
| BR-3.21: Scoped meta-review APIs | `ReviewMetaReviewController.getByConference/getByPaper` (2026-03-23) |
| BR-3.21: Meta-review FE Decision Console | `chair/decisions/page.tsx` + `paper-decision-detail.tsx` (2026-03-23) |
| BR-3.22 (partial): 7/18 settings đã có | `TrackReviewSetting.java` |
| BR-3.26 (partial): Domain conflict trong auto-assign | `DomainConflictUtil.java` |
| BR-3.51: Camera-ready upload + validation | `PaperFileServiceImpl.createCameraReadyFile()` (2026-03-23) |
| BR-3.51: Camera-ready approve → PUBLISHED | `PaperFileServiceImpl.approveCameraReady()` (2026-03-23) |
| BR-3.51: Camera-ready FE author page | `author/camera-ready/page.tsx` (2026-03-23) |

> **2026-03-17:** Thêm domain conflict detection, load balancing, flat ReviewResponseDTO, scoped review API.

### ❌ CHƯA IMPLEMENT
| Business Rule | Mô tả | Ưu tiên |
|---|---|---|
| BR-3.22: 10 settings còn thiếu | `showReviewerIdentity`, `enableAllPapersForDiscussion`, etc. | Trung bình |
| BR-3.24-25: Individual Conflict Management | Entity PaperConflict + UI khai báo conflict | **Cao** |
| BR-3.28: Review Read-Only per paper | `Paper.isReviewReadOnly` + lock logic | Trung bình |
| BR-3.30: Discussion per paper enable/disable | `Paper.isDiscussionEnabled` + bulk enable | **Cao** |
| BR-3.33: Discussion parent-child threads | `ReviewComment.parentCommentId` | Trung bình |
| BR-3.34-36: Review Aggregates | Tính avg score per question per paper | Thấp |
| BR-3.37-39: Emergency Reviewer | `Reviewer.isEmergency` + assign flow | Thấp |
| BR-3.40-42: Email tự động trong review | Notification khi assign, deadline reminder | **Cao** |
| BR-3.43: Paper status REVISION, PUBLISHED | ~~Thêm vào PaperStatus enum~~ ✅ Đã có + REVISION fix (2026-03-23) | **Xong** |
| BR-3.44-45: Author Notification Wizard | Template per status + bulk send | **Cao** |
| BR-3.46-47: Data Visibility controls | Visibility settings per data type | Trung bình |
| BR-3.48-50: Revision Cycle | Revision upload, re-review flow | Trung bình |
| BR-3.51-52: Camera-Ready Submission | ~~Camera-ready upload, PUBLISHED status~~ ✅ (2026-03-23) | **Xong** |
| BR-3.53: Review Question Visibility | Per-question visibility flags | Trung bình |

> **2026-03-17:** Thêm domain conflict detection, load balancing, flat ReviewResponseDTO, scoped review API.
> **2026-03-17:** Bổ sung BR-3.22 → BR-3.53 từ CMT3 Chair Reviewing docs.
> **2026-03-23:** Implement BR-3.21 (role auth, unique constraint, REVISION fix, flat DTO, scoped APIs, FE Decision Console), BR-3.51-52 (camera-ready upload/approve/FE).

