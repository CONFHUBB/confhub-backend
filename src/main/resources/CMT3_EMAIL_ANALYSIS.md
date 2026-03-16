# PHÂN TÍCH HỆ THỐNG EMAIL CMT3 & SO SÁNH VỚI CONFMS

> Nguồn: [CMT3 Documentation](https://cmt3.research.microsoft.com/docs/help/index.html)

---

## 1. TỔNG QUAN

CMT3 sử dụng email làm **kênh giao tiếp chính** xuyên suốt vòng đời conference. Có **4 loại email chính**:

| # | Loại | Mục đích | Ai gửi |
|---|---|---|---|
| 1 | Invitation Emails | Mời người tham gia conference | Chair (qua system) |
| 2 | Bulk Emails | Thông báo hàng loạt cho nhóm | Chair |
| 3 | Individual Emails | Gửi riêng cho 1 người/paper | Chair |
| 4 | System Emails | Tự động phát sinh bởi hệ thống | System (tự động) |

---

## 2. INVITATION EMAILS (Email mời tham gia)

### 2.1. Mục đích
Mời người dùng tham gia conference với role cụ thể (Reviewer, Meta-Reviewer, Senior Meta-Reviewer).

### 2.2. Cách thực thi

**Bước 1 — Chair tạo invitation:**
- Chair vào Manage Reviewers → Actions → Invite (individual) hoặc Invite (Bulk)
- Nhập email người được mời → Search
- Nếu user **có** CMT account: hiển thị thông tin user
- Nếu user **chưa có** CMT account: Chair nhập tên + organization → Create Invite

**Bước 2 — Hệ thống sinh email:**
- Tạo **unique token** cho mỗi invitation (không thể sửa/sao chép)
- Sinh **Accept link** và **Decline link** chứa token unique
- Compose email mời (có thể chỉnh nội dung nhưng **KHÔNG được sửa accept/decline links**)
- Ghi record vào DB: status = `NOT_RESPONDED`, invitation_sent_date, expiration_days

**Bước 3 — Gửi email:**
- Email gửi từ `Microsoft CMT` (sender cố định, không đổi được)
- Nội dung: lời mời + thông tin conference + 2 nút Accept / Decline
- Email template invitation **KHÔNG thể save as template** (khác với bulk email)

**Bước 4 — Người nhận phản hồi:**

| Hành động | Xử lý |
|---|---|
| **Click Accept** | → Cập nhật status = `ACCEPTED` → Nếu setting "Auto-add reviewer on accept" bật → tự thêm role REVIEWER → Nếu chưa có CMT account → redirect tạo account |
| **Click Decline** | → Cập nhật status = `DECLINED` |
| **Không phản hồi** | → Sau `invitation_expiration_days` (mặc định 7 ngày) → Invitation hết hạn → Chair có thể **Resend** (hủy link cũ, tạo link mới) hoặc gửi **Reminder** (giữ link cũ, chỉ nhắc nhở) |

**Bước 5 — Chair quản lý invitation:**
- Trang **Manage Reviewer Invites**: xem tất cả invitation + filter theo status
- Export danh sách invitations ra Excel
- Resend invitation (bulk hoặc individual) — **lưu ý resend sẽ VÔ HIỆU link cũ**
- Delete invitation (bulk hoặc individual)

### 2.3. Business Rules

| Rule | Chi tiết |
|---|---|
| Invite ≠ Add ≠ Assign | **Invite** = mời (cần accept), **Add/Import** = thêm trực tiếp, **Assign** = gán paper cho reviewer |
| Không invite người đã là Reviewer | Nếu đã là Reviewer → assign paper, không invite lại |
| Multi-track | Mỗi track invite riêng, KHÔNG invite nhiều track cùng lúc |
| Invite by Submission | Invite reviewer cho 1 paper cụ thể → khi accept sẽ tự AUTO-ASSIGN paper đó |
| Non-CMT user | Vẫn nhận được email, nhưng phải tạo account khi accept mới vào conference |

### 2.4. Các hình thức invite

| Hình thức | Mô tả |
|---|---|
| Individual | Nhập email 1 người → search → compose → send |
| Bulk | Upload file tab-delimited (email, name, org) → verify → compose → send |
| By Submission | Từ paper cụ thể → invite reviewer → auto-assign khi accept |

---

## 3. BULK EMAILS (Email hàng loạt)

### 3.1. Mục đích
Chair gửi email cho **nhóm người** (tất cả Reviewers, tất cả Authors, Meta-Reviewers) với nội dung tùy chỉnh.

### 3.2. Phân loại Bulk Email

| Đối tượng | Trang gửi | Dùng khi |
|---|---|---|
| **Email Reviewers** | Manage Reviewers → Actions → Email | Thông báo assignments, nhắc deadline |
| **Email Authors** | Chair Console → Actions → Email → Authors | Thông báo status, nhắc camera-ready |
| **Email Meta-Reviewers** | Manage Meta-Reviewers → Actions → Email | Thông báo meta-review assignments |
| **Author Notification** | Chair Console → Actions → Author Notification Wizard | Gửi kết quả Accept/Reject/Revision |

### 3.3. Cách thực thi

**Bước 1 — Filter người nhận:**
- Chair lọc danh sách theo điều kiện (VD: Reviewers có assigned > 0, Authors có status = "Reject")
- Chọn nhóm target: "All Authors With Registered Account", "Primary Contact Only", v.v.

**Bước 2 — Tạo/Chọn email template:**
- Chair chọn template có sẵn hoặc tạo mới
- Click "Show All Supported Placeholders" để xem biến có sẵn
- Compose email body sử dụng **placeholders**

**Danh sách Placeholders (ví dụ cho Email Reviewers):**

| Nhóm | Placeholder | Ý nghĩa |
|---|---|---|
| Conference | `{Conference.Name}` | Tên conference |
| Conference | `{Conference.StartDate}` | Ngày bắt đầu |
| Conference | `{Conference.EndDate}` | Ngày kết thúc |
| Conference | `{Conference.City}` | Thành phố |
| Conference | `{Conference.Country}` | Quốc gia |
| Review | `{Review.Assigned}` | IDs submissions được assign |
| Review | `{Review.AssignedDetail}` | ID + Title list |
| Review | `{Review.NotSubmitted}` | IDs chưa submit review |
| Sender | `{Sender.Name}` | Tên người gửi |
| Sender | `{Sender.Email}` | Email người gửi |
| Recipient | `{Recipient.Name}` | Tên người nhận |
| Recipient | `{Recipient.Email}` | Email người nhận |
| Recipient | `{Recipient.Organization}` | Tổ chức người nhận |

**Bước 3 — Save template (nếu muốn):**
- Click "Save as new template…" → đặt tên → lưu
- Template dùng lại được cho cùng trang (KHÔNG dùng chéo giữa các trang khác nhau)

**Bước 4 — Preview & Send:**
- Click "Preview and Send emails" → xem từng email sẽ gửi
- Scroll qua từng recipient để kiểm tra
- Click "Send emails" → progress bar → hoàn tất

### 3.4. Author Notification Wizard (loại đặc biệt của Bulk Email)

Đây là wizard riêng để gửi kết quả review cho authors:

**Cách thực thi:**
1. Chair vào Actions → Author Notification Wizard
2. Chọn track(s) → Next
3. Tạo template cho **MỖI status** (Accept, Reject, Revision, Withdrawn, Awaiting Decision)
4. Chọn recipients: "Primary Contact Only" hoặc "All With Registered Account"
5. Chọn status + gán template tương ứng → Next
6. Preview từng email → Send
7. **Sau khi gửi**: Activity `AUTHOR_NOTIFICATION` tự động đánh dấu `Complete`, deadline set = ngày gửi

### 3.5. Business Rules

| Rule | Chi tiết |
|---|---|
| **Email per Paper, not per Author** | 1 author có 3 papers = nhận 3 emails |
| **Template KHÔNG dùng chéo** | Template của trang Email Reviewers KHÔNG dùng được ở trang Email Authors |
| **Placeholder khác nhau theo trang** | Mỗi trang gửi email có bộ placeholder riêng |
| **Track Chair vs Chair** | Track Chair tạo template riêng, Chair nhìn được template của Track Chair, nhưng ngược lại thì không |
| **Chỉ gửi cho registered users** | User chưa có CMT account sẽ KHÔNG nhận email |
| **CC = nhận đủ** | Nếu CC ai đó trong bulk email → CC nhận email cho MỌI recipient |

---

## 4. INDIVIDUAL EMAILS (Email riêng lẻ)

### 4.1. Mục đích
Gửi email cho **1 người dùng cụ thể** hoặc **tất cả stakeholders của 1 paper cụ thể**.

### 4.2. Phân loại

| Loại | Mô tả |
|---|---|
| **Email by Paper ID** | Gửi cho tất cả liên quan đến 1 paper (authors, assigned reviewers, meta-reviewers) |
| **Email Individual User** | Gửi trực tiếp 1 email cho 1 user cụ thể |

### 4.3. Cách thực thi

#### Email by Paper ID:
1. Chair Console → chọn paper → Actions → Email
2. Chọn target: Authors, Reviewers, Meta-Reviewers của paper đó
3. Compose email (có thể dùng template) → Preview → Send

#### Email Individual User:
1. Manage Users → tìm user → Actions → Email
2. Compose email → Send

#### Contact Chairs (ngược lại — user gửi cho Chair):
- Bất kỳ role nào (Author, Reviewer, Meta-Reviewer) đều có thể email Chair
- Click **CONTACT CHAIRS** trong blue header → compose → send
- Đây là cách liên lạc chính thức cho vấn đề conference-specific

### 4.4. Business Rules

| Rule | Chi tiết |
|---|---|
| Template có thể dùng lại | Nếu compose ở cùng trang |
| Placeholders tương tự bulk | Nhưng scope nhỏ hơn (1 paper hoặc 1 user) |

---

## 5. SYSTEM EMAILS (Email hệ thống tự động)

### 5.1. Mục đích
Email **tự động phát sinh** khi xảy ra sự kiện quan trọng, KHÔNG cần Chair thao tác.

### 5.2. Các loại System Email

| Loại | Trigger | Người nhận | Nội dung |
|---|---|---|---|
| **Submission Confirmation** | Author tạo submission mới | **TẤT CẢ authors** (cả chưa có CMT account) | Xác nhận paper đã submit thành công |
| **Account Verification** | Tạo tài khoản CMT mới | User đăng ký | Link xác thực email |
| **Password Reset** | Yêu cầu đặt lại mật khẩu | User yêu cầu | Link reset password |
| **Invitation Accept Redirect** | Non-CMT user accept invite | User được mời | Redirect tạo account |

### 5.3. Cách thực thi

#### Submission Confirmation:
1. Author click Submit trên Submission Form
2. System **tự động** gửi confirmation email cho **tất cả authors** trong submission
3. Author cũng có thể **thủ công** gửi lại confirmation từ Summary page → dropdown → "Send confirmation email"

#### Account Verification:
1. User Register trên CMT3
2. System gửi email verification đến email đăng ký
3. User click link trong email → account verified
4. **Lưu ý**: email CMT gửi từ domain `@msr-cmt.org` — một số firewall tổ chức có thể block → cần whitelist

#### Password Reset:
1. User click "Forgot Password" trên login page
2. System gửi email với reset link
3. User click link → đặt password mới

### 5.4. Business Rules

| Rule | Chi tiết |
|---|---|
| **Sender cố định** | System emails luôn gửi từ `@msr-cmt.org` |
| **Không bounced tracking** | CMT chỉ biết email đã queue, KHÔNG biết đã delivered |
| **Whitelist domain** | Khuyến nghị tổ chức whitelist `@msr-cmt.org` |
| **Submission confirmation cho tất cả** | Kể cả co-author chưa có CMT account vẫn nhận được |

---

## 6. EMAIL HISTORY (Quản lý lịch sử email)

Tất cả email (mọi loại) đều được log lại:

| Field | Mô tả |
|---|---|
| ID | Mã email |
| From | Người gửi |
| To | Người nhận |
| CC | CC (nếu có) |
| Subject | Tiêu đề |
| Body | Nội dung |
| Sent On | Thời gian gửi |
| Email Status | Sent / Error |

- **Export to Excel** được
- Filter theo date, status, recipient
- Setting "Show one message per bulk" chỉ ảnh hưởng view, không ảnh hưởng gửi

---

## 7. SO SÁNH VỚI CONFMS HIỆN TẠI

| Tính năng | CMT3 | confms-backend | Trạng thái |
|---|---|---|---|
| Gửi email text đơn giản | ✅ | ✅ `EmailService.sendSimpleMessage()` | ✅ |
| Invitation email HTML | ✅ | ✅ `EmailService.sendInvitationEmail()` | ⚠️ Cơ bản |
| Unique token per invitation | ✅ | ❌ Dùng dummy-token cứng | ❌ |
| Accept/Decline xử lý DB | ✅ | ❌ TODO - trả HTML cứng | ❌ |
| Invitation expiration | ✅ 7 ngày | ❌ | ❌ |
| Invitation status tracking | ✅ | ❌ Không có entity | ❌ |
| Auto-add reviewer on accept | ✅ | ❌ | ❌ |
| Email templates + placeholders | ✅ | ❌ 1 template Thymeleaf cứng | ❌ |
| Bulk email | ✅ | ❌ | ❌ |
| Author Notification Wizard | ✅ | ❌ | ❌ |
| Individual email | ✅ | ⚠️ Chỉ send đơn giản | ⚠️ |
| System emails (confirmation) | ✅ Tự động | ❌ | ❌ |
| Email History | ✅ | ❌ | ❌ |
| File attachment | Không rõ | ✅ MultipartFile | ✅ |
| Async sending | Không rõ | ✅ @Async | ✅ |
| User Email Management | ✅ | ✅ UserEmailController | ✅ |

---

## 8. KHUYẾN NGHỊ IMPLEMENT TIẾP

### Ưu tiên cao:
1. **Invitation Entity** — table `conference_invitation` (token, status, expiration, conference_id, user_email, role)
2. **Token System** — generate UUID token per invitation, validate on accept/decline
3. **Accept/Decline Logic** — update DB + auto-add role + redirect to frontend

### Ưu tiên trung bình:
4. **Email Template Management** — CRUD templates với placeholder system
5. **Bulk Email** — gửi cho nhóm filtered
6. **Email History** — log table cho mọi email đã gửi

### Ưu tiên thấp:
7. **Author Notification Wizard** — gửi kết quả review
8. **System auto-emails** — submission confirmation, deadline reminders
