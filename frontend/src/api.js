async function readResponse(response) {
  if (response.status === 204) return null;

  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(body?.detail || body?.error || body?.message || 'The request could not be completed.');
  }
  return body;
}

async function csrfToken() {
  const response = await fetch('/api/auth/csrf', { credentials: 'include' });
  await readResponse(response);
  const cookie = document.cookie.split(';').map((part) => part.trim())
    .find((part) => part.startsWith('XSRF-TOKEN='));
  if (!cookie) throw new Error('Could not initialize request security.');
  return decodeURIComponent(cookie.slice('XSRF-TOKEN='.length));
}

async function send(method, path, body) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (method !== 'GET') headers['X-XSRF-TOKEN'] = await csrfToken();

  const response = await fetch(path, {
    method,
    headers,
    credentials: 'include',
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  return readResponse(response);
}

export const api = {
  register: (values) => send('POST', '/api/auth/register', values),
  login: (values) => send('POST', '/api/auth/login', values),
  me: () => send('GET', '/api/auth/me'),
  logout: () => send('POST', '/api/auth/logout'),
  dashboard: (role) => {
    const path = {
      ROLE_USER: '/api/user/dashboard',
      ROLE_MANAGER: '/api/manager/dashboard',
      ROLE_ADMIN: '/api/admin/dashboard',
    }[role];
    return send('GET', path);
  },
  users: () => send('GET', '/api/admin/users'),
  updateRole: (id, role) => send('PUT', `/api/admin/users/${id}/role`, { role }),
};