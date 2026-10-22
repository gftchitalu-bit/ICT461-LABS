// Page views (one function per tab). Each returns a DOM node.
// A view may set node.cleanup = fn to release subscriptions when leaving.

import { api } from './api.js';
import { CONFIG } from './config.js';
import { getState, setState, subscribe } from './store.js';
import { startSession, updateUser } from './session.js';
import { sendMessage } from './socket.js';
import { h, page, field, table, toast, validateForm, formError } from './components.js';

const formData = (form) => Object.fromEntries(new FormData(form));

function authPage(title, description, form, footnote = null) {
  return h('section', { class: 'auth-page page' },
    h('header', { class: 'auth-heading' },
      h('p', { class: 'label-eyebrow' }, 'Mulungushi University · CertChain'),
      h('h1', { id: 'page-title', tabindex: '-1' }, title),
      h('p', {}, description),
    ),
    form,
    footnote,
  );
}

// ---------- Public tabs ----------

export function landingView() {
  return verifyView('/');
}

function verificationResult(result) {
  if (!result) return null;
  const variants = {
    VERIFIED: ['verified', 'Verified'],
    TAMPERED: ['revoked', 'Tampered / integrity failure'],
    REVOKED: ['revoked', 'Revoked'],
    NOT_FOUND: ['neutral', 'Not found'],
    EXPIRED_LINK: ['neutral', 'Link expired'],
  };
  const [variant, label] = variants[result.result] || variants.NOT_FOUND;
  const details = [
    ['Owner', result.studentName],
    ['Student number', result.studentNumber],
    ['Certificate', result.programName],
    ['Issued', result.issueDate],
    ['Certificate ID', result.certificateUid],
    ['Blockchain transaction', result.transactionId],
    ['Ledger block hash', result.blockHash],
    ['Ledger integrity', result.chainIntact == null ? '' : result.chainIntact ? 'Intact' : 'Failed - do not trust'],
  ].filter(([, value]) => value);

  return h('section', { class: `card verify-result verify-result-${variant}` },
    h('span', { class: `badge badge-${variant}` }, label),
    h('p', { class: 'verify-message' }, result.message || ''),
    details.length ? h('dl', { class: 'credential-details' }, details.map(([labelText, value]) =>
      h('div', {}, h('dt', { class: 'label-eyebrow' }, labelText), h('dd', {}, value)))) : null,
  );
}

export function verifyView(path = '/') {
  const autoMatch = path.match(/^\/verify\/(?:link\/)?([^/]+)$/);
  const linkMatch = path.match(/^\/verify\/link\/([^/]+)$/);
  const response = h('div', { class: 'verify-response', 'aria-live': 'polite' });

  const showResult = (result) => response.replaceChildren(verificationResult(result));
  const showError = (message) => response.replaceChildren(h('p', { class: 'verify-error', role: 'alert' }, message));
  const verify = async (request) => {
    response.replaceChildren(h('p', { class: 'verify-loading', role: 'status' }, 'Checking the ledger...'));
    try {
      showResult(await request());
    } catch (error) {
      showError(error.message || 'Could not reach the verification service.');
    }
  };

  const manualId = h('input', {
    id: 'certificate-id', name: 'certificateId', type: 'text', required: true,
    placeholder: 'Enter certificate ID', autocomplete: 'off',
  });
  const manualForm = h('form', { class: 'verify-form', onsubmit: (event) => {
    event.preventDefault();
    const id = manualId.value.trim();
    if (id) verify(() => api.verifyCertificate(id));
  } },
    h('label', { class: 'label-eyebrow', for: 'certificate-id' }, 'Enter certificate ID'),
    h('div', { class: 'verify-control-row' }, manualId,
      h('button', { class: 'btn', type: 'submit' }, 'Verify')),
  );

  const certificateFile = h('input', {
    id: 'certificate-file', name: 'certificate', type: 'file', accept: 'application/pdf,.pdf',
    onchange: (event) => { uploadButton.disabled = !event.target.files?.length; },
  });
  const uploadButton = h('button', { class: 'btn btn-secondary', type: 'submit', disabled: true }, 'Verify PDF');
  const uploadForm = h('form', { class: 'verify-form', onsubmit: (event) => {
    event.preventDefault();
    const file = certificateFile.files?.[0];
    if (file) verify(() => api.verifyUpload(file));
  } },
    h('label', { class: 'label-eyebrow', for: 'certificate-file' }, 'Or upload a certificate PDF'),
    h('div', { class: 'verify-control-row' }, certificateFile, uploadButton),
  );

  const content = h('section', { class: 'verify-portal page' },
    h('header', { class: 'portal-heading' },
      h('p', { class: 'label-eyebrow' }, 'Mulungushi University · CertChain'),
      h('h1', { id: 'page-title', tabindex: '-1' }, 'Verify a Credential'),
      h('p', { class: 'portal-description' }, 'Confirm the authenticity of a certificate against the permissioned ledger.'),
    ),
    !autoMatch ? h('div', { class: 'card verify-panel' },
      manualForm,
      h('div', { class: 'verify-divider', role: 'separator' }),
      uploadForm,
      h('p', { class: 'hint verify-tip' }, 'Scanning the QR code printed on a certificate opens its verification page directly.'),
    ) : null,
    response,
  );

  if (autoMatch) {
    const value = decodeURIComponent(autoMatch[1]);
    verify(() => linkMatch ? api.verifyLink(value) : api.verifyCertificate(value));
  }
  return content;
}

