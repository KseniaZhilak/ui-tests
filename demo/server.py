#!/usr/bin/env python3
"""
Демо-стенд (SUT) для локального запуска UI-тестов.

Реализует ровно тот контракт data-testid, который описан в README:
страницы /orders и /users, фильтры, таблица с data-render-id, пагинация,
а также backdoor-API подготовки данных (/api/test-data/orders).

Запуск: python3 demo/server.py [порт]   (по умолчанию 8080)
Зависимостей нет — только стандартная библиотека Python 3.9+.
"""
import json
import random
import sys
import threading
from datetime import date, timedelta
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

LOCK = threading.RLock()
ORDERS = []
USERS = []

STATUSES = ["NEW", "PAID", "SHIPPED", "CANCELLED"]
ROLES = ["ADMIN", "MANAGER", "VIEWER"]


def seed_data():
    random.seed(20261113)
    today = date.today()
    for i in range(1, 13):
        ORDERS.append({
            "number": "ORD-BASE-%03d" % i,
            "customer": "BASELINE-%s" % random.choice(["ACME", "GLOBEX", "UMBRELLA"]),
            "status": STATUSES[i % len(STATUSES)],
            "total": round(100 + i * 137.5, 2),
            "createdAt": (today - timedelta(days=i * 3)).isoformat(),
        })
    for i in range(1, 15):
        USERS.append({
            "email": "user%02d@example.com" % i,
            "fullName": "Пользователь %02d" % i,
            "role": ROLES[i % len(ROLES)],
            "active": i % 4 != 0,
            "lastLogin": (today - timedelta(days=i)).isoformat(),
        })


def order_view(o):
    return {
        "number": o["number"],
        "customer": o["customer"],
        "status": o["status"].lower(),
        "total": o["total"],
        "createdAt": o["createdAt"],
    }


def user_view(u):
    return {
        "email": u["email"],
        "fullName": u["fullName"],
        "role": u["role"].lower(),
        "active": u["active"],
        "lastLogin": u["lastLogin"],
    }


def param(q, name, default=""):
    return q.get(name, [default])[0]


def paginate(rows, q):
    size = int(param(q, "size", "25") or 25)
    page = int(param(q, "page", "1") or 1)
    total = len(rows)
    start = (page - 1) * size
    return {"rows": rows[start:start + size], "total": total}


def sort_rows(rows, q, keys):
    column = param(q, "sort")
    if not column or column not in keys:
        return rows
    field = keys[column]
    return sorted(rows, key=lambda r: r[field], reverse=param(q, "dir") == "desc")


def query_orders(q):
    with LOCK:
        rows = [order_view(o) for o in ORDERS]
    customer = param(q, "customer").strip().lower()
    if customer:
        rows = [r for r in rows if customer in r["customer"].lower()]
    statuses = [s for s in param(q, "status").split(",") if s]
    if statuses:
        rows = [r for r in rows if r["status"] in statuses]
    date_from = param(q, "createdFrom")
    if date_from:
        rows = [r for r in rows if r["createdAt"] >= date_from]
    date_to = param(q, "createdTo")
    if date_to:
        rows = [r for r in rows if r["createdAt"] <= date_to]
    rows = sort_rows(rows, q, {
        "order-number": "number", "customer": "customer", "status": "status",
        "total": "total", "created-at": "createdAt",
    })
    return paginate(rows, q)


def query_users(q):
    with LOCK:
        rows = [user_view(u) for u in USERS]
    email = param(q, "email").strip().lower()
    if email:
        rows = [r for r in rows if email in r["email"].lower()]
    roles = [r for r in param(q, "role").split(",") if r]
    if roles:
        rows = [r for r in rows if r["role"] in roles]
    if param(q, "onlyActive") == "true":
        rows = [r for r in rows if r["active"]]
    rows = sort_rows(rows, q, {
        "email": "email", "full-name": "fullName", "role": "role",
        "active": "active", "last-login": "lastLogin",
    })
    return paginate(rows, q)


