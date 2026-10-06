---
title: 'Redesign leave-management frontend to match LMS reference'
type: 'feature'
created: '2026-10-01'
status: 'in-review'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: 'NO_VCS'
context:
  - '{project-root}/_bmad-output/planning-artifacts/requirements.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The current Vite frontend uses a dark, blue-gradient shell and oversized panels that differ from the supplied leave-management reference, making the existing experience visually inconsistent with the desired employee-facing LMS.

**Approach:** Redesign the full frontend presentation around the reference's warm ivory canvas, white surfaces, indigo actions, amber accents, compact geometry, sticky horizontal navigation, KPI-led dashboard, and responsive page layouts. Create a distinct NghỉPhép+ logo, enrich the login experience, localize every visible page to Vietnamese, add upcoming Vietnamese public holidays, and trim/reject invalid whitespace in login credentials. Keep existing route paths, demo authentication, role guards, forms, approval flows, calendar, HR, and payroll behavior intact; do not change backend code.

## Boundaries & Constraints

**Always:** Preserve current Vite + React + Ant Design stack and package set; keep `/login`, `/dashboard`, `/calendar`, `/approvals`, `/people`, `/payroll`, and `/forbidden` behavior and role restrictions. Use responsive layouts, keyboard-accessible controls, clear focus states, and Vietnamese interface copy consistent with the reference. Keep design tokens centralized in CSS and ensure loading, error, empty, and success states remain legible.

**Credential rule:** Trim both login fields, reject empty values and any remaining whitespace before authentication, and show validation/errors in Vietnamese.

**Never:** Replace the build framework, introduce Tailwind/Next.js or new dependencies, modify backend APIs or authentication contracts, remove existing pages, or present local demo data/actions as persisted backend state.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Authenticated navigation | Employee, manager, or HR opens permitted routes | Active navigation and matching page render in the new shell | Preserve existing redirect for unauthenticated users |
| Restricted route | Role opens a route outside its allowed set | Existing forbidden page remains reachable and visually consistent | Do not reveal protected page contents |
| Narrow viewport | Screen width reduces to mobile dimensions | Navigation, forms, cards, tables, and calendar remain usable without page-level overflow | Dense tables/calendar may scroll within their own region |
| Form validation | User submits an incomplete/invalid leave request | Existing validation and success/error feedback remain visible | Keep error text associated with the relevant control |
| Login credential whitespace | Blank, whitespace-only, internally spaced, or outer-padded email/password | Blank/internal whitespace is rejected; outer whitespace is trimmed and valid credentials authenticate | Show Vietnamese validation/error text without submitting invalid credentials |
| Upcoming public holidays | Employee opens dashboard after the current date | Only future Vietnam public holidays are shown in chronological order | Tet multi-day schedule is marked subject to official notice |

</frozen-after-approval>

## Code Map

- `frontend/src/App.tsx` -- Owns auth context, role guards, routes, and all existing page content. Retain its route and interaction contracts while restructuring the shell and page presentation.
- `frontend/src/App.css` -- Current shell, dashboard, and responsive rules; replace the dark/blue visual system with reference-aligned component styling.
- `frontend/src/index.css` -- Global typography, reset, and page canvas; define shared design tokens here.
- `frontend/index.html` -- Browser document language/title metadata; align with the Vietnamese leave-management app.
- `D:/Project/thiet-ke-ui-ux-lms/app/globals.css` -- Reference color, surface, radius, and typography tokens.
- `D:/Project/thiet-ke-ui-ux-lms/components/lms/` -- Reference patterns for header, overview, approvals, request form, calendar, reports, and footer. Adapt patterns to installed Ant Design; do not copy its Next.js/Tailwind dependencies.
- `frontend/src/main.tsx` -- Existing React entry point; no change expected.

## Tasks & Acceptance

