Recent requests Table uses scroll={{ x: 620 }} so dense columns scroll inside the table on mobile.
At max-width:600px, .record-row changes to a two-column grid; name and department wrap in column one while status remains in column two.

# Independent Review Handoff: Blind Hunter

You are a context-free reviewer. Do not invoke skills or spawn reviewers. Return findings only as a Markdown list, without severity, priority, ranking, or general commentary.

Conduct a review of CONTENT. Look for what is missing, not only what is wrong. Compute your finding floor N from the content size in kilobytes: N = min(floor(sqrt(kB) + 1), 10). State the arithmetic in one line, then find at least N issues to fix or improve. If content is empty, stop and say so. If you have zero findings, re-check and keep thinking; do not stop with an empty list.

Review scope is the following best-effort NO_VCS snapshot. The original workspace has no Git metadata, so the snapshot records the changed surfaces and their implementation behavior, not a generated before/after patch. Treat the specified frontend files as the source of truth if a summary is ambiguous.

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