export function loginView() {
  const form = h('form', { class: 'card', novalidate: true, onsubmit: async (e) => {
    e.preventDefault();
    formError(form, '');
    if (!validateForm(form)) return;
    const btn = form.querySelector('button');
    btn.disabled = true;
    try {
      const { token, user } = await api.login({
        username: formData(form).username,
        password: formData(form).password,
      });
      startSession(token, user);
      location.hash = '#/dashboard';
    } catch (err) {
      formError(form, err.message);
    } finally {
      btn.disabled = false;
    }
  } },
    field({ id: 'username', label: 'Username', required: true, autocomplete: 'username' }),
    field({ id: 'password', label: 'Password', type: 'password', required: true, autocomplete: 'current-password' }),
    h('p', { class: 'form-error', role: 'alert' }),
    h('button', { class: 'btn auth-submit', type: 'submit' }, 'Sign in'),
  );
  return authPage('Sign in to CertChain', 'Registrar, student, and employer portal access.', form,
    h('p', { class: 'auth-footnote' }, 'Just need to verify a credential? ',
      h('a', { href: '#/' }, 'Use the public verification portal'), ' - no account required.'));
}

export function registerView() {
  const form = h('form', { class: 'card', novalidate: true, onsubmit: async (e) => {
    e.preventDefault();
    formError(form, '');
    if (!validateForm(form)) return;
    const btn = form.querySelector('button');
    btn.disabled = true;
    try {
      const { token, user } = await api.register(formData(form));
      startSession(token, user);
      toast('Account created.', 'success');
      location.hash = '#/dashboard';
    } catch (err) {
      formError(form, err.message);
    } finally {
      btn.disabled = false;
    }
  } },
    field({ id: 'name', label: 'Full name', required: true, minlength: 2, maxlength: 60, autocomplete: 'name' }),
    field({ id: 'email', label: 'Email', type: 'email', required: true, autocomplete: 'email' }),
    field({ id: 'password', label: 'Password', type: 'password', required: true, minlength: 8, autocomplete: 'new-password', hint: 'At least 8 characters.' }),
    h('p', { class: 'form-error', role: 'alert' }),
    h('button', { class: 'btn auth-submit', type: 'submit' }, 'Create account'),
  );
  return authPage('Create a CertChain account', 'Request access to the credential platform.', form);
}

// ---------- Logged-in tabs ----------

