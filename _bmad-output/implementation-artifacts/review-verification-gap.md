Recent requests Table uses scroll={{ x: 620 }} so dense columns scroll inside the table on mobile.
At max-width:600px, .record-row changes to a two-column grid; name and department wrap in column one while status remains in column two.

# Independent Review Handoff: Verification Gap Reviewer

You are a context-free reviewer. Do not invoke skills or spawn reviewers. Follow every instruction below. Review the best-effort NO_VCS snapshot inline; the repository has no Git metadata and this snapshot records edited behavior rather than a generated before/after diff.

## Verification Gap Review Instructions

**Goal:** Find changed behavior that could break without reliable verification catching it. Ask: “if the behavior this change is supposed to produce broke where it's actually used, would verification fail?” Do not hunt for correctness bugs, but report genuine problems noticed while tracing verification as `Other findings`.

Main gap shapes:
1. Regression gap: changed code regresses where used and no test covering that use would fail.
2. Missing-adoption gap: a place that should use new behavior does not, and no test flags the omission.
3. Broken-verification gap: an apparent test does not protect the behavior because it is skipped, flaky, not normally run, or too weak.

Evidence rules:
- Read a test before claiming what it covers, runs, asserts, or misses.
- Before claiming no test exists, search the whole repo by symbol and import references.
- Never assert what you did not verify.
- State exactly what you checked and how far you looked.
- Do not assign severity, confidence, priority, or ranking.

Review sequence:
1. Screen each changed part for observable behavior changes; skip only genuinely non-behavioral changes.
2. Identify each changed behavior and trace it to its nearest consumer.
3. For each consumer, demonstrate a realistic regression and inspect relevant tests.
4. Confirm each finding against the actual tests/searches before reporting.
5. Report findings as Markdown blocks using the exact template below; if there are no gaps or other findings, output exactly `No verification gaps found.`

Finding template:

### <one-line title naming the gap>

- **Changed surface:** exact behavior/contract and file location.
- **Impacted consumer or site:** concrete consumer and file location.
- **Existing test evidence:** what relevant test asserts, or symbol/import-reference searches and result.
- **Missing verification:** precise assertion/check absent.
- **Demonstration:** concrete regression and why checked tests would not fail.
- **Consequence:** concrete shipped behavior.
- **Disposition:** `patch` with test to add, or `defer` and why.

For genuine non-gap bugs noticed during verification tracing, append `## Other findings` and list descriptions only.

## Review Content

```diff
# baseline_commit=NO_VCS; exact before/after diff unavailable

diff --git a/frontend/src/index.css b/frontend/src/index.css
--- a/frontend/src/index.css
+++ b/frontend/src/index.css
@@ global theme and base styles @@
+:root defines --canvas #faf9f5, --surface #fff, --surface-soft #fffbeb, --ink #1c1917,
+--muted #6b6358, --primary #6366f1, --primary-soft #eef0ff, --accent #fef3c7,
+--border #eadfb9, success/warning/danger tokens, 8px control and 12px panel radii.
+Global box sizing, 320px minimum canvas, smooth scroll, focus-visible ring, and selection tint added.
+
+diff --git a/frontend/src/App.css b/frontend/src/App.css
+--- a/frontend/src/App.css
++++ b/frontend/src/App.css
+@@ reference-aligned presentation layer @@
++.app-shell uses the ivory canvas, width/min-width constraints, and clipped outer horizontal overflow.
++.topbar is sticky at top with display:block; .header-inner contains brand, role-filtered desktop menu, and profile.
++.mobile-nav is a horizontally scrollable second row below 820px; dashboard content and vertical Space children are constrained to min-width:0.
++.sidebar-menu active/hover states use indigo-soft and amber; buttons, inputs, cards, tables, alerts, login, calendar, people/payroll/forbidden surfaces use shared tokens.
++375px rules stack page headers and metric columns, wrap headings, and keep action buttons usable; dashboard table receives an internal x-scroll region.
++prefers-reduced-motion disables transitions/animations. Information alerts use a soft indigo treatment.
+
+diff --git a/frontend/src/App.tsx b/frontend/src/App.tsx
+--- a/frontend/src/App.tsx
++++ b/frontend/src/App.tsx
+@@ date-only holiday key @@
+-return date.toISOString().split('T')[0]
++const year = date.getFullYear()
++const month = String(date.getMonth() + 1).padStart(2, '0')
++const day = String(date.getDate()).padStart(2, '0')
++return `${year}-${month}-${day}`
+@@ Ant Design 6 properties @@
+-Card bordered={false}
++Card variant="borderless"
+-Space direction="vertical"
++Space orientation="vertical"
+-Alert message={...}
++Alert title={...}
+@@ forbidden state @@
++ForbiddenPage is centered inside the same login-page canvas and state-card surface.
+@@ AppShell navigation @@
++Menu items declare roles: dashboard/calendar all roles, approvals manager/HR, people/payroll HR.
++visibleMenuItems filters nav by current role; ProtectedRoute remains unchanged and enforces access.
++Header is wrapped in header-inner, brand mark uses CalendarOutlined, profile shows Vietnamese role label,
++and mobile-nav renders visible items as buttons while preserving existing navigate/logout handlers.
+@@ narrow table @@
++Recent requests Table uses scroll={{ x: 620 }} so dense columns scroll inside the table on mobile.
+
+diff --git a/frontend/index.html b/frontend/index.html
+--- a/frontend/index.html
++++ b/frontend/index.html
+@@ document metadata @@
+-<html lang="en">
++<html lang="vi">
++Adds app theme-color/description and the title “NghỉPhép+ | Quản lý nghỉ phép”.
+
+## Verification observed
+- npm run build: passed; Vite retains an existing >500 kB vendor chunk notice.
+- npm run lint: passed.
+- Browser probe at an exact 375px iframe viewport: clientWidth and scrollWidth both 375.
+- Empty leave request dialog opened; primary action was disabled.
+- Employee navigation to /approvals resolved to /forbidden.
+```
