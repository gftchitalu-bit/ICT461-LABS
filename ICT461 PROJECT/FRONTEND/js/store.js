// Central state store: the single source of truth for the UI.
// Views read from it and the REST/socket clients write into it.

const listeners = new Set();

let state = {
  user: null,          // { id, name, email, role }
  token: null,
  notifications: [],   // pushed by WebSocket
  messages: [],        // chat messages
  unread: 0,
  socketStatus: 'offline', // offline | connecting | online | reconnecting
};

export function getState() {
  return state;
}

export function setState(patch) {
  const next = typeof patch === 'function' ? patch(state) : patch;
  state = { ...state, ...next };
  listeners.forEach((fn) => fn(state));
}

export function subscribe(fn) {
  listeners.add(fn);
  return () => listeners.delete(fn);
}
