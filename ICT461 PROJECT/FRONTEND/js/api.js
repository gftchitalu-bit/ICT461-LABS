// REST client. Every call goes through request(), which adds the auth token,
// handles errors in one place and can switch to an in-memory mock backend.

import { CONFIG } from './config.js';
import { getState } from './store.js';

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

const delay = (ms) => new Promise((r) => setTimeout(r, ms));

async function request(path, { method = 'GET', body } = {}) {
  const { token } = getState();

  if (CONFIG.USE_MOCK) {
    await delay(250);
    return mock(path, method, body, token);
  }

  let res;
  try {
    res = await fetch(CONFIG.API_BASE + path, {
      method,
      headers: {
        ...(body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Check your connection.');
  }

  const data = res.status === 204 ? null : await res.json().catch(() => ({}));
  if (!res.ok) {
    if (res.status === 401 && token) window.dispatchEvent(new Event('auth:expired'));
    throw new ApiError(res.status, data?.message || `Request failed (${res.status})`);
  }
  return data;
}

// ---- Endpoints (keep in sync with the backend OpenAPI 3.1 spec) ----
export const api = {
  login: (creds) => request('/auth/login', { method: 'POST', body: creds }),
  register: (data) => request('/auth/register', { method: 'POST', body: data }),
  updateProfile: (data) => request('/me', { method: 'PATCH', body: data }),
  getStats: () => request('/stats'),
  getItems: () => request('/items'),
  createItem: (item) => request('/items', { method: 'POST', body: item }),
  deleteItem: (id) => request(`/items/${id}`, { method: 'DELETE' }),
  getUsers: () => request('/admin/users'),
  setUserRole: (id, role) => request(`/admin/users/${id}`, { method: 'PATCH', body: { role } }),
  getAudit: () => request('/admin/audit'),
  getReport: () => request('/admin/reports'),
  verifyCertificate: (id) => request(`/verify/${encodeURIComponent(id)}`),
  verifyLink: (token) => request(`/verify/link/${encodeURIComponent(token)}`),
  verifyUpload: (file) => {
    const body = new FormData();
    body.append('file', file);
    return request('/verify/upload', { method: 'POST', body });
  },
};

// ---- Mock backend (demo only: passwords in plain text, no real security) ----
const db = {
  users: [
    { id: 1, name: 'Admin', username: 'admin', email: 'admin@demo.com', password: 'admin123', role: 'admin' },
    { id: 2, name: 'Demo User', username: 'user', email: 'user@demo.com', password: 'user123', role: 'user' },
  ],
  items: [
    { id: 1, title: 'First record', status: 'open' },
    { id: 2, title: 'Second record', status: 'done' },
    { id: 3, title: 'Third record', status: 'open' },
  ],
  audit: [],
  nextId: 10,
};

const publicUser = ({ id, name, email, role }) => ({ id, name, email, role });
const log = (actor, action) => db.audit.unshift({ id: db.nextId++, at: new Date().toISOString(), actor, action });

function mock(path, method, body, token) {
  const me = db.users.find((u) => `mock-${u.id}` === token);
  const need = (role) => {
    if (!me) throw new ApiError(401, 'Please log in.');
    if (role && me.role !== role) throw new ApiError(403, 'You do not have permission.');
  };

  if (path.startsWith('/verify/') && (method === 'GET' || method === 'POST')) {
    return { result: 'NOT_FOUND', message: 'No matching certificate was found in the demo ledger.' };
  }

  if (path === '/auth/login' && method === 'POST') {
    const u = db.users.find((x) => (x.username === body.username || x.email === body.username) && x.password === body.password);
    if (!u) throw new ApiError(401, 'Invalid email or password.');
    log(u.email, 'login');
    return { token: `mock-${u.id}`, user: publicUser(u) };
  }
  if (path === '/auth/register' && method === 'POST') {
    if (db.users.some((x) => x.email === body.email)) throw new ApiError(409, 'That email is already registered.');
    const u = { id: db.nextId++, name: body.name, username: body.email.split('@')[0], email: body.email, password: body.password, role: 'user' };
    db.users.push(u);
    log(u.email, 'register');
    return { token: `mock-${u.id}`, user: publicUser(u) };
  }
  if (path === '/me' && method === 'PATCH') {
    need();
    me.name = body.name;
    return publicUser(me);
  }
  if (path === '/stats') {
    need();
    return {
      total: db.items.length,
      open: db.items.filter((i) => i.status === 'open').length,
      done: db.items.filter((i) => i.status === 'done').length,
    };
  }
  if (path === '/items' && method === 'GET') { need(); return [...db.items]; }
  if (path === '/items' && method === 'POST') {
    need();
    const item = { id: db.nextId++, title: body.title, status: body.status };
    db.items.unshift(item);
    log(me.email, `created record "${item.title}"`);
    return item;
  }
  const del = path.match(/^\/items\/(\d+)$/);
  if (del && method === 'DELETE') {
    need();
    db.items = db.items.filter((i) => i.id !== Number(del[1]));
    log(me.email, `deleted record ${del[1]}`);
    return null;
  }
  if (path === '/admin/users' && method === 'GET') { need('admin'); return db.users.map(publicUser); }
  const role = path.match(/^\/admin\/users\/(\d+)$/);
  if (role && method === 'PATCH') {
    need('admin');
    const u = db.users.find((x) => x.id === Number(role[1]));
    if (!u) throw new ApiError(404, 'User not found.');
    u.role = body.role;
    log(me.email, `set ${u.email} to ${u.role}`);
    return publicUser(u);
  }
  if (path === '/admin/audit') { need('admin'); return [...db.audit]; }
  if (path === '/admin/reports') {
    need('admin');
    return [
      { label: 'Open', value: db.items.filter((i) => i.status === 'open').length },
      { label: 'Done', value: db.items.filter((i) => i.status === 'done').length },
      { label: 'Users', value: db.users.length },
    ];
  }
  throw new ApiError(404, 'Not found.');
}
