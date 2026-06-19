# SecureBank Frontend — UI & Design System

The UI is built on **shadcn/ui** primitives styled with **Tailwind CSS**. shadcn is not
an installed component library — the components are hand-written source we own and can
edit, living in `src/components/ui`.

## Design tokens (the core idea)

Colors are defined as **CSS custom properties** in `src/index.css`, as raw HSL channel
numbers:

```css
:root  { --primary: 221 83% 45%; --background: 0 0% 100%; ... }
.dark  { --primary: 217 91% 60%; --background: 222 47% 8%; ... }
```

`tailwind.config.js` maps Tailwind color utilities onto those variables:

```js
colors: { primary: "hsl(var(--primary) / <alpha-value>)", ... }
```

So `bg-primary`, `text-muted-foreground`, `border-border` etc. all resolve to a CSS
variable. **Theming is just swapping variable values** — toggling the `.dark` class on
`<html>` flips the entire palette with zero component changes. `<alpha-value>` keeps
opacity utilities (`bg-primary/50`) working.

### Palette intent (banking)

| Token | Role |
|---|---|
| `primary` (deep blue) | brand, primary actions, active nav |
| `success` (green) | money-in, positive states, completed |
| `destructive` (red) | money-out, errors, failed |
| `muted` / `secondary` | surfaces, borders, secondary text |
| `card` / `popover` | elevated surfaces |

A trustworthy blue/green retail-banking palette, tuned separately for light and dark.

## Theming

`hooks/useTheme.ts` reads a saved preference (or the OS `prefers-color-scheme`) and
toggles the `.dark` class on `<html>`, persisting to `localStorage`. `ThemeToggle` in
the topbar flips it; `sonner` toasts follow the same theme.

## Component inventory (`src/components/ui`)

| Primitive | Built on | Used for |
|---|---|---|
| `button` | Radix Slot + cva | all actions; variants: default/destructive/success/outline/secondary/ghost/link |
| `card` | div | every panel/surface |
| `input`, `label` | native + Radix Label | form fields |
| `form` | react-hook-form context | wires inputs to RHF + aria + zod messages |
| `select` | Radix Select | account/currency/type/language pickers |
| `dialog` | Radix Dialog | open-account, add-beneficiary modals |
| `dropdown-menu` | Radix DropdownMenu | user/account menu |
| `sheet` | Radix Dialog | mobile nav drawer |
| `tabs` | Radix Tabs | grouped views |
| `table` | native table | transactions, beneficiaries, audit logs |
| `badge` | div + cva | status pills, roles |
| `avatar` | Radix Avatar | user menu |
| `separator` | Radix Separator | dividers |
| `skeleton` | div | loading placeholders |
| `sonner` (toast) | sonner | success/error notifications |

`lib/utils.ts` provides `cn()` (clsx + tailwind-merge) — every primitive accepts a
`className` that cleanly overrides defaults because tailwind-merge resolves conflicts.

## Variants with `class-variance-authority` (cva)

Components like `button` and `badge` express their visual variants declaratively:

```ts
const buttonVariants = cva("base classes", {
  variants: { variant: { default: "...", destructive: "..." }, size: {...} },
  defaultVariants: { variant: "default", size: "default" },
});
```

This gives type-safe, discoverable props (`variant`, `size`) and one source of truth for
each look.

## Composed (non-ui) components

Built from the primitives, reused across pages: `AccountCard`, `TransactionsTable`,
`SpendingChart`, `Money`, `StatusBadge`, `PageHeader`, `States` (empty/error),
`LanguageSwitcher`, `ThemeToggle`, and the `layout/` shell (`AppLayout`, `Sidebar`,
`Topbar`).

## Accessibility

Radix primitives bring focus management, keyboard nav, and ARIA wiring for free. The
`form` primitive auto-associates labels, descriptions, and error messages
(`htmlFor`, `aria-describedby`, `aria-invalid`). Icon-only buttons carry `sr-only`
labels.

## Responsiveness

Tailwind breakpoints (`sm`, `lg`) drive the layout: the sidebar is visible on `lg+` and
collapses into a `Sheet` drawer on smaller screens; grids reflow from one to three
columns. Content is centered with a `max-w` container.
