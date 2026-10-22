// Reusable UI building blocks. Everything is built with DOM methods and
// textContent (never innerHTML) so user data cannot inject markup (XSS).

export function h(tag, attrs = {}, ...children) {
  const node = document.createElement(tag);
  for (const [key, value] of Object.entries(attrs || {})) {
    if (value === false || value == null) continue;
    if (key.startsWith('on') && typeof value === 'function') {
      node.addEventListener(key.slice(2).toLowerCase(), value);
    } else if (key === 'class') {
      node.className = value;
    } else if (value === true) {
      node.setAttribute(key, '');
    } else {
      node.setAttribute(key, value);
    }
  }
  for (const child of children.flat()) {
    if (child == null || child === false) continue;
    node.append(child.nodeType ? child : document.createTextNode(String(child)));
  }
  return node;
}

export function toast(message, type = 'info') {
  const box = document.getElementById('toasts');
  if (!box) return;
  const item = h('div', { class: `toast ${type}` }, message);
  box.append(item);
  setTimeout(() => item.remove(), 4500);
}

export function field({ id, label, type = 'text', required = false, value = '', hint, autocomplete, options, minlength, maxlength }) {
  const describedBy = hint ? `${id}-hint ${id}-err` : `${id}-err`;
  const control = type === 'select'
    ? h('select', { id, name: id, required, 'aria-describedby': describedBy },
        options.map((o) => h('option', { value: o, selected: o === value }, o)))
    : h('input', { id, name: id, type, required, value, autocomplete, minlength, maxlength, 'aria-describedby': describedBy });

  return h('div', { class: 'field' },
    h('label', { for: id }, label, required ? h('span', { class: 'req', 'aria-hidden': 'true' }, ' *') : null),
    control,
    hint ? h('p', { id: `${id}-hint`, class: 'hint' }, hint) : null,
    h('p', { id: `${id}-err`, class: 'error' }),
  );
}

// Client-side validation with accessible inline errors.
// The backend must validate again: this is for usability, not security.
export function validateForm(form) {
  let ok = true;
  form.querySelectorAll('input, select').forEach((control) => {
    const err = form.querySelector(`#${control.id}-err`);
    if (!control.checkValidity()) {
      ok = false;
      control.setAttribute('aria-invalid', 'true');
      if (err) err.textContent = control.validationMessage;
    } else {
      control.removeAttribute('aria-invalid');
      if (err) err.textContent = '';
    }
  });
  const first = form.querySelector('[aria-invalid="true"]');
  if (first) first.focus();
  return ok;
}

export function formError(form, message) {
  const box = form.querySelector('.form-error');
  if (box) box.textContent = message || '';
}

export function table({ caption, columns, rows, empty = 'No records yet.' }) {
  if (!rows.length) return h('p', { class: 'empty' }, empty);
  return h('div', { class: 'table-wrap', role: 'region', 'aria-label': caption, tabindex: '0' },
    h('table', {},
      h('caption', { class: 'sr-only' }, caption),
      h('thead', {}, h('tr', {}, columns.map((c) => h('th', { scope: 'col' }, c.label)))),
      h('tbody', {}, rows.map((row) =>
        h('tr', {}, columns.map((c) => h('td', {}, c.render ? c.render(row) : row[c.key]))))),
    ));
}

export function page(title, ...children) {
  return h('section', { class: 'page' }, h('h1', { id: 'page-title', tabindex: '-1' }, title), ...children);
}
