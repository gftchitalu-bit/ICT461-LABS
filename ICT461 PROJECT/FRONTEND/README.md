# ICT461 Lab 1: Frontend (FE & UX)

Vanilla JavaScript (ES modules), semantic HTML and CSS. No build step.

## Run it

```bash
cd ict461-frontend
python3 -m http.server 8080
# open http://localhost:8080
```

It starts in mock mode (`USE_MOCK: true` in `js/config.js`), so it works with no backend.
Demo logins: `user@demo.com / user123` and `admin@demo.com / admin123`.

To connect the real backend, set `USE_MOCK: false` and fill in `API_BASE` and `WS_URL`.

## Structure

| File | Role in the architecture |
|------|--------------------------|
| `index.html` | Semantic page shell, skip link, live region, CSP meta tag |
| `css/styles.css` | Responsive layout, focus styles, 44px targets, dark mode, reduced motion |
| `js/router.js` | Hash router and route guards (public / guest / user / admin) |
| `js/views.js` | Pages: Landing, Login, Register, Dashboard, Records, Live feed, Profile, Users, Reports, Audit log |
| `js/components.js` | Reusable pieces: `h()`, `field()`, `table()`, `toast()`, form validation |
| `js/store.js` | State management (user, token, notifications, messages, socket status) |
| `js/session.js` | Login, logout and session restore |
| `js/api.js` | REST client over HTTPS (token header, error handling, mock backend) |
| `js/socket.js` | WebSocket client over WSS (auto-reconnect with backoff) |

## Endpoints the frontend expects (align with the OpenAPI 3.1 spec)

| Method | Path | Access |
|--------|------|--------|
| POST | `/auth/login`, `/auth/register` | public |
| PATCH | `/me` | user |
| GET | `/stats`, `/items` | user |
| POST | `/items` | user |
| DELETE | `/items/:id` | user |
| GET | `/admin/users`, `/admin/audit`, `/admin/reports` | admin |
| PATCH | `/admin/users/:id` (body `{ role }`) | admin |

WebSocket frames are JSON: `{ "type": "notification" | "message", "payload": { ... } }`.

## Notes for the report

- Accessibility (WCAG 2.2): skip link, one `h1` per page that receives focus on navigation, labelled form fields with inline errors, `aria-live` regions, visible focus, 44px targets, `prefers-reduced-motion`.
- Security: DOM built with `textContent` (no `innerHTML`), Content-Security-Policy meta tag, route guards for UX only, server must enforce RBAC and validate all input.
- Token storage: the token is kept in `sessionStorage` for simplicity. An httpOnly cookie set by the backend is safer against XSS and is worth discussing in the report.
- Replace `APP_NAME` in `js/config.js` and the landing page text with your project's real name and description.
