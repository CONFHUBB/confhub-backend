# Excel Template Test Data Guide

This document contains sample data for all Excel import templates used in the ConfHub system. You can copy this data into the templates downloaded from the backend API to quickly test the import flows.

> **How to use:**
> 1. Download the template from the API endpoint (or fill in the columns manually using the column headers below)
> 2. Fill in row 2+ with the sample data from this document
> 3. Upload the file via the corresponding import endpoint
> 4. Check the preview endpoint first to validate data before committing

---

## 1. Conference Template

**Download:** `GET /api/v1/conferences/import/template`
**Preview:** `POST /api/v1/conferences/import/preview`
**Import:** `POST /api/v1/conferences/import`

### Columns (14 fields)

| Column | Required | Description |
|--------|----------|-------------|
| `name` | Yes | Full name of the conference |
| `acronym` | Yes | Short acronym (e.g., ICAI2026) |
| `description` | No | Conference description |
| `location` | Yes | City/venue location |
| `startDate` | Yes | Start date (format: `yyyy-MM-dd`, e.g., `2026-06-01`) |
| `endDate` | Yes | End date (format: `yyyy-MM-dd`, e.g., `2026-06-03`) |
| `websiteUrl` | Yes | Conference website URL |
| `country` | No | Country |
| `province` | No | State/Province/Region |
| `area` | No | Academic area (e.g., Computer Science) |
| `contactInformation` | No | Contact email or info |
| `chairEmails` | No | Comma-separated chair emails |
| `bannerImageUrl` | No | Banner image URL |
| `societySponsor` | No | Sponsoring societies/organizations |

### Sample Data (Row 2)

```
IEEE Conference on AI 2026 | ICAI2026 | Annual international conference on Artificial Intelligence | Ho Chi Minh City | 2026-06-01 | 2026-06-03 | https://icai2026.org | Vietnam | Ho Chi Minh | Computer Science | contact@icai2026.org | chair1@icai2026.org,chair2@icai2026.org | https://icai2026.org/banner.png | IEEE, ACM
```

### Alternative Sample Data (Row 2)

```
International Conference on Software Engineering 2026 | ICSE2026 | Premier software engineering conference | Da Nang | 2026-08-15 | 2026-08-20 | https://icse2026.org | Vietnam | Da Nang | Software Engineering | info@icse2026.org | pc-chair@icse2026.org | https://icse2026.org/banner.png | IEEE CS, ACM SIGSOFT
```

---

## 2. Track Template

**Download:** `GET /api/v1/conferences/{conferenceId}/tracks/import/template`
**Preview:** `POST /api/v1/conferences/{conferenceId}/tracks/import/preview`
**Import:** `POST /api/v1/conferences/{conferenceId}/tracks/import`

> **Important:** Import tracks AFTER creating the conference (Step 1). You need the `conferenceId` from Step 1.

### Columns (2 fields)

| Column | Required | Description |
|--------|----------|-------------|
| `name` | Yes | Track name |
| `description` | Yes | Track description |

### Sample Data (Rows 2-6)

| name | description |
|------|-------------|
| Machine Learning | Machine learning algorithms, deep learning, and neural networks |
| NLP | Natural language processing, text mining, and computational linguistics |
| Computer Vision | Image recognition, object detection, and visual understanding |
| Robotics | Robot control, autonomous systems, and human-robot interaction |
| Data Science | Big data analytics, data mining, and data visualization |

### Alternative Sample Data (Rows 2-4)

| name | description |
|------|-------------|
| Software Engineering | Software development methodologies and best practices |
| Cloud Computing | Distributed systems, serverless, and cloud infrastructure |
| Cybersecurity | Network security, cryptography, and privacy-preserving techniques |

---

## 3. Subject Area Template

**Download:** `GET /api/v1/conferences/{conferenceId}/subject-areas/import/template`
**Preview:** `POST /api/v1/conferences/{conferenceId}/subject-areas/import/preview`
**Import:** `POST /api/v1/conferences/{conferenceId}/subject-areas/import`

> **Important:** Import subject areas AFTER creating tracks (Step 2). You need the `conferenceId` from Step 1. The `trackName` must match exactly with the track names created in Step 2.

### Columns (4 fields)

| Column | Required | Description |
|--------|----------|-------------|
| `trackName` | Yes | Name of the track this subject area belongs to |
| `name` | Yes | Subject area name |
| `description` | No | Subject area description |
| `parentName` | No | Parent subject area name (for hierarchical structure). Must appear in an earlier row within the same track. |

### Sample Data (Rows 2-10)

| trackName | name | description | parentName |
|-----------|------|-------------|------------|
| Machine Learning | Deep Learning | Neural network architectures including CNNs and RNNs | |
| Machine Learning | Reinforcement Learning | RL algorithms, Q-learning, and policy gradients | |
| Machine Learning | Transfer Learning | Domain adaptation and pre-trained models | Deep Learning |
| Machine Learning | Generative Models | GANs, VAEs, and diffusion models | Deep Learning |
| NLP | Text Classification | Document categorization and sentiment analysis | |
| NLP | Machine Translation | Neural machine translation and multilingual models | |
| NLP | Question Answering | Reading comprehension and open-domain QA | Text Classification |
| Computer Vision | Object Detection | YOLO, Faster R-CNN, and detection benchmarks | |
| Computer Vision | Image Segmentation | Semantic and instance segmentation | Object Detection |

### Alternative Sample Data (Rows 2-7)

