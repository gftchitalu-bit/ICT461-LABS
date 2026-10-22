// Hash router with route guards.
// access: public (anyone) | guest (logged out only) | user (logged in) | admin
// NOTE: these guards only improve the UX. The backend must enforce RBAC on
// every endpoint, because anything in the browser can be bypassed.

import { CONFIG } from './config.js';
import { getState } from './store.js';
import { toast } from './components.js';
import * as v from './views.js';

export const ROUTES = [
  { path: '/', label: 'Verify credentials', access: 'public', view: v.landingView },
  { path: '/verify', label: 'Verify credentials', access: 'public', view: v.landingView },
  { path: '/login', label: 'Login', access: 'guest', view: v.loginView },
  { path: '/register', label: 'Register', access: 'guest', view: v.registerView },
  { path: '/dashboard', label: 'Dashboard', access: 'user', view: v.dashboardView },
  { path: '/items', label: 'Certificates', access: 'user', view: v.itemsView },
  { path: '/live', label: 'Activity', access: 'user', view: v.liveView },
  { path: '/profile', label: 'Profile', access: 'user', view: v.profileView },
  { path: '/admin/users', label: 'Users', access: 'admin', view: v.usersView },
  { path: '/admin/reports', label: 'Reports', access: 'admin', view: v.reportsView },
  { path: '/admin/audit', label: 'Audit log', access: 'admin', view: v.auditView },
];

export function visibleRoutes(user) {
  return ROUTES.filter((r) => {
    if (!user) return r.access === 'public' || r.access === 'guest';
    if (r.access === 'admin') return user.role === 'admin';
    return r.access === 'user';
  });
}

const go = (path) => { location.hash = `#${path}`; };
let cleanup = null;

export function currentPath() {
  return location.hash.slice(1) || '/';
}

export function renderRoute() {
  const { user } = getState();
  const path = currentPath();
  let route = ROUTES.find((r) => r.path === path);
  const linkMatch = path.match(/^\/verify\/link\/([^/]+)$/);
  const certificateMatch = path.match(/^\/verify\/([^/]+)$/);
  if (!route && linkMatch) {
    route = { path, label: 'Verify credential', access: 'public', view: () => v.verifyView(path) };
  } else if (!route && certificateMatch) {
    route = { path, label: 'Verify credential', access: 'public', view: () => v.verifyView(path) };
  }
  const main = document.getElementById('main');

  if (cleanup) { cleanup(); cleanup = null; }

  if (!route) {
    main.replaceChildren(v.notFoundView());
    return;
  }
  if (path === '/' && user) return go('/dashboard');
  if (route.access === 'guest' && user) return go('/dashboard');
  if ((route.access === 'user' || route.access === 'admin') && !user) {
    toast('Please log in first.', 'error');
    return go('/login');
  }
  if (route.access === 'admin' && user.role !== 'admin') {
    toast('You do not have permission to view that page.', 'error');
    return go('/dashboard');
  }

  const node = route.view();
  cleanup = node.cleanup || null;
  main.replaceChildren(node);
  document.title = `${route.label} · ${CONFIG.APP_NAME}`;
  const heading = node.querySelector('#page-title');
  if (heading) heading.focus();
}