export function dashboardView() {
  const { user } = getState();
  const cards = h('div', { class: 'grid', 'aria-busy': 'true' });
  const card = (label, value) => h('div', { class: 'card stat-card' },
    h('p', { class: 'stat' }, String(value)), h('p', { class: 'stat-label' }, label));

  api.getStats().then((s) => {
    cards.replaceChildren(
      card('Total records', s.total), card('Open', s.open), card('Done', s.done),
      card('Unread notifications', getState().unread));
    cards.removeAttribute('aria-busy');
  }).catch((e) => toast(e.message, 'error'));

  return page(user.role === 'admin' ? 'Administration' : 'Credential overview',
    h('p', { class: 'lead' }, `Welcome, ${user.name}. Review credential activity and account records.`),
    cards,
    h('p', { class: 'dashboard-action' }, h('a', { class: 'btn', href: '#/items' }, 'Open certificate register')));
}

export function itemsView() {
  let items = [];
  let query = '';
  let status = 'all';
  const list = h('div', { 'aria-live': 'polite' });

  const draw = () => {
    const rows = items.filter((i) =>
      (status === 'all' || i.status === status) && i.title.toLowerCase().includes(query.toLowerCase()));
    list.replaceChildren(table({
      caption: 'Records',
      rows,
      columns: [
        { label: 'Title', key: 'title' },
        { label: 'Status', key: 'status' },
        { label: 'Actions', render: (r) => h('button', {
          class: 'btn btn-small btn-danger', type: 'button', 'aria-label': `Delete ${r.title}`,
          onclick: async () => {
            try {
              await api.deleteItem(r.id);
              items = items.filter((i) => i.id !== r.id);
              draw();
              toast('Record deleted.', 'success');
            } catch (e) { toast(e.message, 'error'); }
          },
        }, 'Delete') },
      ],
    }));
  };

  const addForm = h('form', { class: 'card inline-form', novalidate: true, onsubmit: async (e) => {
    e.preventDefault();
    if (!validateForm(addForm)) return;
    try {
      const created = await api.createItem(formData(addForm));
      items = [created, ...items];
      addForm.reset();
      draw();
      toast('Record added.', 'success');
    } catch (err) { toast(err.message, 'error'); }
  } },
    field({ id: 'title', label: 'Title', required: true, minlength: 2, maxlength: 80 }),
    field({ id: 'status', label: 'Status', type: 'select', options: ['open', 'done'], value: 'open' }),
    h('button', { class: 'btn', type: 'submit' }, 'Add record'),
  );

  const filters = h('div', { class: 'card inline-form' },
    h('div', { class: 'field' }, h('label', { for: 'q' }, 'Search'),
      h('input', { id: 'q', type: 'search', oninput: (e) => { query = e.target.value; draw(); } })),
    h('div', { class: 'field' }, h('label', { for: 'f' }, 'Status'),
      h('select', { id: 'f', onchange: (e) => { status = e.target.value; draw(); } },
        ['all', 'open', 'done'].map((o) => h('option', { value: o }, o)))),
  );

  api.getItems().then((data) => { items = data; draw(); }).catch((e) => toast(e.message, 'error'));

  return page('Certificate register',
    h('div', { class: 'section-heading' }, h('h2', {}, 'Issue a certificate')),
    addForm,
    h('div', { class: 'section-heading' }, h('h2', {}, 'Issued credentials')),
    filters, list);
}