| trackName | name | description | parentName |
|-----------|------|-------------|------------|
| Software Engineering | Agile Development | Scrum, Kanban, and iterative development | |
| Software Engineering | DevOps | CI/CD pipelines, containerization, and automation | |
| Software Engineering | Code Quality | Testing, linting, and code review practices | Agile Development |
| Cloud Computing | Serverless | AWS Lambda, Azure Functions, and FaaS | |
| Cloud Computing | Container Orchestration | Kubernetes, Docker Swarm, and service mesh | |
| Cloud Computing | Microservices | API design, service discovery, and distributed systems | Serverless |

### Validation Rules
- `trackName` must match an existing track in the conference
- `parentName` must be defined in an earlier row within the same track
- Duplicate subject area names within the same track are not allowed

---

## 4. Member Template

**Download:** `GET /api/v1/conferences/{conferenceId}/members/import/template`
**Preview:** `POST /api/v1/conferences/{conferenceId}/members/import/preview`
**Import:** `POST /api/v1/conferences/{conferenceId}/members/import`

> **Important:** Import members AFTER creating tracks (Step 2). You need the `conferenceId` from Step 1. For `REVIEWER` and `PROGRAM_CHAIR` roles, the `trackName` must match exactly with track names from Step 2.

### Columns (3 fields)

| Column | Required | Description |
|--------|----------|-------------|
| `email` | Yes | User email address |
| `role` | Yes | Role type. Valid values: `CONFERENCE_CHAIR`, `PROGRAM_CHAIR`, `REVIEWER` |
| `trackName` | Conditional | Required for `PROGRAM_CHAIR` and `REVIEWER`. Leave empty for `CONFERENCE_CHAIR`. |

### Role Descriptions

| Role | Track Required | Description |
|------|---------------|-------------|
| `CONFERENCE_CHAIR` | No | Overall conference organizer — no track assignment |
| `PROGRAM_CHAIR` | Yes | Manages a specific track's review process |
| `REVIEWER` | Yes | Reviews papers for a specific track |

### Sample Data (Rows 2-8)

| email | role | trackName |
|-------|------|----------|
| alice.johnson@example.com | REVIEWER | Machine Learning |
| bob.smith@example.com | REVIEWER | Machine Learning |
| carol.davis@example.com | REVIEWER | NLP |
| david.lee@example.com | REVIEWER | Computer Vision |
| emily.chen@example.com | PROGRAM_CHAIR | Machine Learning |
| frank.wong@example.com | PROGRAM_CHAIR | NLP |
| grace.taylor@example.com | CONFERENCE_CHAIR | |

### Alternative Sample Data (Rows 2-6)

| email | role | trackName |
|-------|------|----------|
| alice.ml@example.com | REVIEWER | Machine Learning |
| bob.nlp@example.com | REVIEWER | NLP |
| carol.cv@example.com | REVIEWER | Computer Vision |
| david.robotics@example.com | REVIEWER | Robotics |
| emily.sechair@example.com | PROGRAM_CHAIR | Software Engineering |
| frank.cloudchair@example.com | PROGRAM_CHAIR | Cloud Computing |

### Behavior on Import

| User Status | Behavior |
|-------------|----------|
| **Existing user** (email found in DB) | Invitation is sent via email + in-app notification. User must accept/decline. |
| **New user** (email not found) | Placeholder account is created (inactive). OTP is generated. Invitation email is sent with activation link. User must accept and activate the account. |

---

## Complete Test Flow

### Step 1: Create Conference
```
POST /api/v1/conferences/import
Body: multipart/form-data, file = conference_template.xlsx
```
**Expected response:**
```json
{
  "success": true,
  "conferenceId": 1,
  "conferenceName": "IEEE Conference on AI 2026"
}
```

### Step 2: Import Tracks
```
POST /api/v1/conferences/{conferenceId}/tracks/import
Body: multipart/form-data, file = track_template.xlsx
```
**Expected response:**
```json
{
  "success": true,
  "conferenceId": 1,
  "tracksCreated": 5
}
```

### Step 3: Import Subject Areas
```
POST /api/v1/conferences/{conferenceId}/subject-areas/import
Body: multipart/form-data, file = subject_area_template.xlsx
```
**Expected response:**
```json
{
  "success": true,
  "subjectAreasCreated": 9
}
```

### Step 4: Import Members
```
POST /api/v1/conferences/{conferenceId}/members/import
Body: multipart/form-data, file = member_template.xlsx
```
**Expected response:**
```json
{
  "success": true,
  "conferenceId": 1,
  "membersCreated": 7
}
```

---

## Validation Rules Summary

| Template | Required Fields | Key Rules |
|----------|----------------|-----------|
| **Conference** | name, acronym, location, startDate, endDate, websiteUrl | Dates must be `yyyy-MM-dd` format |
| **Tracks** | name, description | Track names must be unique within the conference |
| **Subject Areas** | trackName, name | trackName must exist; parentName must appear in an earlier row of same track |
| **Members** | email, role, (trackName if role is PROGRAM_CHAIR or REVIEWER) | Role must be one of: CONFERENCE_CHAIR, PROGRAM_CHAIR, REVIEWER |

---

## Notes

- All templates are `.xlsx` format only (`.xls` is NOT supported)
- All dates must be in `yyyy-MM-dd` format (e.g., `2026-06-01`)
- For Subject Areas with hierarchical structure, the parent must be defined in an earlier row within the same track
- Members with existing emails get in-app notifications; new emails get placeholder accounts with OTP
- You can use the preview endpoints to validate data before committing to the database
