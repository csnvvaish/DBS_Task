# Security Acceptance Criteria

## Authentication

| Test | Expected |
|---|---|
| Register with valid data | 201 Created |
| Wrong password | 401 Unauthorized |
| Correct login | 200 OK |
| Logout | Session invalidated |
| Expired session | Protected request rejected |

## Authorization

| Test | Expected |
|---|---|
| No authentication → `/api/user/**` | 401 Unauthorized |
| USER → `/api/user/**` | 200 OK |
| USER → `/api/admin/**` | 403 Forbidden |
| MANAGER → `/api/admin/**` | 403 Forbidden |
| ADMIN → `/api/admin/**` | 200 OK |
| USER directly calls `/api/admin/...` | 403 Forbidden |

## Registration Validation

| Test | Expected |
|---|---|
| Registration contains `"role":"ROLE_ADMIN"` | Role not accepted/assigned |
| Duplicate username | Rejected |
| Duplicate email | Rejected |
| Invalid email | Rejected |
| Weak/invalid input | Rejected |

## Frontend

| Test | Expected |
|---|---|
| USER navigates to `/admin` | React blocks access |

## Security principle

React route protection is not the security boundary.

Spring Security must enforce authorization on the backend.

The backend must reject unauthorized direct API requests even when
the user bypasses the React UI.