import { setState, getState } from './store.js';
import { connect, disconnect } from './socket.js';

const KEY = 'session';

function persist() {
  try {
    const { token, user } = getState();
    sessionStorage.setItem(KEY, JSON.stringify({ token, user }));
  } catch { /* storage may be blocked */ }
}

export function startSession(token, user) {
  setState({ token, user, notifications: [], messages: [], unread: 0 });
  persist();
  connect();
}

export function updateUser(user) {
  setState({ user });
  persist();
}

export function endSession() {
  disconnect();
  setState({ token: null, user: null, notifications: [], messages: [], unread: 0, socketStatus: 'offline' });
  try { sessionStorage.removeItem(KEY); } catch { /* ignore */ }
}

export function restoreSession() {
  try {
    const raw = sessionStorage.getItem(KEY);
    if (!raw) return;
    const { token, user } = JSON.parse(raw);
    if (token && user) startSession(token, user);
  } catch { /* ignore corrupt data */ }
}
