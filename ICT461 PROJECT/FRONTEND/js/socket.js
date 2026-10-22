// WebSocket client: connects over WSS, reconnects with exponential backoff
// and pushes incoming events into the store. Mock mode simulates a server.

import { CONFIG } from './config.js';
import { getState, setState } from './store.js';
import { toast } from './components.js';

let ws = null;
let retries = 0;
let reconnectTimer = null;
let mockTimer = null;
let closedByUser = false;

function setStatus(socketStatus) {
  setState({ socketStatus });
}

function handleIncoming(type, payload) {
  const at = new Date().toISOString();
  if (type === 'notification') {
    const onLivePage = location.hash === '#/live';
    setState((s) => ({
      notifications: [{ text: payload.text, at }, ...s.notifications].slice(0, 50),
      unread: onLivePage ? s.unread : s.unread + 1,
    }));
    toast(payload.text);
  } else if (type === 'message') {
    setState((s) => ({
      messages: [...s.messages, { from: payload.from, text: payload.text, at, mine: false }].slice(-100),
    }));
  }
}

export function connect() {
  disconnect();
  closedByUser = false;
  setStatus('connecting');

  if (CONFIG.USE_MOCK) {
    setTimeout(() => setStatus('online'), 400);
    let n = 0;
    mockTimer = setInterval(() => {
      n += 1;
      handleIncoming('notification', { text: `Server update #${n}` });
    }, 12000);
    return;
  }

  const { token } = getState();
  ws = new WebSocket(`${CONFIG.WS_URL}?token=${encodeURIComponent(token)}`);
  ws.onopen = () => {
    retries = 0;
    setStatus('online');
  };
  ws.onmessage = (event) => {
    try {
      const { type, payload } = JSON.parse(event.data);
      handleIncoming(type, payload);
    } catch {
      /* ignore malformed frames */
    }
  };
  ws.onclose = () => {
    if (closedByUser) return;
    setStatus('reconnecting');
    const wait = Math.min(30000, 1000 * 2 ** retries);
    retries += 1;
    reconnectTimer = setTimeout(connect, wait);
  };
}

export function disconnect() {
  closedByUser = true;
  clearTimeout(reconnectTimer);
  clearInterval(mockTimer);
  if (ws) {
    ws.close();
    ws = null;
  }
  setStatus('offline');
}

export function sendMessage(text) {
  const { user, socketStatus } = getState();
  if (socketStatus !== 'online') {
    toast('Not connected. Message not sent.', 'error');
    return;
  }
  setState((s) => ({
    messages: [...s.messages, { from: user.name, text, at: new Date().toISOString(), mine: true }],
  }));
  if (CONFIG.USE_MOCK) {
    setTimeout(() => handleIncoming('message', { from: 'Support', text: `Got it: "${text}"` }), 800);
  } else if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'message', payload: { text } }));
  }
}
