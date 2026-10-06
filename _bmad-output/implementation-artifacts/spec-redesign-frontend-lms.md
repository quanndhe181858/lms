---
title: 'Redesign leave-management frontend to match LMS reference'
type: 'feature'
created: '2026-10-01'
status: 'in-progress'
route: 'oneshot'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The leave-management frontend currently uses a dark sidebar and blue gradient dashboard that do not match the supplied LMS UI/UX reference.

**Approach:** Rework the frontend visual system and shell to match the reference's warm ivory canvas, indigo primary, amber borders/highlights, white panels, compact rounded geometry, sticky navigation, KPI-led dashboard hierarchy, and responsive spacing while preserving the existing application's routes, authentication, role guards, forms, approvals, calendar, HR, and payroll behavior.

</frozen-after-approval>

## Implementation Notes

- Reference source: `D:\Project\thiet-ke-ui-ux-lms\app\globals.css` and LMS components under `D:\Project\thiet-ke-ui-ux-lms\components\lms`.
- Current behavior owner: `frontend/src/App.tsx`; preserve all route paths and existing Ant Design interactions.
- Primary implementation surface: `frontend/src/index.css` and `frontend/src/App.css`, with only focused `frontend/src/App.tsx` class/structure changes where needed for the reference shell.
- Visual tokens: `#faf9f5` background, `#1c1917` foreground, `#ffffff` surface, `#6366f1` primary, `#eef0ff` secondary, `#fef3c7` accent, `#fde68a` border, `#eadfb9` input, `#10b981` success, `#b45309` warning, `#ef4444` danger, `0.75rem` base radius.
- Verification: run `npm run build` and `npm run lint` from `frontend`; inspect login, dashboard, approvals, calendar, people, payroll, and forbidden routes at desktop and narrow widths.
