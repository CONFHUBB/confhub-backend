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
- `ConferenceActivity` loại `REVIEWER_BIDDING` phải `isEnabled = true`
- Chỉ users có role `REVIEWER` trong conference mới bid được
- Chỉ bid trước deadline

### BR-3.4: Papers hiển thị cho bidding
- Reviewer thấy **tất cả papers** trong conference
- **LOẠI TRỪ**: papers mà reviewer có conflict of interest (PaperConflict)
- **LOẠI TRỪ**: papers có status `WITHDRAWN`
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

## ⚠️ MIS/MATCH VỚI CODE HIỆN TẠI

### ✅ ĐÃ IMPLEMENT
| Business Rule | File |
|---|---|
| BR-3.1: `requireSubjectAreas` check trước khi bid | `BiddingServiceImpl.validateRequireSubjectAreas()` |
| BR-3.2: Expertise weight trong relevance score | `BiddingServiceImpl.calculateRelevanceScoreWithExpertise()` (EXPERT=1.0, KNOWLEDGEABLE=0.7, INTERESTED=0.4) |
| BR-3.3: Check REVIEWER_BIDDING activity enabled + deadline | `BiddingServiceImpl.validateBiddingPhaseOpen()` |
| BR-3.4: Lọc WITHDRAWN/DRAFT papers | `BiddingServiceImpl.getPapersForBidding()` |
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

> **2026-03-17:** Thêm domain conflict detection, load balancing, flat ReviewResponseDTO, scoped review API.

