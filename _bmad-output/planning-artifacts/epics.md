---
stepsCompleted:
  - step-01-validate-prerequisites
  - step-02-design-epics
  - step-03-create-stories
  - step-04-final-validation
inputDocuments:
  - requirements.md
  - ARCHITECTURE.md
---

# Enterprise Leave Management System (LMS) - Epic & Story Breakdown

## Overview

This document provides the complete epic and story breakdown for the Enterprise Leave Management System (LMS), decomposing the requirements from the Product Requirements Document ([requirements.md](file:///d:/Project/leave-management/requirements.md)) and the Technical Architecture Blueprint ([ARCHITECTURE.md](file:///d:/Project/leave-management/ARCHITECTURE.md)) into implementable, vertically sliced user stories ready for execution by Amelia (`@bmad-agent-dev`).

---

## Requirements Inventory

### Functional Requirements

* **FR-1: User Authentication & JWT Session Management:** Email/password authentication via Spring Security; stateless JWT access token (15–30 min) and refresh token (HttpOnly cookie, 7 days); temporary account lockout after 5 consecutive failed attempts.
* **FR-2: Role-Based Authorization & Hierarchies:** Role enforcement across `ROLE_EMPLOYEE`, `ROLE_MANAGER`, `ROLE_HR_ADMIN`.
* **FR-3: User Profile & Organizational Hierarchy Mapping:** User model containing department and `manager_id` reference to establish approval reporting lines.
* **FR-4: Double-Entry Balance Ledger:** Append-only immutable `leave_ledger_entries` audit records for every credit and debit; direct balance updates prohibited.
* **FR-5: Concurrency Safety & Overdraft Prevention:** Row-level pessimistic locking (`SELECT ... FOR UPDATE`) on `leave_balances` during submission and approval to guarantee zero balance overdrafts and zero double-spending.
* **FR-6: Automated Monthly Accruals & Pro-Rata Engine:** Scheduled monthly batch job (1st of month); standard FTEs accrue +1.66 days/month; 3-month probation employees accrue +1.0 day/month pro-rata and can utilize accrued balance during probation.
* **FR-7: Public Holiday & Vietnamese Working Hours:** Vietnamese standard business hours (Monday–Friday, 08:00–12:00, 12:00–13:00 lunch/nap rest, 13:00–17:00); statutory public holidays excluded from leave deduction; half-day counted as exact 0.5-day quantum.
* **FR-8: Dynamic Leave Request Submission Form:** Submission with leave type, date range, morning/afternoon half-day quantum, reason, attachment, and client/server overlap validation.
* **FR-9: 1-Tier Approval Routing with 48h SLA & Auto-Escalation:** Direct manager review within 48 business hours; automated 24-hour reminder alert; auto-transition to `ESCALATED` status re-routing to Skip-Level Manager (Department Head) and HR Admin queue upon SLA breach.
* **FR-10: Confidential Medical Attachment Handling:** Sick leave medical certificates isolated at storage layer; direct managers see certificate indicator but cannot download; download restricted to `ROLE_HR_ADMIN` and file owner.
* **FR-11: Pre-Leave and Post-Leave Cancellation:** Unilateral cancellation before leave start date (instant balance credit); cancellation after start date requires manager confirmation (`CANCEL_REQUESTED`).
* **FR-12: Backdated Leave Request Handling:** Mandatory justification and dual approval (Manager + HR Admin) for leaves with start dates in the past.
* **FR-13: Individual Employee Dashboard:** Metric cards for balance (accrued, used, pending, remaining), filterable request history table (including `ESCALATED`), and upcoming company holidays.
* **FR-14: Interactive Team Presence Calendar:** Department monthly/weekly presence view with masked leave reasons for peers to preserve medical confidentiality.
* **FR-15: Manager Approvals Inbox & Conflict Visualizer:** Dedicated inbox with SLA countdown chips (Normal, Warning <=24h, Escalated) and team overlap warning indicator.
* **FR-16: Immutable System Audit Log:** Audit records for every lifecycle transition (`SUBMIT`, `APPROVE`, `REJECT`, `ESCALATE`, `CANCEL`, `ADJUSTMENT`) with actor, timestamp, and JSON delta.
* **FR-17: Monthly Payroll Deduction & Accrual Export:** HR Admin export of leave deductions and net balance adjustments to CSV and Microsoft Excel (`.xlsx`).

---

### NonFunctional Requirements

* **NFR-1 (P95 Latency):** Read operations (Dashboard load, Calendar view) respond in $< 300\text{ms}$ at P95 under standard enterprise load (up to 5,000 active employees).
* **NFR-2 (Write Transactions):** Leave submission and approval transactions commit in $< 500\text{ms}$ including ledger updates and pessimistic lock release.
* **NFR-3 (Password Storage):** Passwords hashed using BCrypt with a minimum work factor of 12.
* **NFR-4 (Stateless Tokens):** JWT signatures validated using HMAC-SHA256 with secrets configured via environment variables.
* **NFR-5 (Medical Privacy):** Attachment endpoints enforce strict role checks preventing unauthorized manager downloads of sick leave certificates.
* **NFR-6 (Transport Security):** Enforce HTTPS/TLS 1.3 across all endpoints.
* **NFR-7 (Zero Double-Spend):** Database transaction isolation set to `READ_COMMITTED` with explicit pessimistic locking (`SELECT ... FOR UPDATE`) on balance ledger rows during modification.
* **NFR-8 (Idempotency):** Request submission API accepts a client-generated `Idempotency-Key` header to prevent duplicate submissions from network re-tries or UI double-clicks.
* **NFR-9 (Component Library):** Frontend standardizes on Ant Design (v5+) adhering to WCAG 2.1 Level AA color contrast standards.
* **NFR-10 (Responsive Layout):** Clean layout support across desktop monitors ($\ge 1280\text{px}$), tablets, and mobile browser viewports ($\ge 375\text{px}$).

---

### Additional Requirements (From Architecture)

* **ARCH-1 (Technology Stack Baseline):** Java 25 LTS, Spring Boot 3.5.x, MySQL 8.0 (InnoDB), Flyway 10.x migrations, Vite + React 18 + TypeScript + Ant Design 5 + TanStack Query v5.
* **ARCH-2 (Database Schema DDL):** Core tables: `departments`, `users`, `leave_types`, `leave_balances`, `leave_attachments`, `leave_requests`, `leave_ledger_entries`, `public_holidays`, `system_audit_logs`.
* **ARCH-3 (Balance Concurrency Anchor):** Row-level lock on `leave_balances` per `(user_id, leave_type_id)` avoids global table locking while guaranteeing serializable balance checks.
* **ARCH-4 (Background Schedulers):** Spring `@EnableScheduling` with `ThreadPoolTaskScheduler` for `MonthlyAccrualScheduler` (`0 5 0 1 * ?`) and `SlaEscalationScheduler` (`0 */15 * * * ?`).
* **ARCH-5 (Business Hours Engine):** Working calendar logic computing elapsed business hours according to Vietnam standard (Mon–Fri 08:00–12:00, 13:00–17:00, excluding public holidays).
* **ARCH-6 (Secure Physical Storage):** Attachment files stored outside web root with cryptographic UUID file naming and MIME-type verification.
* **ARCH-7 (Frontend Server Cache):** TanStack Query keys with automated invalidation on leave mutation triggers.

---

### UX Design Requirements

* **UX-DR1 (Balance Metric Cards):** High-visibility dashboard metric cards displaying Available, Used, Pending, and Accrued days with clear color accents.
* **UX-DR2 (Dynamic Request Modal & Conflict Visualizer):** Form with date range picker, morning/afternoon shift toggles, live non-working day calculations, and peer collision warnings.
* **UX-DR3 (Manager Inbox with SLA Badges):** Approvals inbox featuring visual SLA countdown badges (Green $>24\text{h}$, Yellow $\le 24\text{h}$, Red Pulsing `ESCALATED`).
* **UX-DR4 (Interactive Presence Calendar):** Department calendar with privacy-masked absence tags for peers.
* **UX-DR5 (Mobile Responsive Layout):** Mobile-first viewport adaptations for personal balance checks, emergency sick leave submission, and 1-click approvals.

---

### FR Coverage Map

| Requirement ID | Description | Covered in Epic & Story |
| :--- | :--- | :--- |
| **FR-1** | User Authentication & JWT Session Management | **Epic 1:** Story 1.1, Story 1.2 |
| **FR-2** | Role-Based Authorization & Hierarchies | **Epic 1:** Story 1.3 |
| **FR-3** | User Profile & Organizational Hierarchy Mapping | **Epic 1:** Story 1.1, Story 1.3 |
| **FR-4** | Double-Entry Balance Ledger | **Epic 2:** Story 2.1 |
| **FR-5** | Concurrency Safety & Overdraft Prevention | **Epic 2:** Story 2.1 |
| **FR-6** | Automated Monthly Accruals & Pro-Rata Engine | **Epic 2:** Story 2.3 |
| **FR-7** | Public Holiday & Vietnamese Working Hours Calculation | **Epic 2:** Story 2.2 |
| **FR-8** | Dynamic Leave Request Submission & Overlap Check | **Epic 3:** Story 3.1 |
| **FR-9** | 1-Tier Approval Routing with 48h SLA & Auto-Escalation | **Epic 3:** Story 3.2, Story 3.3 |
| **FR-10** | Confidential Medical Attachment Handling | **Epic 4:** Story 4.1 |
| **FR-11** | Pre-Leave and Post-Leave Cancellation | **Epic 3:** Story 3.4 |
| **FR-12** | Backdated Leave Request Handling | **Epic 3:** Story 3.4 |
| **FR-13** | Individual Employee Dashboard | **Epic 5:** Story 5.2 |
| **FR-14** | Interactive Team Presence Calendar | **Epic 5:** Story 5.5 |
| **FR-15** | Manager Approvals Inbox & Conflict Visualizer | **Epic 5:** Story 5.4 |
| **FR-16** | Immutable System Audit Log | **Epic 4:** Story 4.2 |
| **FR-17** | Monthly Payroll Deduction & Accrual Export | **Epic 4:** Story 4.3 |

---

## Epic List

* **Epic 1: Foundation, User Authentication & Organizational Hierarchy**  
  *Goal:* Establish the Spring Boot 3 core infrastructure, MySQL 8.0 schema via Flyway, and stateless JWT authentication, enabling users to log in with role-based permissions (`ROLE_EMPLOYEE`, `ROLE_MANAGER`, `ROLE_HR_ADMIN`) and view organizational reporting lines.
* **Epic 2: Core Leave Engine & Double-Entry Balance Ledger**  
  *Goal:* Deliver the transactional backbone of leave management: atomic double-entry ledger bookkeeping (`leave_ledger_entries`), row-level pessimistic locking (`leave_balances`), statutory Vietnamese business hours & holiday calendar calculations, and automated monthly pro-rata accruals.
* **Epic 3: Leave Request Workflow & SLA Auto-Escalation Engine**  
  *Goal:* Guide leave requests through submission, validation, manager approval, 48-hour SLA auto-escalation, cancellation, and immutable state audit logging.
* **Epic 4: Medical Privacy & Compliance Reporting**  
  *Goal:* Ensure statutory medical document confidentiality by isolating file storage and restricting download streams to `ROLE_HR_ADMIN`, while empowering HR with immutable audit logs and monthly payroll reconciliation exports.
* **Epic 5: Employee Self-Service & Manager Portal (React 18 + Ant Design)**  
  *Goal:* Provide an intuitive, responsive web application for employees and managers featuring real-time balance cards, dynamic request submission with conflict checks, approvals inbox with SLA countdown chips, and privacy-masked team presence calendars.

---

## Epic 1: Foundation, User Authentication & Organizational Hierarchy

**Epic Goal:** Establish the Spring Boot 3 core infrastructure, MySQL 8.0 schema via Flyway, and stateless JWT authentication, enabling users to log in with role-based permissions (`ROLE_EMPLOYEE`, `ROLE_MANAGER`, `ROLE_HR_ADMIN`) and view organizational reporting lines.

### Story 1.1: Project Scaffold & Database Migration Baseline

As an IT Engineer / Developer,  
I want a configured Spring Boot 3.5.x backend scaffold with MySQL 8.0 connection pool (HikariCP) and Flyway migration baseline,
So that all subsequent database tables and seed configurations can be tracked and deployed reproducibly.

**Acceptance Criteria:**

* **Given** a clean MySQL 8.0 database, **when** the Spring Boot application boots, **then** Flyway migration `V1__init_schema.sql` creates `departments` and `users` tables with strict foreign keys and indexes.
* **Given** application startup, **when** calling `GET /actuator/health`, **then** the system returns HTTP 200 `{"status": "UP"}`.
* **Given** the initial database state, **then** baseline seed data establishes default departments (`Engineering`, `HR`, `Operations`) and admin accounts.

**Technical Tasks:**
1. Initialize Spring Boot 3.5.x Maven project with Java 25, Spring Data JPA, Spring Web, Validation, and MySQL Driver.
2. Configure `application.yml` with HikariCP connection pool settings and Flyway migration path.
3. Write `V1__init_schema.sql` with table definitions for `departments` and `users`.
4. Implement BaseEntity with `created_at` and `updated_at` timestamps.

---

### Story 1.2: User Authentication & JWT Session Management

As an Employee or Manager,  
I want to authenticate using my corporate email and password,  
So that I receive a stateless JWT access token and secure refresh token for session management.

**Acceptance Criteria:**

* **Given** valid credentials, **when** POSTing to `/api/v1/auth/login`, **then** the system returns HTTP 200 with a signed JWT access token (15–30 min validity) and sets an HttpOnly Secure `refreshToken` cookie (7 days).
* **Given** invalid credentials, **when** POSTing to `/api/v1/auth/login`, **then** the system returns HTTP 401 Unauthorized with error code `INVALID_CREDENTIALS`.
* **Given** 5 consecutive failed login attempts, **when** attempting a 6th login, **then** the account is temporarily locked for 15 minutes and returns HTTP 423 Locked.
* **Given** a valid refresh token cookie, **when** POSTing to `/api/v1/auth/refresh`, **then** the system issues a new JWT access token without requiring re-entry of password.

**Technical Tasks:**
1. Configure Spring Security 6 `SecurityFilterChain` disabling CSRF (stateless) and enabling CORS.
2. Implement `JwtTokenProvider` using HMAC-SHA256 with environment secret key.
3. Implement `AuthService` and `AuthController` with login and refresh endpoints.
4. Implement failed login attempt counter with BCrypt password verification (work factor 12).

---

### Story 1.3: Role-Based Authorization & Profile Context

As an Authenticated User,  
I want my identity, role (`ROLE_EMPLOYEE`, `ROLE_MANAGER`, `ROLE_HR_ADMIN`), and manager reporting line verified on every API call,  
So that I only access resources and actions permitted for my organizational role.

**Acceptance Criteria:**

* **Given** an unauthenticated request to any protected `/api/v1/**` endpoint, **then** the system returns HTTP 401 Unauthorized.
* **Given** an authenticated user with role `ROLE_EMPLOYEE`, **when** attempting to access manager-only or HR-only endpoints, **then** the system returns HTTP 403 Forbidden.
* **Given** a valid JWT bearer token, **when** calling `GET /api/v1/users/me`, **then** the system returns the authenticated user's profile, role, department name, and direct manager details.

**Technical Tasks:**
1. Implement `JwtAuthenticationFilter` reading `Authorization: Bearer <token>` header.
2. Configure `@EnableMethodSecurity(prePostEnabled = true)` for role-scoped annotations.
3. Implement `UserController` and `UserService` exposing `GET /api/v1/users/me`.
4. Write integration test verifying 401 and 403 behavior across roles.

---

## Epic 2: Core Leave Engine & Double-Entry Balance Ledger

**Epic Goal:** Deliver the transactional backbone of leave management: atomic double-entry ledger bookkeeping (`leave_ledger_entries`), row-level pessimistic locking (`leave_balances`), statutory Vietnamese business hours & holiday calendar calculations, and automated monthly pro-rata accruals.

### Story 2.1: Double-Entry Balance Ledger & Pessimistic Locking

As a System Auditor / HR Operations Specialist,  
I want all employee leave balance mutations recorded in an append-only ledger with row-level pessimistic locks,  
So that balance double-spending, overdrafts, and silent mutations are physically prevented.

**Acceptance Criteria:**

* **Given** a leave balance transaction, **when** balance is modified, **then** a corresponding row is inserted into `leave_ledger_entries` with `transaction_uuid`, `user_id`, `leave_type_id`, `amount`, `entry_type`, `balance_after`, and `actor_user_id`. Direct updates to balances without ledger entries are prohibited.
* **Given** an employee with 1.0 day available balance, **when** two concurrent requests attempt to reserve 1.0 day simultaneously, **then** the row lock (`SELECT ... FOR UPDATE` on `leave_balances`) ensures exactly one request succeeds and the other fails with HTTP 422 `INSUFFICIENT_BALANCE`.
* **Given** `GET /api/v1/leaves/balances`, **then** the system calculates real-time available balance as: $\text{accrued\_days} + \text{carried\_over\_days} - \text{used\_days} - \text{pending\_days}$.

**Technical Tasks:**
1. Create Flyway migration `V2__leave_types_and_ledger.sql` for `leave_types`, `leave_balances`, and `leave_ledger_entries`.
2. Implement `LeaveBalance` entity and `LeaveLedgerEntry` entity with JPA relationships.
3. Implement `LeaveBalanceRepository` with `@Lock(LockModeType.PESSIMISTIC_WRITE)` query method.
4. Implement `LeaveLedgerService` with transactional debit, credit, and reservation methods.
5. Write concurrent multithreaded test validating zero overdraft under race conditions.

---

### Story 2.2: Vietnamese Workday, Holiday & Half-Day Quantum Calculation

As an Employee planning time off,  
I want the system to calculate the exact billable leave days by excluding weekends and Vietnamese public holidays and applying 0.5-day morning/afternoon quanta,  
So that my balance is deducted accurately without paying for non-working days.

**Acceptance Criteria:**

* **Given** Vietnamese business hours (`08:00–12:00` morning, `12:00–13:00` nap/lunch, `13:00–17:00` afternoon), **when** requesting a morning or afternoon half-day, **then** the system deducts exactly 0.5 days.
* **Given** a leave request spanning Friday to Monday where Monday is a statutory holiday in `public_holidays`, **then** total billable days calculated is exactly 1.0 day (Friday only).
* **Given** a date calculation request `POST /api/v1/leaves/calculate-days`, **then** the system returns total billable days, excluded weekend days, and excluded holiday names.

**Technical Tasks:**
1. Create Flyway migration `V3__public_holidays.sql` with statutory Vietnamese holidays (Tet, National Day, Reunification Day, etc.).
2. Implement `PublicHoliday` entity and repository.
3. Implement `LeaveCalculationService` with working-day iteration excluding weekends and public holidays.
4. Expose `POST /api/v1/leaves/calculate-days` endpoint.

---

### Story 2.3: Automated Monthly Pro-Rata Accrual Scheduler

As an HR Administrator,  
I want a scheduled monthly batch engine that credits fractional leave days to all eligible employees on the 1st of each month,  
So that annual leave accruals are computed automatically without manual spreadsheet entry.

**Acceptance Criteria:**

* **Given** the 1st day of a calendar month at 00:05 AM, **when** the scheduled job triggers, **then** it iterates all active employees (`is_active = true`).
* **Given** an employee with `employment_status = 'PROBATION'`, **then** the system credits +1.0 day pro-rata and allows immediate usage.
* **Given** an employee with `employment_status = 'PERMANENT'`, **then** the system credits +1.66 days (for 20 days annual entitlement).
* **Given** the accrual run, **then** each employee receives an `ACCRUAL` transaction entry in `leave_ledger_entries` and their `leave_balances.accrued_days` is updated within a chunked batch transaction.

**Technical Tasks:**
1. Configure Spring `@EnableScheduling` with a dedicated `ThreadPoolTaskScheduler`.
2. Implement `MonthlyAccrualScheduler` with cron `0 5 0 1 * ?`.
3. Process employees in chunks of 50 users per transaction.
4. Write integration test verifying accrual calculation across probation and permanent employees.

---

## Epic 3: Leave Request Workflow & SLA Auto-Escalation Engine

**Epic Goal:** Guide leave requests through submission, validation, manager approval, 48-hour SLA auto-escalation, cancellation, and immutable state audit logging.

### Story 3.1: Leave Request Submission & Overlap Validation

As an Employee,  
I want to submit a leave request specifying leave type, dates, morning/afternoon shifts, and reason,  
So that my requested days are reserved and routed to my direct line manager for review.

**Acceptance Criteria:**

* **Given** a request submission `POST /api/v1/leaves/submit`, **then** the system validates that `startDate <= endDate` and rejects requests overlapping with existing `SUBMITTED`, `ESCALATED`, or `APPROVED` requests for the same employee with HTTP 409 Conflict.
* **Given** valid submission, **then** the system increments `leave_balances.pending_days`, appends a `RESERVATION` ledger entry, and creates a `leave_requests` record with status `SUBMITTED`.
* **Given** an `Idempotency-Key` header, **when** duplicate requests are submitted rapidly, **then** the second request returns the cached initial response without double-booking balance.

**Technical Tasks:**
1. Create Flyway migration `V4__leave_requests.sql` defining `leave_requests` table and indexes.
2. Implement `LeaveRequest` entity with enum statuses (`DRAFT`, `SUBMITTED`, `ESCALATED`, `APPROVED`, `REJECTED`, `CANCELLED`, `CANCEL_REQUESTED`).
3. Implement `IdempotencyFilter` caching keys in memory or database.
4. Implement `LeaveRequestService.submitLeave(...)` executing validation and balance reservation.

---

### Story 3.2: 1-Tier Approval Routing & Direct Manager Resolution

As a Direct Line Manager,  
I want to view pending leave requests from my direct reports and approve or reject them with feedback,  
So that team scheduling is maintained and decisions are recorded promptly.

**Acceptance Criteria:**

* **Given** a manager calling `GET /api/v1/approvals/pending`, **then** the system returns all requests where `assigned_approver_id = managerId` and `status IN ('SUBMITTED', 'ESCALATED')`.
* **Given** an approval `POST /api/v1/approvals/{id}/act` with `decision = APPROVE`, **then** request status transitions to `APPROVED`, pending days shift to `used_days`, and a `DEDUCTION` ledger entry is appended.
* **Given** a rejection with `decision = REJECT`, **then** a mandatory rejection reason is required, request status transitions to `REJECTED`, and pending days are released back to available balance (`RESERVATION_RELEASE`).

**Technical Tasks:**
1. Implement `ApprovalController` with endpoints `GET /api/v1/approvals/pending` and `POST /api/v1/approvals/{id}/act`.
2. Implement approval and rejection state transitions in `LeaveRequestService`.
3. Enforce that only the assigned manager (or Skip-Level/HR for escalated requests) can execute approval.
4. Write unit tests for approval and rejection state transitions and ledger reconciliations.

---

### Story 3.3: 24h Warning & 48h SLA Auto-Escalation Scheduler

As a Department Head & HR Administrator,  
I want an automated scheduler that warns managers at 24 hours and auto-escalates unresolved requests to Skip-Level/HR at 48 hours,  
So that employees are not left waiting indefinitely for leave decisions.

**Acceptance Criteria:**

* **Given** a request in `SUBMITTED` status for $\ge 24$ business hours (or $\le 24\text{h}$ prior to leave start date) with `reminder_sent = false`, **when** the SLA scheduler runs, **then** it sends a high-priority warning alert to the manager and marks `reminder_sent = true`.
* **Given** a request in `SUBMITTED` status for $\ge 48$ business hours without action, **when** the scheduler runs, **then** it transitions status to `ESCALATED`, sets `escalated_at = NOW()`, and re-routes the approval task to the Skip-Level Manager and HR Admin queue.
* **Given** an `ESCALATED` request, **then** Skip-Level Managers and HR Admins have full authority to approve or reject.

**Technical Tasks:**
1. Implement `SlaEscalationScheduler` running every 15 minutes (`0 */15 * * * ?`).
2. Implement business-hour calculation counting elapsed hours between `08:00–12:00` and `13:00–17:00` Mon–Fri excluding public holidays.
3. Optimize SLA query with index on `(status, submitted_at, reminder_sent)`.
4. Log escalation transition to `system_audit_logs`.

---

### Story 3.4: Leave Cancellation & Backdated Request Handling

As an Employee or Manager,  
I want clear rules for cancelling approved leaves and processing emergency backdated requests,  
So that exceptional scheduling changes are handled with full audit integrity.

**Acceptance Criteria:**

* **Given** an approved leave prior to its start date, **when** the employee requests cancellation, **then** the system unilaterally cancels the request, releases `used_days` back to available balance, and appends a `REVERSAL` ledger record.
* **Given** an approved leave on or after its start date, **when** cancellation is requested, **then** status changes to `CANCEL_REQUESTED` requiring manager or HR approval before reversing the balance.
* **Given** a leave request where `startDate < currentDate`, **then** the system flags `is_backdated = true`, requires mandatory justification, and enforces dual approval by both Direct Manager and HR Admin.

**Technical Tasks:**
1. Implement cancellation endpoints: `POST /api/v1/leaves/{id}/cancel`.
2. Add backdated request evaluation in `LeaveRequestService`.
3. Implement reversal transactions in `LeaveLedgerService`.

---

## Epic 4: Medical Privacy & Compliance Reporting

**Epic Goal:** Ensure statutory medical document confidentiality by isolating file storage and restricting download streams to `ROLE_HR_ADMIN`, while empowering HR with immutable audit logs and monthly payroll reconciliation exports.

### Story 4.1: Confidential Medical Attachment Storage & Access Gate

As an Employee taking sick leave,  
I want to upload medical certificates with strict privacy guarantees,  
So that my direct line manager cannot view or download my confidential health documents.

**Acceptance Criteria:**

* **Given** a Sick Leave submission, **when** uploading a medical certificate (PDF, JPEG, PNG, max 5MB), **then** the file is stored in a non-web-root secure directory with a randomized UUID filename.
* **Given** a file download request `GET /api/v1/attachments/{fileUuid}/download`, **then** access is permitted ONLY if the authenticated user has role `ROLE_HR_ADMIN` or is the owner who uploaded the file.
* **Given** a Direct Line Manager attempting to download their subordinate's medical certificate, **then** the system returns HTTP 403 Forbidden. The manager interface displays only `medical_certificate_attached: true`.

**Technical Tasks:**
1. Create Flyway migration `V5__leave_attachments.sql` for `leave_attachments`.
2. Implement `SecureFileStorageService` storing files on disk outside web root with cryptographic UUIDs.
3. Implement `LeaveAttachmentController` with `@PreAuthorize("hasRole('HR_ADMIN') or @securityUtils.isAttachmentOwner(#fileUuid, authentication.principal.id)")`.
4. Validate MIME types and file size limits (5MB max).

---

### Story 4.2: Immutable System Audit Logging

As an HR Compliance Officer,  
I want an immutable audit log of every state transition and administrative action,  
So that system audits are 100% tamper-proof and traceable.

**Acceptance Criteria:**

* **Given** any state transition (`SUBMIT`, `APPROVE`, `REJECT`, `ESCALATE`, `CANCEL`, `ADJUSTMENT`), **then** an audit record is saved containing `entity_name`, `entity_id`, `action`, `actor_user_id`, `previous_state`, `new_state` (JSON), and `ip_address`.
* **Given** the audit records in `system_audit_logs`, **then** no API endpoint exists to update or delete these records.

**Technical Tasks:**
1. Create Flyway migration `V6__system_audit_logs.sql`.
2. Implement `AuditLogService` with `@Async` execution for non-blocking persistence.
3. Capture client IP address via `HttpServletRequest`.

---

### Story 4.3: Monthly Payroll Deduction & Accrual Export

As an HR Administrator,  
I want to generate monthly payroll deduction reports in CSV and Excel (`.xlsx`) formats,  
So that finance can execute accurate salary deductions for unpaid leaves.

**Acceptance Criteria:**

* **Given** `GET /api/v1/reports/payroll?month=2026-09&format=xlsx` (or `csv`), **then** access is restricted to `ROLE_HR_ADMIN` (returning HTTP 403 for other roles).
* **Given** the exported report, **then** it includes `EmployeeID`, `EmployeeName`, `Department`, `UnpaidLeaveDays`, `PaidLeaveDaysTaken`, and `ClosingBalance`.

**Technical Tasks:**
1. Implement `PayrollExportService` querying ledger deductions aggregated by department and month.
2. Generate Excel workbooks via Apache POI and CSV streaming via OpenCSV.
3. Expose `PayrollReportController` with appropriate `Content-Disposition: attachment; filename=...` headers.

---

## Epic 5: Employee Self-Service & Manager Portal (React 18 + Ant Design)

**Epic Goal:** Provide an intuitive, responsive web application for employees and managers featuring real-time balance cards, dynamic request submission with conflict checks, approvals inbox with SLA countdown chips, and privacy-masked team presence calendars.

### Story 5.1: Frontend Scaffold, Auth State & Protected Routing

As a Web User,  
I want a responsive application shell with secure login and role-aware navigation,  
So that I can access features appropriate to my role on desktop and mobile browsers.

**Acceptance Criteria:**

* **Given** the frontend project, **then** Vite 5 + React 18 + TypeScript + Ant Design 5 are configured with centralized token styling.
* **Given** an API request returning HTTP 401, **then** the Axios interceptor attempts token refresh and retries the request seamlessly before redirecting to `/login`.
* **Given** protected routes, **then** `RoleGuard` restricts `/approvals` to managers/HR and `/hr/**` to HR Admins.

**Technical Tasks:**
1. Initialize Vite React TypeScript project with Ant Design (`antd`) and TanStack Query.
2. Implement `apiClient.ts` with request/response interceptors for JWT Bearer token and 401 refresh logic.
3. Implement `AuthContext`, `useAuth` hook, and `RoleGuard` component.
4. Build `MainLayout` with header, responsive sidebar, and user profile display.

---

### Story 5.2: Employee Dashboard & Metric Cards

As an Employee,  
I want to view my current available leave balances and past request status at a glance,  
So that I know exactly how many days I have before requesting time off.

**Acceptance Criteria:**

* **Given** the employee dashboard, **then** metric cards display Annual Leave (Accrued, Used, Pending, Remaining) and Sick Leave balances.
* **Given** the recent requests table, **then** requests are displayed with status tags (`SUBMITTED`, `ESCALATED`, `APPROVED`, `REJECTED`, `CANCELLED`).
* **Given** an approved request with a future start date, **then** a "Cancel Request" button is displayed allowing unilateral pre-leave cancellation.

**Technical Tasks:**
1. Implement `EmployeeDashboardPage` component.
2. Build `BalanceMetricsCards` component consuming `useQuery(['leaves', 'balances'])`.
3. Build `RecentRequestsTable` with pagination, status filters, and pre-leave cancel action.

---

### Story 5.3: Dynamic Leave Request Modal with Live Conflict & Day Calculator

As an Employee,  
I want an interactive leave request modal that calculates billable days and warns of team conflicts in real-time,  
So that I submit valid requests with confidence.

**Acceptance Criteria:**

* **Given** the request modal, **then** the employee can select leave type, date range, morning/afternoon shift toggles, reason, and upload attachment.
* **Given** date range selection, **then** the form debounces a call to `/calculate-days` and dynamically displays billable days excluding weekends and holidays.
* **Given** days exceeding available balance, **then** the submit button is disabled with an explanatory tooltip.
* **Given** successful submission, **then** TanStack Query cache for balances and my-requests are invalidated immediately.

**Technical Tasks:**
1. Implement `LeaveSubmissionModal` with Ant Design `Form`, `DatePicker.RangePicker`, and `Radio.Group`.
2. Integrate file upload dropzone with client-side 5MB size and format validation.
3. Implement `useMutation` hook with optimistic UI feedback and cache invalidation.

---

### Story 5.4: Manager Approvals Inbox with SLA Countdown Urgency Chips

As a Direct Manager,  
I want an approvals inbox with visual SLA timers and team overlap warnings,  
So that I can prioritize urgent requests before the 48h escalation threshold.

**Acceptance Criteria:**

* **Given** pending approval requests, **then** each card displays an SLA urgency countdown chip:
  * **Green Chip:** $> 24\text{h}$ remaining until SLA breach.
  * **Yellow Chip:** $\le 24\text{h}$ remaining (urgent action required).
  * **Red Pulsing Chip:** `ESCALATED` (breached 48h SLA, visible to Skip-Level/HR).
* **Given** multiple team members on leave on overlapping dates, **then** a Team Overlap warning banner indicates remaining team capacity percentage.
* **Given** clicking "Approve" or "Reject", **then** the manager can approve in 1 click or submit a mandatory rejection reason.

**Technical Tasks:**
1. Implement `ManagerApprovalsPage` consuming `useQuery(['approvals', 'pending'])`.
2. Implement `SlaCountdownChip` component calculating remaining business hours.
3. Build `TeamOverlapWarning` indicator.
4. Implement approve and reject modal workflows with cache invalidation.

---

### Story 5.5: Interactive Team Presence Calendar with Privacy Masking

As an Employee or Manager,  
I want a monthly/weekly calendar view of team attendance,  
So that I can coordinate coverage while respecting colleague medical confidentiality.

**Acceptance Criteria:**

* **Given** `TeamCalendarPage`, **then** a monthly/weekly grid (Ant Design `Calendar`) displays scheduled leaves for department members.
* **Given** a colleague viewing peer leave cards, **then** the reason is masked to "Out of Office / Leave". Confidential medical details are strictly hidden from peers.
* **Given** a mobile viewport ($< 768\text{px}$), **then** the calendar automatically collapses into a vertical day-agenda list.

**Technical Tasks:**
1. Implement `TeamCalendarPage` using Ant Design `Calendar` with custom `cellRender`.
2. Implement responsive styling toggling to agenda view on mobile.
3. Write end-to-end user journey test for submission-to-calendar sync.

---

*This document constitutes the final backlog for Sprint Planning and execution by Amelia (`@bmad-agent-dev`).*
