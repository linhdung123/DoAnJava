(function (global) {
  const LS_API = 'apiBase';
  const LS_USER = 'authUser';

  const $ = (sel) => document.querySelector(sel);
  const $all = (sel) => Array.from(document.querySelectorAll(sel));

  const ui = {
    showMsg(el, text, ok) {
      if (!el) return;
      el.textContent = text || '';
      el.className = 'msg show ' + (ok ? 'msg-ok' : 'msg-error');
      if (!text) el.classList.remove('show');
    },
    hide(el) {
      if (el) el.classList.add('hidden');
    },
    show(el) {
      if (el) el.classList.remove('hidden');
    },
    closeMobileNav() {
      document.body.classList.remove('nav-open');
    },
  };

  const state = {
    get user() {
      try {
        return JSON.parse(localStorage.getItem(LS_USER) || 'null');
      } catch {
        return null;
      }
    },
    hasRole(role) {
      const u = this.user;
      return !!(u && Array.isArray(u.roles) && u.roles.includes(role));
    },
  };

  const util = {
    todayISO() {
      const d = new Date();
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${y}-${m}-${day}`;
    },
    padRange(from, to) {
      const u = new URLSearchParams();
      u.set('from', from);
      u.set('to', to);
      return u.toString();
    },
    dayStatusVi(code) {
      const map = {
        PRESENT: 'Có mặt',
        ABSENT: 'Vắng (không phép)',
        ON_LEAVE: 'Nghỉ phép (đã duyệt)',
        LEAVE_PENDING: 'Xin nghỉ (chờ duyệt)',
      };
      return map[code] || code;
    },
    /** Tổng giây → chuỗi tiếng Việt ngắn (vd. 7g 30p). */
    formatDurationSeconds(sec) {
      if (sec == null || sec < 0) return '—';
      const h = Math.floor(sec / 3600);
      const m = Math.floor((sec % 3600) / 60);
      if (h <= 0) return `${m}p`;
      return m > 0 ? `${h}g ${m}p` : `${h}g`;
    },
    fileToDataUrl(file) {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(String(reader.result || ''));
        reader.onerror = () => reject(new Error('Không đọc được file ảnh'));
        reader.readAsDataURL(file);
      });
    },
  };

  const shell = {
    saveApiBase() {
      const v = ($('#apiBaseInput').value || '').trim().replace(/\/$/, '');
      localStorage.setItem(LS_API, v);
      global.API_BASE = v;
    },
    loadApiBase() {
      const v = localStorage.getItem(LS_API) || '';
      if ($('#apiBaseInput')) $('#apiBaseInput').value = v;
      global.API_BASE = v;
    },
    applyRoleVisibility() {
      const admin = state.hasRole('ROLE_ADMIN');
      const manager = state.hasRole('ROLE_MANAGER') || admin;
      const employee = state.hasRole('ROLE_EMPLOYEE') && !manager && !admin;
      document.body.classList.toggle('show-admin', admin);
      document.body.classList.toggle('show-manager', manager);
      document.body.classList.toggle('show-employee', employee);

      const allowedPanels = admin
        ? null
        : manager
          ? new Set([
              'dashboard',
              'face-enrollment',
              'leaves',
              'my-calendar',
              'worktime',
              'overtime-logs',
              'pending',
              'reports',
              'payroll',
            ])
          : new Set(['face-enrollment', 'leaves', 'my-calendar', 'worktime', 'payroll', 'overtime-logs']);

      $all('.nav-btn[data-panel]').forEach((btn) => {
        const p = btn.getAttribute('data-panel');
        const canView = allowedPanels == null ? true : allowedPanels.has(p);
        btn.classList.toggle('hidden', !canView);
      });

      $all('.panel').forEach((panel) => {
        const p = panel.id?.replace('panel-', '');
        const canView = allowedPanels == null ? true : allowedPanels.has(p);
        panel.classList.toggle('hidden', !canView);
      });
    },
    showLogin() {
      $('#app-shell')?.classList.add('hidden');
      $('#login-screen')?.classList.remove('hidden');
    },
    showApp() {
      $('#login-screen')?.classList.add('hidden');
      $('#app-shell')?.classList.remove('hidden');
      const u = state.user;
      if ($('#userLabel')) {
        $('#userLabel').textContent = u
          ? `${u.fullName} (${u.employeeCode}) · ${(u.roles || []).join(', ')}`
          : '';
      }
      this.applyRoleVisibility();
    },
    showPanel(name) {
      $all('.panel').forEach((p) => p.classList.remove('active'));
      const panel = $('#panel-' + name);
      if (panel) panel.classList.add('active');
      $all('.nav-btn[data-panel]').forEach((b) =>
        b.classList.toggle('active', b.getAttribute('data-panel') === name)
      );
      const activeNav = document.querySelector('.nav-btn[data-panel="' + name + '"]');
      const title = activeNav && activeNav.getAttribute('data-title');
      if (title && $('#topbarTitle')) $('#topbarTitle').textContent = title;
      ui.closeMobileNav();
    },
  };

  const commonData = {
    async loadLeaveTypesSelect() {
      const sel = $('#leaveTypeId');
      if (!sel) return;
      sel.innerHTML = '';
      const rows = await global.Api.api('/api/leave-types');
      rows.forEach((r) => {
        const o = document.createElement('option');
        o.value = r.id;
        o.textContent = r.name;
        sel.appendChild(o);
      });
    },
  };

  global.AppCore = { LS_API, LS_USER, $, $all, ui, state, util, shell, commonData };
})(window);