PAGE_HTML = r"""<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="utf-8">
<title>Реестры — демо-стенд</title>
<style>
  body { font-family: system-ui, sans-serif; margin: 24px; color: #1a1a1a; }
  nav a { margin-right: 16px; }
  .filter-bar { display: flex; gap: 16px; align-items: flex-start; flex-wrap: wrap;
                padding: 12px; border: 1px solid #ddd; border-radius: 8px; }
  .filter { position: relative; display: flex; flex-direction: column; gap: 4px; }
  .filter .label { font-size: 12px; color: #666; }
  .options { position: absolute; top: 100%; left: 0; z-index: 10; background: #fff;
             border: 1px solid #bbb; border-radius: 6px; padding: 4px; display: flex; flex-direction: column; }
  .chip { background: #eef; border-radius: 10px; padding: 1px 8px; margin-right: 4px; font-size: 12px; }
  table { border-collapse: collapse; width: 100%; margin-top: 16px; }
  th, td { border-bottom: 1px solid #eee; padding: 6px 10px; text-align: left; font-size: 14px; }
  th button { background: none; border: none; cursor: pointer; font-weight: 600; padding: 0; }
  .loader { padding: 12px; color: #888; }
  .empty { padding: 24px; color: #888; }
  .pagination { display: flex; gap: 12px; align-items: center; margin-top: 16px; flex-wrap: wrap; }
  .pagination button[aria-current="page"] { font-weight: 700; text-decoration: underline; }
</style>
</head>
<body>
<nav><a href="/orders">Orders</a><a href="/users">Users</a></nav>
<h1 id="title"></h1>
<div data-testid="page-__PAGE__">
  <div data-testid="filter-bar" class="filter-bar"></div>
  <div data-testid="__PAGE__-table" data-render-id="0">
    <div data-testid="table-loader" class="loader" style="display:none">Загрузка…</div>
    <table><thead><tr id="head-row"></tr></thead><tbody id="body"></tbody></table>
    <div data-testid="table-empty" class="empty" style="display:none">Нет данных по заданным условиям</div>
  </div>
  <div data-testid="pagination" class="pagination">
    <span data-testid="page-summary"></span>
    <button type="button" data-testid="page-prev">&larr;</button>
    <span id="page-numbers"></span>
    <button type="button" data-testid="page-next">&rarr;</button>
    <select data-testid="page-size">
      <option value="10">10</option>
      <option value="25" selected>25</option>
      <option value="50">50</option>
      <option value="100">100</option>
    </select>
  </div>
</div>
<script>
const PAGE = "__PAGE__";
const money = v => v.toLocaleString('ru-RU', {minimumFractionDigits: 2, maximumFractionDigits: 2}) + ' \u20BD';
const dmy = iso => { const p = iso.split('-'); return p[2] + '.' + p[1] + '.' + p[0]; };
const iso = dmyText => { const p = dmyText.trim().split('.'); return p.length === 3 ? p[2] + '-' + p[1] + '-' + p[0] : ''; };
const ORDER_STATUS = {new: 'Новый', paid: 'Оплачен', shipped: 'Отгружен', cancelled: 'Отменён'};
const USER_ROLE = {admin: 'Администратор', manager: 'Менеджер', viewer: 'Наблюдатель'};

const CFG = {
  orders: {
    title: 'Заказы', api: '/api/orders',
    filters: [
      {key: 'customer', param: 'customer', type: 'text', label: 'Клиент'},
      {key: 'status', param: 'status', type: 'select', label: 'Статус',
       options: Object.keys(ORDER_STATUS).map(id => [id, ORDER_STATUS[id]])},
      {key: 'created-at', param: 'created', type: 'daterange', label: 'Создан'}
    ],
    columns: [
      {id: 'order-number', title: 'Номер', text: r => r.number},
      {id: 'customer', title: 'Клиент', text: r => r.customer},
      {id: 'status', title: 'Статус', text: r => ORDER_STATUS[r.status], value: r => r.status},
      {id: 'total', title: 'Сумма', text: r => money(r.total)},
      {id: 'created-at', title: 'Создан', text: r => dmy(r.createdAt)}
    ],
    rowId: r => r.number
  },
  users: {
    title: 'Пользователи', api: '/api/users',
    filters: [
      {key: 'email', param: 'email', type: 'text', label: 'E-mail'},
      {key: 'role', param: 'role', type: 'select', label: 'Роль',
       options: Object.keys(USER_ROLE).map(id => [id, USER_ROLE[id]])},
      {key: 'only-active', param: 'onlyActive', type: 'toggle', label: 'Только активные'}
    ],
    columns: [
      {id: 'email', title: 'E-mail', text: r => r.email},
      {id: 'full-name', title: 'ФИО', text: r => r.fullName},
      {id: 'role', title: 'Роль', text: r => USER_ROLE[r.role], value: r => r.role},
      {id: 'active', title: 'Активен', text: r => r.active ? 'Да' : 'Нет', value: r => String(r.active)},
      {id: 'last-login', title: 'Последний вход', text: r => dmy(r.lastLogin)}
    ],
    rowId: r => r.email
  }
};

const cfg = CFG[PAGE];
const state = {page: 1, size: 25, sort: '', dir: '', renderId: 0, total: 0};
const tableRoot = document.querySelector('[data-testid="' + PAGE + '-table"]');

function el(html) {
  const t = document.createElement('template');
  t.innerHTML = html.trim();
  return t.content.firstElementChild;
}
function box(key) { return document.querySelector('[data-testid="filter-' + key + '"]'); }

function buildFilters() {
  const bar = document.querySelector('[data-testid="filter-bar"]');
  cfg.filters.forEach(f => {
    const wrap = el('<div class="filter" data-testid="filter-' + f.key + '"></div>');
    wrap.appendChild(el('<span class="label">' + f.label + '</span>'));
    if (f.type === 'text') {
      wrap.appendChild(el('<input type="text" data-testid="filter-value">'));
    } else if (f.type === 'toggle') {
      wrap.appendChild(el('<input type="checkbox" data-testid="filter-value">'));
    } else if (f.type === 'daterange') {
      wrap.appendChild(el('<input type="text" placeholder="дд.мм.гггг" data-testid="filter-date-from">'));
      wrap.appendChild(el('<input type="text" placeholder="дд.мм.гггг" data-testid="filter-date-to">'));
    } else if (f.type === 'select') {
      const toggle = el('<button type="button" data-testid="filter-toggle">выбрать…</button>');
      const chips = el('<span class="chips"></span>');
      const clear = el('<button type="button" data-testid="filter-clear" style="display:none">× сбросить</button>');
      const opts = el('<div class="options" data-testid="filter-options" style="display:none"></div>');
      f.options.forEach(pair => {
        const opt = el('<button type="button" data-testid="option-' + pair[0] + '">' + pair[1] + '</button>');
        opt.addEventListener('click', () => toggleOption(wrap, pair[0], pair[1]));
        opts.appendChild(opt);
      });
      toggle.addEventListener('click', () => {
        opts.style.display = opts.style.display === 'none' ? 'flex' : 'none';
      });
      clear.addEventListener('click', () => clearChips(wrap));
      wrap.appendChild(toggle);
      wrap.appendChild(chips);
      wrap.appendChild(clear);
      wrap.appendChild(opts);
    }
    bar.appendChild(wrap);
  });
  const apply = el('<button type="button" data-testid="filters-apply">Применить</button>');
  const reset = el('<button type="button" data-testid="filters-reset">Сбросить</button>');
  apply.addEventListener('click', () => { state.page = 1; load(); });
  reset.addEventListener('click', () => { resetFilters(); state.page = 1; load(); });
  bar.appendChild(apply);
  bar.appendChild(reset);
}

function toggleOption(wrap, id, label) {
  const chips = wrap.querySelector('.chips');
  const existing = chips.querySelector('[data-option="' + id + '"]');
  if (existing) {
    existing.remove();
  } else {
    chips.appendChild(el('<span class="chip" data-testid="selected-option" data-option="' + id + '">' + label + '</span>'));
  }
  syncClear(wrap);
}
function clearChips(wrap) {
  wrap.querySelector('.chips').innerHTML = '';
  syncClear(wrap);
}
function syncClear(wrap) {
  const has = wrap.querySelectorAll('[data-testid="selected-option"]').length > 0;
  wrap.querySelector('[data-testid="filter-clear"]').style.display = has ? 'inline-block' : 'none';
}

function resetFilters() {
  cfg.filters.forEach(f => {
    const wrap = box(f.key);
    if (f.type === 'text') { wrap.querySelector('[data-testid="filter-value"]').value = ''; }
    if (f.type === 'toggle') { wrap.querySelector('[data-testid="filter-value"]').checked = false; }
    if (f.type === 'daterange') {
      wrap.querySelector('[data-testid="filter-date-from"]').value = '';
      wrap.querySelector('[data-testid="filter-date-to"]').value = '';
    }
    if (f.type === 'select') { clearChips(wrap); }
  });
}

function query() {
  const p = new URLSearchParams();
  cfg.filters.forEach(f => {
    const wrap = box(f.key);
    if (f.type === 'text') {
      const v = wrap.querySelector('[data-testid="filter-value"]').value.trim();
      if (v) { p.set(f.param, v); }
    } else if (f.type === 'toggle') {
      if (wrap.querySelector('[data-testid="filter-value"]').checked) { p.set(f.param, 'true'); }
    } else if (f.type === 'daterange') {
      const from = iso(wrap.querySelector('[data-testid="filter-date-from"]').value);
      const to = iso(wrap.querySelector('[data-testid="filter-date-to"]').value);
      if (from) { p.set(f.param + 'From', from); }
      if (to) { p.set(f.param + 'To', to); }
    } else if (f.type === 'select') {
      const ids = Array.prototype.map.call(
        wrap.querySelectorAll('[data-testid="selected-option"]'), c => c.getAttribute('data-option'));
      if (ids.length) { p.set(f.param, ids.join(',')); }
    }
  });
  p.set('page', String(state.page));
  p.set('size', String(state.size));
  if (state.sort) { p.set('sort', state.sort); p.set('dir', state.dir); }
  return p;
}

function ariaOf(columnId) {
  if (state.sort !== columnId) { return 'none'; }
  return state.dir === 'asc' ? 'ascending' : 'descending';
}

function renderHead() {
  const head = document.getElementById('head-row');
  head.innerHTML = '';
  cfg.columns.forEach(c => {
    const th = el('<th></th>');
    const btn = el('<button type="button" data-testid="sort-' + c.id + '" aria-sort="' + ariaOf(c.id) + '">' + c.title + '</button>');
    btn.addEventListener('click', () => {
      if (state.sort !== c.id) { state.sort = c.id; state.dir = 'asc'; }
      else if (state.dir === 'asc') { state.dir = 'desc'; }
      else { state.sort = ''; state.dir = ''; }
      load();
    });
    th.appendChild(btn);
    head.appendChild(th);
  });
}

function renderRows(rows) {
  const body = document.getElementById('body');
  body.innerHTML = '';
  rows.forEach(r => {
    const tr = el('<tr data-testid="table-row" data-row-id="' + cfg.rowId(r) + '"></tr>');
    cfg.columns.forEach(c => {
      const value = c.value ? ' data-value="' + c.value(r) + '"' : '';
      tr.appendChild(el('<td data-testid="cell-' + c.id + '"' + value + '>' + c.text(r) + '</td>'));
    });
    body.appendChild(tr);
  });
  document.querySelector('[data-testid="table-empty"]').style.display = rows.length ? 'none' : 'block';
}

function renderPagination(total) {
  const pages = Math.max(1, Math.ceil(total / state.size));
  if (state.page > pages) { state.page = pages; }
  const from = total === 0 ? 0 : (state.page - 1) * state.size + 1;
  const to = total === 0 ? 0 : Math.min(state.page * state.size, total);
  document.querySelector('[data-testid="page-summary"]').textContent = from + '\u2013' + to + ' из ' + total;
  const numbers = document.getElementById('page-numbers');
  numbers.innerHTML = '';
  if (total > 0) {
    for (let i = 1; i <= pages; i++) {
      const btn = el('<button type="button" data-testid="page-number" data-page="' + i + '">' + i + '</button>');
      if (i === state.page) { btn.setAttribute('aria-current', 'page'); }
      btn.addEventListener('click', () => { state.page = i; load(); });
      numbers.appendChild(btn);
    }
  }
  document.querySelector('[data-testid="page-prev"]').disabled = state.page <= 1 || total === 0;
  document.querySelector('[data-testid="page-next"]').disabled = state.page >= pages || total === 0;
}

function load() {
  const loader = tableRoot.querySelector('[data-testid="table-loader"]');
  loader.style.display = 'block';
  fetch(cfg.api + '?' + query().toString())
    .then(r => r.json())
    .then(data => {
      // Искусственная задержка: эмулируем асинхронную перерисовку реального приложения
      setTimeout(() => {
        state.total = data.total;
        renderHead();
        renderRows(data.rows);
        renderPagination(data.total);
        loader.style.display = 'none';
        state.renderId += 1;
        tableRoot.setAttribute('data-render-id', String(state.renderId));
      }, 180);
    });
}

document.getElementById('title').textContent = cfg.title;
document.querySelector('[data-testid="page-size"]').addEventListener('change', e => {
  state.size = parseInt(e.target.value, 10);
  state.page = 1;
  load();
});
document.querySelector('[data-testid="page-prev"]').addEventListener('click', () => { state.page -= 1; load(); });
document.querySelector('[data-testid="page-next"]').addEventListener('click', () => { state.page += 1; load(); });
buildFilters();
renderHead();
load();
</script>
</body>
</html>
"""


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):
        pass

    def _send(self, code, body, content_type):
        payload = body.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)

    def _json(self, code, data):
        self._send(code, json.dumps(data, ensure_ascii=False), "application/json; charset=utf-8")

    def do_GET(self):
        url = urlparse(self.path)
        q = parse_qs(url.query)
        if url.path == "/":
            self.send_response(302)
            self.send_header("Location", "/orders")
            self.send_header("Content-Length", "0")
            self.end_headers()
        elif url.path in ("/orders", "/users"):
            page = url.path.strip("/")
            self._send(200, PAGE_HTML.replace("__PAGE__", page), "text/html; charset=utf-8")
        elif url.path == "/api/orders":
            self._json(200, query_orders(q))
        elif url.path == "/api/users":
            self._json(200, query_users(q))
        else:
            self._json(404, {"error": "not found: " + url.path})

    def do_POST(self):
        url = urlparse(self.path)
        if url.path != "/api/test-data/orders":
            self._json(404, {"error": "not found: " + url.path})
            return
        length = int(self.headers.get("Content-Length", "0"))
        seed = json.loads(self.rfile.read(length).decode("utf-8"))
        order = {
            "number": seed["number"],
            "customer": seed["customer"],
            "status": str(seed["status"]).upper(),
            "total": float(seed["total"]),
            "createdAt": seed["createdAt"],
        }
        with LOCK:
            ORDERS.append(order)
        self._json(201, {
            "number": order["number"],
            "customer": order["customer"],
            "status": order["status"],
            "total": order["total"],
            "createdAt": order["createdAt"],
        })

    def do_DELETE(self):
        url = urlparse(self.path)
        if url.path != "/api/test-data/orders":
            self._json(404, {"error": "not found: " + url.path})
            return
        prefix = param(parse_qs(url.query), "customerPrefix")
        with LOCK:
            before = len(ORDERS)
            kept = [o for o in ORDERS if not (prefix and o["customer"].startswith(prefix))]
            ORDERS[:] = kept
        self._json(200, {"deleted": before - len(kept)})


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    seed_data()
    server = ThreadingHTTPServer(("0.0.0.0", port), Handler)
    print("Демо-стенд: http://localhost:%d/orders" % port)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        server.shutdown()


if __name__ == "__main__":
    main()
