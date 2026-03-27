/**
 * Gọi API backend (cùng origin hoặc đặt window.API_BASE = 'http://IP:8080').
 */
(function (global) {
  function base() {
    return global.API_BASE != null ? global.API_BASE : '';
  }

  function authHeader() {
    const t = localStorage.getItem('accessToken');
    return t ? { Authorization: 'Bearer ' + t } : {};
  }

  /**
   * @param {string} path
   * @param {RequestInit & { json?: object }} opts
   */
  async function api(path, opts = {}) {
    const { json, ...rest } = opts;
    const headers = { ...authHeader(), ...rest.headers };
    let body = rest.body;
    if (json !== undefined) {
      headers['Content-Type'] = 'application/json';
      body = JSON.stringify(json);
    }
    const res = await fetch(base() + path, { ...rest, headers, body });
    if (res.status === 401) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('authUser');
      global.dispatchEvent(new CustomEvent('app:unauthorized'));
    }
    const text = await res.text();
    let data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch {
        data = { raw: text };
      }
    }
    if (!res.ok) {
      const msg =
        (data && (data.error || data.message)) ||
        res.statusText ||
        'Lỗi HTTP ' + res.status;
      const err = new Error(msg);
      err.status = res.status;
      err.data = data;
      throw err;
    }
    return data;
  }

  async function downloadCsv(path, filename) {
    const res = await fetch(base() + path, { headers: { ...authHeader() } });
    if (res.status === 401) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('authUser');
      global.dispatchEvent(new CustomEvent('app:unauthorized'));
      throw new Error('Phiên đăng nhập hết hạn');
    }
    if (!res.ok) {
      const t = await res.text();
      let msg = 'Tải file thất bại';
      try {
        const j = JSON.parse(t);
        if (j.error) msg = j.error;
      } catch {
        /* ignore */
      }
      throw new Error(msg);
    }
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename || 'export.csv';
    a.click();
    URL.revokeObjectURL(url);
  }

  global.Api = { api, downloadCsv, authHeader, base };
})(window);