export function liveView() {
  const status = h('p', { class: 'status', role: 'status' });
  const notes = h('ul', { class: 'feed' });
  const msgs = h('ul', { class: 'feed chat', 'aria-live': 'polite' });

  const draw = (s) => {
    status.textContent = `Connection: ${s.socketStatus}`;
    notes.replaceChildren(...s.notifications.slice(0, 15).map((n) =>
      h('li', {}, h('time', { datetime: n.at }, new Date(n.at).toLocaleTimeString()), ' ', n.text)));
    msgs.replaceChildren(...s.messages.map((m) =>
      h('li', { class: m.mine ? 'mine' : '' }, h('strong', {}, `${m.from}: `), m.text)));
    msgs.scrollTop = msgs.scrollHeight;
  };

  const form = h('form', { class: 'inline-form', novalidate: true, onsubmit: (e) => {
    e.preventDefault();
    if (!validateForm(form)) return;
    sendMessage(form.elements.text.value.trim());
    form.reset();
  } },
    field({ id: 'text', label: 'Message', required: true, maxlength: 300 }),
    h('button', { class: 'btn', type: 'submit' }, 'Send'),
  );

  setState({ unread: 0 });
  const node = page('Live feed', status,
    h('div', { class: 'two-col' },
      h('div', { class: 'card' }, h('h2', {}, 'Notifications'), notes),
      h('div', { class: 'card' }, h('h2', {}, 'Chat'), msgs, form)));
  const unsubscribe = subscribe((s) => { draw(s); if (s.unread) setState({ unread: 0 }); });
  node.cleanup = unsubscribe;
  draw(getState());
  return node;
}

export function profileView() {
  const { user } = getState();
  const form = h('form', { class: 'card', novalidate: true, onsubmit: async (e) => {
    e.preventDefault();
    if (!validateForm(form)) return;
    try {
      const updated = await api.updateProfile({ name: formData(form).name });
      updateUser(updated);
      toast('Profile saved.', 'success');
    } catch (err) { formError(form, err.message); }
  } },
    field({ id: 'name', label: 'Full name', required: true, minlength: 2, maxlength: 60, value: user.name }),
    h('p', {}, `Email: ${user.email}`),
    h('p', {}, `Role: ${user.role}`),
    h('p', { class: 'form-error', role: 'alert' }),
    h('button', { class: 'btn', type: 'submit' }, 'Save changes'),
  );
  return page('Profile', form);
}

// ---------- Admin-only tabs ----------

export function usersView() {
  const box = h('div');
  const me = getState().user;
  api.getUsers().then((users) => {
    box.replaceChildren(table({
      caption: 'All users',
      rows: users,
      columns: [
        { label: 'Name', key: 'name' },
        { label: 'Email', key: 'email' },
        { label: 'Role', render: (u) => h('select', {
          'aria-label': `Role for ${u.name}`, disabled: u.id === me.id,
          onchange: async (e) => {
            try { await api.setUserRole(u.id, e.target.value); toast('Role updated.', 'success'); }
            catch (err) { toast(err.message, 'error'); e.target.value = u.role; }
          },
        }, ['user', 'admin'].map((r) => h('option', { value: r, selected: r === u.role }, r))) },
      ],
    }));
  }).catch((e) => toast(e.message, 'error'));
  return page('User management', box);
}

export function reportsView() {
  const box = h('div', { class: 'card' });
  api.getReport().then((rows) => {
    const max = Math.max(1, ...rows.map((r) => r.value));
    box.replaceChildren(...rows.map((r) => {
      const fill = h('div', { class: 'bar-fill' });
      fill.style.width = `${(r.value / max) * 100}%`;
      return h('div', { class: 'bar-row' }, h('span', {}, r.label), h('div', { class: 'bar-track', 'aria-hidden': 'true' }, fill), h('strong', {}, String(r.value)));
    }));
  }).catch((e) => toast(e.message, 'error'));
  return page('Reports', box);
}

export function auditView() {
  const box = h('div');
  api.getAudit().then((rows) => {
    box.replaceChildren(table({
      caption: 'Audit log',
      rows,
      empty: 'No activity recorded yet.',
      columns: [
        { label: 'Time', render: (r) => new Date(r.at).toLocaleString() },
        { label: 'Who', key: 'actor' },
        { label: 'Action', key: 'action' },
      ],
    }));
  }).catch((e) => toast(e.message, 'error'));
  return page('Audit log', box);
}

export function notFoundView() {
  return page('Page not found', h('p', {}, h('a', { href: '#/' }, 'Go to the home page')));
}