**Execution:**
- [x] `frontend/src/index.css` -- Establish shared warm-light tokens, typography, base sizing, and accessible focus treatment.
- [x] `frontend/src/App.css` -- Rebuild the responsive shell, compact surfaces, KPI grid, forms, tables, calendar, login, and role-page styling to match the reference.
- [x] `frontend/src/App.tsx` -- Apply the sticky horizontal shell and reference-like page hierarchy while preserving routes, role checks, current demo interactions, and all page content.
- [x] `frontend/index.html` -- Set Vietnamese document language and a product-appropriate page title.
- [x] `frontend/public/brand-mark.svg` -- Add the NghỉPhép+ mark for the header, login page, and browser favicon.
- [x] `frontend/src/App.tsx` -- Add upcoming statutory holiday data, Vietnamese component locale/copy, and trimmed credential validation.

**Acceptance Criteria:**
- Given any existing route and an authorized demo role, when the page loads, then its existing behavior is unchanged and its visual styling follows the supplied reference.
- Given a manager or HR account, when opening approvals, then the existing approval queue remains usable and status/SLA cues are clear.
- Given an employee account, when opening the request form, then validation, date selection, submit feedback, and balance preview remain usable.
- Given a viewport at 375px or wider, when navigating each page, then controls and content remain readable, keyboard-operable, and free of unintended horizontal page overflow.
- Given an unauthenticated user or disallowed role, when opening a protected route, then existing redirects and access restrictions continue to work.
- Given blank or whitespace-only credentials, when submitting login, then authentication is blocked with Vietnamese field validation.
- Given credentials padded with outer whitespace, when submitting, then values are trimmed and valid demo credentials still authenticate; internal whitespace is rejected.
- Given the current date, when viewing upcoming holidays, then only future Vietnamese public holidays are listed and leave-day calculation uses the same holiday dates.
- Given a clean frontend install, when `npm run build` and `npm run lint` run, then both succeed.

## Implementation Notes

- Adapt the supplied Next.js/Tailwind appearance to the current Ant Design application; do not transplant framework-specific markup.
- The existing frontend has no test files or Playwright configuration. Verify with build/lint and manually exercise each route at desktop and mobile widths.
- Added a sticky, horizontal responsive header with role-filtered route links while retaining route guards.
- Corrected local date key formatting so timezone offsets cannot move a selected leave day into the prior date during holiday checks.
- Replaced deprecated Ant Design 6 properties with current equivalents to keep the runtime console free of those warnings.
- Reflowed the HR people rows into a narrow-screen grid so name, department, and status remain within the card.
- Added a branded calendar/check SVG for app identity and favicon; replaced the sparse login with a responsive Vietnamese welcome/calendar/sign-in layout.
- Added upcoming 2027 public holiday dates, preserved the 2026 National Day holiday for calculations, and localized the Ant Design date/calendar controls.
- Verified 2027 lunar dates with the runtime lunisolar calendar: Tết is 2027-02-07 and Giỗ Tổ Hùng Vương is 2027-04-16; Tết's multi-day break remains subject to official scheduling.
- Added four KPI-style dashboard cards, a richer HR directory/payroll composition, and current October team-calendar examples.
- Replaced the month-only calendar with a responsive two-workweek team timeline matching the supplied schedule image: member rows, weekday columns, category/status bars, week navigation, and absence totals.
- Schedule styling uses a scoped pastel-yellow canvas and amber grid, with solid approved bars and dashed pending bars in type-specific colors; mobile keeps horizontal date scrolling inside the schedule frame.
- Schedule timeline build and lint pass; desktop and 375px screenshots confirmed the reference-style grid and non-overlapping mobile toolbar.
- Login browser checks passed for blank, whitespace-only, internal-space, and padded-valid credentials; padded values authenticate after trimming and auth rejects invalid whitespace independently of the form.
- Verified a true 375px viewport: login content is readable with submit visible, dashboard has no page-level horizontal overflow, empty leave form stays disabled, and employee approvals access redirects to forbidden.
- Final `npm run build`, `npm run lint`, and TypeScript editor diagnostics pass; only Vite's existing large-chunk warning remains.

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- From `frontend`: `npm run build` -- expected: TypeScript and Vite production build succeed.
- From `frontend`: `npm run lint` -- expected: ESLint reports no errors.

**Manual checks:**
- Exercise login, dashboard, approvals, calendar, people, payroll, and forbidden routes with each relevant role.
- Inspect desktop and 375px mobile layouts; confirm no unintended page overflow and visible keyboard focus.
