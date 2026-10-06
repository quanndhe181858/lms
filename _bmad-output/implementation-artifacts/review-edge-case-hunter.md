Recent requests Table uses scroll={{ x: 620 }} so dense columns scroll inside the table on mobile.
At max-width:600px, .record-row changes to a two-column grid; name and department wrap in column one while status remains in column two.

# Independent Review Handoff: Edge Case Hunter

You are a context-free reviewer. Do not invoke skills or spawn reviewers. Follow the execution instructions below exactly. Review only the supplied best-effort NO_VCS change snapshot and report only unhandled paths. The workspace lacks Git metadata, so the snapshot records the edited surfaces and behavior rather than a generated before/after patch; when the snapshot is ambiguous, use the source excerpts and claims included here.

## Edge Case Hunter Review Instructions

# Edge Case Hunter Review

**Goal:** You are a pure path tracer. Never comment on whether code is good or bad; only list missing handling. When a diff is provided, scan only the diff hunks and list boundaries that are directly reachable from the changed lines and lack an explicit guard in the diff. When no diff is provided, treat the entire provided content as the scope. Ignore the rest of the codebase unless the provided content explicitly references external functions.

**MANDATORY:** Execute steps in exact order. Do not skip steps or change sequence. When a halt condition triggers, follow its instruction exactly.

### Step 1: Receive Content

Take the content below as the review content. Identify it as a best-effort change snapshot because the repository has no VCS baseline.

### Step 2: Exhaustive Path Analysis

Walk every branching path and boundary condition within scope. Walk control flow and domain boundaries, including missing defaults, null/empty inputs, off-by-one loops, type coercion, race conditions, timeout gaps, and implicit branches in fixed sets. For each changed call site, check argument count, order, types, and defaults against the callee declaration when that information is supplied. Collect only unhandled paths; discard handled ones silently.

### Step 3: Validate Completeness

Revisit every edge class. Add newly found unhandled paths; discard paths confirmed handled.

### Step 4: Deletion Check

There are code replacements in the snapshot. Apply this deletion check: for each replaced behavior, ask whether the previous contract is re-established or intentionally retired; report resulting regression, orphaned reference, or newly-dead code only if directly supported by the snapshot.

### Step 5: Claims Check

Only now read the claims below. Read only the spec’s Intent and Tasks & Acceptance as the claims. Extract checkable claims about behavior, preservation, routes, responsive layout, and verification, then falsify each against the supplied change snapshot. Verified claims produce no finding.

### Step 6: Present Findings

Return ONLY a valid JSON array. Each finding has `location`, `trigger_condition` (max 15 words), `guard_snippet` (minimal one-line escaped code sketch), and `potential_consequence` (max 15 words). Deletion/claim findings also have `kind` and `confidence`. Return `[]` if no unhandled path is found. No markdown wrapper or extra prose.

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
+
+## Claims File (Do Not Read Until Step 5)
+
+```markdown
+## Intent
+
+**Problem:** The current Vite frontend uses a dark, blue-gradient shell and oversized panels that differ from the supplied leave-management reference, making the existing experience visually inconsistent with the desired employee-facing LMS.
+
+**Approach:** Redesign the full frontend presentation around the reference's warm ivory canvas, white surfaces, indigo actions, amber accents, compact geometry, sticky horizontal navigation, KPI-led dashboard, and responsive page layouts. Keep existing route paths, demo authentication, role guards, forms, approval flows, calendar, HR, and payroll behavior intact; do not add backend/API integration or change backend code.
+
+## Tasks & Acceptance
+
+**Execution:**
+- [x] `frontend/src/index.css` -- Establish shared warm-light tokens, typography, base sizing, and accessible focus treatment.
+- [x] `frontend/src/App.css` -- Rebuild the responsive shell, compact surfaces, KPI grid, forms, tables, calendar, login, and role-page styling to match the reference.
+- [x] `frontend/src/App.tsx` -- Apply the sticky horizontal shell and reference-like page hierarchy while preserving routes, role checks, current demo interactions, and all page content.
+- [x] `frontend/index.html` -- Set Vietnamese document language and a product-appropriate page title.
+
+**Acceptance Criteria:**
+- Given any existing route and an authorized demo role, when the page loads, then its existing behavior is unchanged and its visual styling follows the supplied reference.
+- Given a manager or HR account, when opening approvals, then the existing approval queue remains usable and status/SLA cues are clear.
+- Given an employee account, when opening the request form, then validation, date selection, submit feedback, and balance preview remain usable.
+- Given a viewport at 375px or wider, when navigating each page, then controls and content remain readable, keyboard-operable, and free of unintended horizontal page overflow.
+- Given an unauthenticated user or disallowed role, when opening a protected route, then existing redirects and access restrictions continue to work.
+- Given a clean frontend install, when `npm run build` and `npm run lint` run, then both succeed.
+```
