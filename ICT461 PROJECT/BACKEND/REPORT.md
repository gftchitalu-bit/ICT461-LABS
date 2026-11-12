# Backend implementation report

## What was built

The backend is a Java 21 / Spring Boot 3.4 Maven application in the requested Maven layout. It defines repository records and interfaces in `data/Data.java`, plus a `mem`-profile in-memory implementation seeded with the five requested accounts and BCrypt password hashes. Session IDs and share tokens are generated as random URL-safe values; only their SHA-256 hex digests are retained by repositories. The API contract is maintained in `src/main/resources/static/openapi.yaml` and the Swagger UI is configured at `/api-docs` to load that file.

Controllers cover login, logout, current-user lookup, issuing/listing/getting credentials, revocation request/approval, share links, verification, and audit events. The services enforce graduate existence/ownership, two-registrar approval, credential-state conflicts, expiring links, QR creation, and audit events. Revocation approval performs both state changes together inside the synchronized repository operation. No SQL, schema, frontend, or WebSocket code was added.

## Request check order

Every `/api/v1/**` request is checked in this order, before request-body parsing:

1. **RateLimitInterceptor** — per-IP, per-minute buckets; login defaults to 10 and other requests to 120. It sets the three `X-RateLimit-*` headers and adds `Retry-After` on 429.
2. **JsonOnlyInterceptor** — all methods other than GET, HEAD, and OPTIONS require `application/json`; otherwise 415.
3. **SessionInterceptor** — except for `@Public` methods, hashes and looks up `session_id`, rejecting missing, revoked, expired, or inactive sessions with 401.
4. **RoleInterceptor** — applies `@Roles`, returning 403 for disallowed roles.
5. **Spring/Jackson parsing and validation** — malformed/unknown/wrong-type body data is 400; validly parsed but invalid values are 422.

API exceptions use `application/problem+json` with RFC 7807 fields and an `errors` array. Unexpected server exceptions are logged and their internal messages are not returned.

## Validation checklist

| Contract rule | Matching server rule | Status code |
|---|---|---:|
| Login email: 5–190 chars, email format | `@Size`, `@Email`, `@NotBlank` on `LoginRequest.email` | 422 |
| Login password: 8–72 chars | `@Size` and `@NotNull` on `LoginRequest.password` | 422 |
| Credential graduateId: integer >= 1 | `@Min(1)`; missing/nonpositive is rejected | 422 |
| Credential programme: 3–120 chars, required pattern, nonblank | `@Size`, matching `@Pattern`, `@NotBlank` | 422 |
| Credential awardDate: ISO date, not future | `LocalDate` parsing plus `@PastOrPresent` | 400 malformed; 422 future |
| Revocation reason: 3–500 chars, no `<` or `>` | `@Size`, matching `@Pattern`, `@NotBlank` | 422 |
| Share expiresInHours: 1–720 | `@Min(1)` and `@Max(720)` | 422 |
| List q: <= 100 chars and programme pattern if present | `@Size` and matching `@Pattern` | 422 |
| List status: ACTIVE or REVOKED | `@Pattern("^(ACTIVE|REVOKED)$")` | 422 |
| List/audit page: 0–100000; size: 1–100 | `@Min` / `@Max`, defaults 0 and 20 | 422 |
| Resource IDs: positive int64 | `long` path parameter with `@Min(1)` | 400 wrong type; 422 constraint |
| Verify token: exactly 43 URL-safe characters | `@Pattern("^[A-Za-z0-9_-]{43}$")` | 422 |
| Invalid JSON, unknown field, wrong JSON/query/path type, malformed date | Jackson strict unknown/scalar handling; MVC type/read errors | 400 |
| Missing/wrong/expired session or inactive user | `SessionInterceptor` | 401 |
| Wrong role or cross-owner access | `@Roles` and service-level owner check | 403 |
| Missing credential/revocation/token | Service lookup | 404 |
| Inactive credential, pending/approved revocation, or revoked share | Service state checks | 409 |
| Request validation failures after successful parsing | Bean/MVC method validation | 422 |
| POST without application/json | `JsonOnlyInterceptor` | 415 |
| Per-IP rate limit exceeded | `RateLimitInterceptor` | 429 |
| Unexpected server exception | Sanitized API exception handler; server log | 500 |

## Business rules and status codes

Credential issuance is registrar-only, requires an existing graduate, creates an ACTIVE credential, writes `CREDENTIAL_ISSUED`, and returns 201 with `Location`. Listing is registrar/auditor-only and paginated; a credential is readable by registrars or its owning graduate. Only an ACTIVE credential can receive a PENDING revocation request. Its requester cannot approve it; another registrar approves it and atomically changes the request to APPROVED and credential to REVOKED. Repeats and invalid state transitions return 409.

Graduates may create share links only for their own non-revoked credentials. A link expires after 1–720 hours and returns a random token, verification URL, PNG QR data URI, and expiration timestamp. Verification is public: unknown token is 404; revoked or expired links return `valid=false` and the corresponding status; active links expose only programme and award date. Audit listing is auditor-only and newest-first.

## Sessions, roles, logout, and audit

Login uses BCrypt and a dummy BCrypt comparison for unknown email addresses; failed credentials share one generic 401. Successful login sets an HttpOnly, SameSite=Strict, Path=/ session cookie with a 15-minute TTL and configurable Secure flag. Logout revokes the hashed session, writes `LOGOUT`, expires the cookie using the same cookie settings, and returns 204 with no body. `/auth/me` returns the authenticated user. Session and role checks run before JSON parsing; JSON-only POSTs are used as the cross-site request defence alongside SameSite=Strict.

## Figures

1. **Figure 1.** Swagger UI at `/api-docs`, showing the hand-authored OpenAPI contract.
2. **Figure 2.** Swagger UI login “Try it out” response and issued `session_id` cookie.
3. **Figure 3.** Authenticated `/auth/me` “Try it out” response.
4. **Figure 4.** MockMvc revocation approval flow, including self-approval denial and second-registrar approval.

Screenshots are not embedded; the application was not started, so the captions describe the requested figures rather than captured images.

## Assumptions and verification

The endpoint notes available in the repository document fewer operations than the supplied task. The implementation uses the task’s explicit path `/revocations/{revocationId}/approvals`. The temporary `mem` profile is the default so a local run does not require a database; no database persistence is claimed.

The backend was compiled and tested with Maven 3.9.9 on Java 21: **13 tests run, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**. During verification, the POM was found targeting Java 25; it has been corrected to the required Java 21. The fetched `Ict461/main` and local `main` point to the same commit, so there were no upstream commits to merge. The Java test context successfully initialized the application and exercised the API controllers. A separate application launch, browser verification of `/api-docs` and Swagger Try it out for login and `/auth/me`, and screenshots were not performed.
