# BUSINESS RULES FOR FLOW 2: Author Nộp Bài

> Tham khảo: [CMT3 Author Submission](https://cmt3.research.microsoft.com/docs/help/author/author-submission-form.html)  
> So sánh với: confms-backend hiện tại

---

## 1. ĐIỀU KIỆN TRƯỚC KHI NỘP BÀI

### BR-2.1: Activity check
- Chỉ cho phép nộp khi `ConferenceActivity` loại `PAPER_SUBMISSION` có `isEnabled = true`
- Nếu `deadline` != null → chỉ cho nộp trước deadline
- Sau deadline → không tạo mới, nhưng có thể edit (nếu Chair cho phép)

### BR-2.2: User phải đăng ký
- User phải có tài khoản CMT (registered)
- Khi submit paper → tự động gán role `AUTHOR` trong conference (nếu chưa có)

---

## 2. TẠO SUBMISSION (Paper)

### BR-2.3: Thông tin bắt buộc
| Field | Required | Validation |
|---|:---:|---|
| title | ✅ | Không rỗng, max 500 ký tự |
| abstractField | ✅ | Không rỗng |
| track | ✅ | Track phải thuộc conference đang mở submission |
| primarySubjectArea | ✅* | Bắt buộc nếu Chair enable subject areas |
| secondarySubjectAreas | ❌ | Optional, nhiều lựa chọn |
| keyword1 | ✅ | Ít nhất 1 keyword |
| keyword2-4 | ❌ | Optional |
| files | ✅* | Tùy config: có thể submit abstract trước, file sau |

### BR-2.4: Paper status khi tạo
- Nếu submit đầy đủ (có file) → `PaperStatus.SUBMITTED`
- Nếu chỉ có title + abstract (chưa upload file) → `PaperStatus.DRAFT`
- submissionTime = thời điểm tạo paper

### BR-2.5: Subject Areas
- Primary Subject Area: chọn 1 từ danh sách của conference
- Secondary Subject Areas: chọn nhiều (optional)
- Dùng cho relevance score khi auto-assign reviewers

---

## 3. AUTHORS / CO-AUTHORS

### BR-2.6: Primary author
- Người tạo submission = primary author (auto-populated)
- Primary author = primary contact mặc định

### BR-2.7: Co-author rules
- Primary author có thể thêm co-authors
- Co-author cần: email, firstName, lastName, organization, country
- Co-author không cần có tài khoản (placeholder) nhưng KHUYẾN KHÍCH đăng ký
- Không tự động verify email co-author
- Thứ tự authors có ý nghĩa (author order)

### BR-2.8: Author list lock
- Chair có thể lock author list (không cho thêm/xóa co-authors)
- Config trong `ConferenceSubmissionForm` hoặc track settings

---

## 4. CONFLICT OF INTEREST (COI)

### BR-2.9: Domain conflicts
- Author kê khai domain conflicts (VD: organization domains)
- Không nhập public email domains (gmail.com, yahoo.com)

### BR-2.10: Auto-detect conflicts
- Nếu author và reviewer cùng organization → tự động flag conflict
- PaperConflict lưu: paper + user + conflictType (CO_AUTHOR / PERSONAL)

---

## 5. FILES

### BR-2.11: Upload rules
- Hỗ trợ upload nhiều files (PaperFile entity)
- File chính (main paper) = bắt buộc
- Supplementary files = optional
- Chair config: file size limit, file types allowed (PDF, DOCX, etc.)
- File không được lưu nếu chưa click Submit

### BR-2.12: File versioning
- Khi edit submission + upload file mới → thay thế file cũ (hoặc thêm version)
- Giữ timestamp upload

---

## 6. EDIT / DELETE / WITHDRAW

### BR-2.13: Edit submission
- Chỉ cho edit khi `PAPER_SUBMISSION` activity còn enabled (hoặc Chair cho phép edit sau deadline)
- Có thể sửa: title, abstract, keywords, subject areas, files, co-authors
- Không đổi được track sau khi submit (cần Chair can thiệp)
- Gửi confirmation email khi edit

### BR-2.14: Delete submission
- Chỉ cho delete khi `PAPER_SUBMISSION` activity còn enabled
- Delete = xóa vĩnh viễn
- Không gửi email thông báo
- Không xóa được nếu paper đã có reviews

### BR-2.15: Withdraw submission
- Withdraw != Delete: paper vẫn tồn tại nhưng `status = WITHDRAWN`
- Chỉ **sau deadline** mới hiện nút Withdraw (trước deadline → Delete)
- Chair phải enable "allow withdraw after deadline"
- Withdrawn papers không hiển thị cho reviewers
- Có thể Restore withdrawn paper (Chair only)

---

## 7. PAPER STATUS FLOW

```
DRAFT → SUBMITTED → UNDER_REVIEW → ACCEPTED → PUBLISHED
                         ↓              ↓
                     WITHDRAWN       REJECTED
```

### BR-2.16: Status transition rules
| Từ | Đến | Khi nào |
|---|---|---|
| DRAFT → SUBMITTED | Author upload file + click Submit |
| SUBMITTED → UNDER_REVIEW | Chair/System gán reviewers cho paper |
| UNDER_REVIEW → ACCEPTED | Meta-review decision = APPROVE |
| UNDER_REVIEW → REJECTED | Meta-review decision = REJECT |
| SUBMITTED/UNDER_REVIEW → WITHDRAWN | Author withdraw |
| ACCEPTED → PUBLISHED | Camera-ready submitted + Chair approve |

### BR-2.17: Status constraints
- Không thể edit paper khi `UNDER_REVIEW` (trừ Chair override)
- WITHDRAWN papers không hiển thị trong reviewer console
- REJECTED papers có thể submit lại (tạo submission mới)

---

## 8. ADDITIONAL QUESTIONS (Submission Form)

### BR-2.18: Custom questions
- Chair tạo custom questions cho submission form (ConferenceSubmissionForm)
- Question types: text, agreement, multiple choice
- Một số questions có thể required
- Câu trả lời lưu trong `Paper.extraAnswersJson` (JSON text)

---

## ⚠️ MIS/MATCH VỚI CODE HIỆN TẠI

### ✅ ĐÃ IMPLEMENT
| Business Rule | File |
|---|---|
| BR-2.1: Check PAPER_SUBMISSION enabled + deadline | `PaperServiceImpl.validatePaperSubmissionActivity()` |
| BR-2.2: Auto-gán role AUTHOR khi submit | `PaperServiceImpl.autoAssignAuthorRole()` |
| BR-2.3: Paper entity đầy đủ fields | `Paper.java` |
| BR-2.4: DRAFT status cho paper chưa có file | `PaperStatus.DRAFT` — hỗ trợ qua status param |
| BR-2.5: Primary + Secondary Subject Areas | `Paper.java` (ManyToOne + ManyToMany) |
| BR-2.6: PaperAuthor entity + thứ tự + primary contact | `PaperAuthor.java` (orderIndex, isPrimaryContact) |
| BR-2.9: PaperConflict + ConflictType | `PaperConflict.java`, `ConflictType.java` |
| BR-2.11: PaperFile entity | `PaperFile.java` |
| BR-2.13: Check activity enabled trước khi edit | `PaperServiceImpl.updatePaper()` |
| BR-2.14: Check "có reviews?" trước khi delete | `PaperServiceImpl.deletePaper()` |
| BR-2.15: Withdraw + restore flow | `PaperServiceImpl.withdrawPaper()`, `restorePaper()` |
| BR-2.16: Status transition validation | `PaperServiceImpl.VALID_TRANSITIONS` map |
| BR-2.17: Không cho edit khi UNDER_REVIEW | `PaperServiceImpl.updatePaper()` |
| BR-2.18: Extra answers JSON | `Paper.extraAnswersJson` |
| FIX: Keywords đổi JSON | `Paper.keywordsJson` TEXT, DTO dùng `List<String>` |
| FIX: PaperAuthor thêm orderIndex + isPrimaryContact | `PaperAuthor.java` |

> **2026-03-15:** Đã xóa `Paper.isPassedPlagiarism` — feature chưa triển khai, loại khỏi MVP.

### ⚠️ CẦN LÀM THÊM (Enhancement)
| Business Rule | Ghi chú |
|---|---|
| BR-2.10: Auto-detect conflicts (cùng organization) | Cần thêm logic so sánh user.organization khi tạo paper |
| BR-2.7: Author ordering API (reorder authors) | Entity có orderIndex, cần API endpoint để reorder |
| BR-2.8: Author list lock (Chair config) | Cần setting trong ConferenceSubmissionForm |


