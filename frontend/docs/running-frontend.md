# SecureBank Frontend — Running & Building

## Prerequisites

- **Node.js 20+** and npm.
- The **backend** running on `http://localhost:8080` (context path `/api`) for live data.
  The UI still loads without it; data sections will show error/retry states.

## Install

```bash
cd frontend
npm install
```

## Develop

```bash
npm run dev
```

- Serves on **http://localhost:5173** (fixed by the project spec).
- Hot Module Replacement is on.
- Requests to `/api/*` are proxied to `http://localhost:8080` (see `vite.config.ts`), so
  the browser never makes cross-origin calls and you don't need CORS in dev.

## Other scripts

| Script | What it does |
|---|---|
| `npm run build` | type-checks (`tsc -b`) then builds the production bundle into `dist/` |
| `npm run preview` | serves the built `dist/` locally to sanity-check the production build |
| `npm run lint` | ESLint over `src` (TypeScript + React rules) |
| `npm run test` | runs the Vitest suite once (jsdom env) |
| `npm run test:watch` | Vitest in watch mode |

## Environment & the API proxy

The app talks to the backend **only** via the relative path `/api`. There is no API base
URL to configure:

- **Dev:** Vite proxies `/api` → `http://localhost:8080` (`vite.config.ts`).
- **Prod:** nginx proxies `/api` → the `securebank-api` service (`nginx.conf`).

`.env.example` documents the (optional) knobs. If your backend runs elsewhere in dev,
change the `proxy.target` in `vite.config.ts`.

## Building & running the Docker image

The `Dockerfile` is multi-stage: it builds with Node, then serves the static `dist/`
with nginx (no Node in the final image).

```bash
# from frontend/
docker build -t securebank-frontend .
docker run --rm -p 8080:80 securebank-frontend
# open http://localhost:8080
```

In `docker-compose` / Kubernetes, the frontend container expects the backend to be
reachable at service name `securebank-api:8080` (the upstream in `nginx.conf`). Adjust
that hostname to match your orchestration if different.

## Project structure & docs

- Architecture & data flow: `docs/frontend-architecture.md`
- Components & the transfer flow: `docs/frontend-LLD.md`
- Redux/RTK Query, tags, re-auth: `docs/state-management.md`
- i18n: `docs/i18n.md`
- Design system: `docs/ui-design-system.md`

## Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| Data sections show "Failed to load" | backend not running on :8080, or proxy target wrong |
| Login succeeds then immediately logs out | backend refresh endpoint returning non-2xx; check `/auth/refresh` |
| Strings show as keys (e.g. `nav.dashboard`) | locale JSON missing that key; add it to all three files |
| Build fails on types | run `npm run build` to see `tsc` errors; strict mode is on |
