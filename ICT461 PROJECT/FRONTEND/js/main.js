import { getState, subscribe } from './store.js';
import { restoreSession, endSession } from './session.js';
import { visibleRoutes, renderRoute, currentPath } from './router.js';
import { h, toast } from './components.js';

const nav = document.getElementById('nav');
const brand = document.getElementById('brand');

// Role-aware navigation. Re-rendered only when what it shows has changed,
// so keyboard focus is not lost when live events arrive.
let lastKey = '';
function renderNav() {
  const { user, unread } = getState();
  const current = currentPath();
  const key = `${user ? `${user.role}:${user.name}` : 'guest'}|${unread}|${current}`;
  if (key === lastKey) return;
  lastKey = key;

  brand.href = user ? '#/dashboard' : '#/';
  if (!user) {
    nav.replaceChildren(h('div', { class: 'nav-guest' },
      h('a', { href: '#/', 'aria-current': current === '/' ? 'page' : null }, 'Verify a credential'),
      h('a', { class: 'nav-sign-in', href: '#/login' }, 'Sign in')));
    return;
  }

  const routes = visibleRoutes(user).filter((route) => route.access === 'user' || route.access === 'admin');
  const links = routes.map((route) => h('li', {},
    h('a', { href: `#${route.path}`, 'aria-current': route.path === current ? 'page' : null },
      route.label,
      route.path === '/live' && unread > 0 ? h('span', { class: 'badge' }, String(unread)) : null)));
  const logout = h('button', {
    class: 'nav-sign-out', type: 'button',
    onclick: () => { endSession(); location.hash = '#/login'; toast('You have been logged out.'); },
  }, 'Sign out');

  nav.replaceChildren(h('div', { class: 'nav-user' },
    h('details', { class: 'workspace-menu' },
      h('summary', {}, 'Workspace'),
      h('ul', { class: 'workspace-links' },
        h('li', {}, h('a', { href: '#/verify' }, 'Verify a credential')),
        links)),
    h('span', { class: 'user-identity' },
      h('span', {}, user.name), h('span', { class: 'badge badge-neutral' }, user.role.toUpperCase())),
    logout));
}

window.addEventListener('hashchange', () => { renderRoute(); renderNav(); });
window.addEventListener('auth:expired', () => {
  endSession();
  toast('Your session expired. Please log in again.', 'error');
  location.hash = '#/login';
});
subscribe(renderNav);

restoreSession();
renderRoute();
renderNav();
