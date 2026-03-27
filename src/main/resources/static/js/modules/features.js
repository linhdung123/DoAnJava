(function (global) {
  const { $, ui, state, util, commonData } = global.AppCore;

  const dashboard = {
    async load() {
      ui.showMsg($('#dashMsg'), '', true);
      try {
        const q = $('#dashDate').value ? '?date=' + encodeURIComponent($('#dashDate').value) : '';
        const data = await global.Api.api('/api/dashboard/day' + q);
        $('#dashStats').innerHTML = `
          <div class="stat"><div class="num">${data.checkedInEmployees}</div><div class="lbl">Đã check-in</div></div>
          <div class="stat"><div class="num">${data.lateCount}</div><div class="lbl">Đi trễ</div></div>
          <div class="stat"><div class="num">${data.onApprovedLeaveCount}</div><div class="lbl">Nghỉ phép duyệt</div></div>`;
      } catch (e) {
        ui.showMsg($('#dashMsg'), e.message, false);
      }
    },
  };

  const attendance = {
    updateHistoryScopeUI() {
      if (!$('#histEmpWrap') || !$('#histEmpId')) return;
      const need = state.hasRole('ROLE_MANAGER') || state.hasRole('ROLE_ADMIN');
      $('#histEmpWrap')?.classList.toggle('hidden', !need);
      if (need && state.hasRole('ROLE_ADMIN') && state.user && !$('#histEmpId').value) {
        $('#histEmpId').value = String(state.user.employeeId);
      }
    },
    async previewNfc() {
      ui.showMsg($('#nfcMsg'), '', true);
      ui.hide($('#nfcPreviewOut'));
      const uid = $('#nfcUidPreview').value.trim();
      if (!uid) return ui.showMsg($('#nfcMsg'), 'Nhập NFC UID', false);
      try {
        const data = await global.Api.api('/api/attendance/nfc-preview?nfcUid=' + encodeURIComponent(uid));
        $('#nfcPreviewOut').textContent = JSON.stringify(data, null, 2);
        ui.show($('#nfcPreviewOut'));
      } catch (e) {
        ui.showMsg($('#nfcMsg'), e.message, false);
      }
    },
    fillSampleEmbedding() {
      const arr = Array.from({ length: 128 }, () => (Math.random() * 2 - 1).toFixed(4));
      $('#ciEmbed').value = '[' + arr.join(', ') + ']';
    },
    async checkIn() {
      ui.showMsg($('#ciMsg'), '', true);
      ui.hide($('#ciOut'));
      const body = {
        nfcUid: $('#ciNfc').value.trim(),
        faceEmbeddingJson: $('#ciEmbed').value.trim() || null,
        livenessPassed: $('#ciLiveness').checked,
        locationGps: $('#ciGps').value.trim() || null,
        verifyMethod: 'NFC_FACE',
      };
      if (!body.nfcUid) return ui.showMsg($('#ciMsg'), 'Nhập NFC UID', false);
      try {
        const data = await global.Api.api('/api/attendance/check-in', { method: 'POST', json: body });
        $('#ciOut').textContent = JSON.stringify(data, null, 2);
        ui.show($('#ciOut'));
        ui.showMsg($('#ciMsg'), 'Check-in thành công', true);
      } catch (e) {
        ui.showMsg($('#ciMsg'), e.message, false);
      }
    },
    async checkOut() {
      ui.showMsg($('#coMsg'), '', true);
      ui.hide($('#coOut'));
      const body = {
        nfcUid: $('#coNfc').value.trim(),
        locationGps: $('#coGps').value.trim() || null,
      };
      if (!body.nfcUid) return ui.showMsg($('#coMsg'), 'Nhập NFC UID', false);
      try {
        const data = await global.Api.api('/api/attendance/check-out', { method: 'POST', json: body });
        $('#coOut').textContent = JSON.stringify(data, null, 2);
        ui.show($('#coOut'));
        ui.showMsg($('#coMsg'), 'Check-out thành công', true);
      } catch (e) {
        ui.showMsg($('#coMsg'), e.message, false);
      }
    },
    async loadHistory() {
      ui.showMsg($('#histMsg'), '', true);
      const tb = $('#histTable tbody');
      tb.innerHTML = '';
      const from = $('#histFrom').value;
      const to = $('#histTo').value;
      if (!from || !to) return ui.showMsg($('#histMsg'), 'Chọn khoảng ngày', false);
      let url = '/api/attendance/history?' + util.padRange(from, to);
      if (state.hasRole('ROLE_MANAGER') || state.hasRole('ROLE_ADMIN')) {
        const eid = $('#histEmpId').value.trim();
        if (!eid) return ui.showMsg($('#histMsg'), 'Nhập employeeId', false);
        url += '&employeeId=' + encodeURIComponent(eid);
      }
      try {
        const rows = await global.Api.api(url);
        rows.forEach((r) => {
          const tr = document.createElement('tr');
          const code = r.employeeCode ? String(r.employeeCode) : '';
          const name = r.fullName ? String(r.fullName) : '';
          const who = code && name ? `${code} — ${name}` : name || code || '—';
          tr.innerHTML = `<td>${r.date}</td>
            <td>${r.checkIn ? new Date(r.checkIn).toLocaleString('vi-VN') : '—'}</td>
            <td>${r.checkOut ? new Date(r.checkOut).toLocaleString('vi-VN') : '—'}</td>
            <td>${r.status}</td><td>${who}</td>`;
          tb.appendChild(tr);
        });
      } catch (e) {
        ui.showMsg($('#histMsg'), e.message, false);
      }
    },
  };

  const payroll = {
    rows: [],
    syncScopeUI() {
      const isAdmin = state.hasRole('ROLE_ADMIN');
      $('#payrollEmpWrap')?.classList.add('hidden');
      $('#payrollCodeWrap')?.classList.toggle('hidden', !isAdmin);
      if (!isAdmin && state.user?.employeeId) {
        $('#payrollEmpId').value = String(state.user.employeeId);
      }
    },
    getSelectedYearMonth() {
      const el = $('#payrollMonth');
      if (!el || !el.value) return null;
      // value format: YYYY-MM
      const [y, m] = el.value.split('-').map((x) => parseInt(x, 10));
      if (!y || !m) return null;
      return { year: y, month: m };
    },
    isoDateLocal(d) {
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${y}-${m}-${day}`;
    },
    renderRow(tb, p) {
      const tr = document.createElement('tr');
      tr.innerHTML = `<td>${p.employeeCode || '—'}</td>
        <td>${p.fullName || '—'}</td>
        <td>${p.month}/${p.year}</td>
        <td>${(p.baseSalary || 0).toLocaleString('vi-VN')}</td>
        <td>${p.payrollDays}/${p.standardWorkDays}</td>
        <td>${(p.allowance || 0).toLocaleString('vi-VN')}</td>
        <td>${(p.overtimePay || 0).toLocaleString('vi-VN')}</td>
        <td>${(p.latePenalty || 0).toLocaleString('vi-VN')}</td>
        <td><strong>${(p.netSalary || 0).toLocaleString('vi-VN')}</strong></td>`;
      tb.appendChild(tr);
    },
    csvEscape(value) {
      const s = String(value ?? '');
      if (s.includes('"') || s.includes(',') || s.includes('\n')) {
        return `"${s.replaceAll('"', '""')}"`;
      }
      return s;
    },
    exportCsv() {
      if (!this.rows.length) {
        ui.showMsg($('#payrollMsg'), 'Chưa có dữ liệu để xuất CSV', false);
        return;
      }
      const headers = [
        'Ma NV',
        'Nhan vien',
        'Thang',
        'Nam',
        'Luong goc',
        'Ngay cong',
        'Ngay cong chuan',
        'Phu cap',
        'Tien OT',
        'Tien phat',
        'Thuc linh',
      ];
      const lines = [headers.join(',')];
      this.rows.forEach((p) => {
        lines.push(
          [
            this.csvEscape(p.employeeCode),
            this.csvEscape(p.fullName),
            this.csvEscape(p.month),
            this.csvEscape(p.year),
            this.csvEscape(p.baseSalary ?? 0),
            this.csvEscape(p.payrollDays ?? 0),
            this.csvEscape(p.standardWorkDays ?? 0),
            this.csvEscape(p.allowance ?? 0),
            this.csvEscape(p.overtimePay ?? 0),
            this.csvEscape(p.latePenalty ?? 0),
            this.csvEscape(p.netSalary ?? 0),
          ].join(',')
        );
      });
      const csvContent = '\uFEFF' + lines.join('\n');
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      const now = new Date();
      const stamp = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
      a.href = url;
      a.download = `bang-luong-${stamp}.csv`;
      a.click();
      URL.revokeObjectURL(url);
      ui.showMsg($('#payrollMsg'), `Đã xuất CSV ${this.rows.length} nhân viên`, true);
    },
    async load() {
      this.syncScopeUI();
      ui.showMsg($('#payrollMsg'), '', true);
      const tb = $('#payrollTable tbody');
      tb.innerHTML = '';
      this.rows = [];
      const ym = this.getSelectedYearMonth();
      const isAdmin = state.hasRole('ROLE_ADMIN');
      try {
        if (isAdmin) {
          const keyword = ($('#payrollCodeSearch').value || '').trim().toLowerCase();
          const employees = await global.Api.api('/api/employees');
          const filtered = employees.filter(
            (e) => !keyword || String(e.employeeCode || '').toLowerCase().includes(keyword)
          );
          if (!filtered.length) return ui.showMsg($('#payrollMsg'), 'Không có nhân viên khớp mã cần tìm', false);
          const qs = ym ? `?year=${encodeURIComponent(ym.year)}&month=${encodeURIComponent(ym.month)}` : '';
          const settled = await Promise.allSettled(
            filtered.map((e) => global.Api.api('/api/payroll/calculate/' + e.id + qs))
          );
          let ok = 0;
          settled.forEach((x) => {
            if (x.status === 'fulfilled') {
              ok += 1;
              this.rows.push(x.value);
              this.renderRow(tb, x.value);
            }
          });
          ui.showMsg($('#payrollMsg'), `Đã tải bảng lương ${ok}/${filtered.length} nhân viên`, true);
          return;
        }

        const employeeId = state.user?.employeeId;
        if (!employeeId) return ui.showMsg($('#payrollMsg'), 'Nhập employeeId hợp lệ', false);
        const qs = ym ? `?year=${encodeURIComponent(ym.year)}&month=${encodeURIComponent(ym.month)}` : '';
        const p = await global.Api.api('/api/payroll/calculate/' + employeeId + qs);
        this.rows.push(p);
        this.renderRow(tb, p);
      } catch (e) {
        ui.showMsg($('#payrollMsg'), e.message, false);
      }
    },
  };

  const overtimeLogs = {
    isoDateLocal(d) {
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${y}-${m}-${day}`;
    },
    getSelectedYearMonth() {
      const el = $('#otMonth');
      if (!el || !el.value) return null;
      const [y, m] = el.value.split('-').map((x) => parseInt(x, 10));
      if (!y || !m) return null;
      return { year: y, month: m };
    },
    renderRow(tb, r) {
      const tr = document.createElement('tr');
      tr.innerHTML = `<td>${r.employeeCode || '—'}</td>
        <td>${r.fullName || '—'}</td>
        <td>${r.date || '—'}</td>
        <td>${(r.hours || 0).toLocaleString('vi-VN')}</td>
        <td>${r.departmentName || '—'}</td>
        <td>${r.approvedByFullName || '—'}</td>`;
      tb.appendChild(tr);
    },
    async loadRange(from, to) {
      const tb = $('#otTable tbody');
      tb.innerHTML = '';
      ui.showMsg($('#otMsg'), '', true);
      if (!from || !to) return ui.showMsg($('#otMsg'), 'Chọn khoảng ngày', false);
      try {
        const rows = await global.Api.api('/api/overtime/logs?' + util.padRange(from, to));
        (Array.isArray(rows) ? rows : []).forEach((r) => this.renderRow(tb, r));
        ui.showMsg($('#otMsg'), `Đã tải ${tb.children.length} dòng OT`, true);
      } catch (e) {
        ui.showMsg($('#otMsg'), e.message, false);
      }
    },
    async loadDay() {
      const day = $('#otDay')?.value;
      if (!day) return ui.showMsg($('#otMsg'), 'Chọn ngày', false);
      return this.loadRange(day, day);
    },
    async loadMonth() {
      const ym = this.getSelectedYearMonth();
      if (!ym) return this.loadRange(util.todayISO(), util.todayISO());
      const from = `${ym.year}-${String(ym.month).padStart(2, '0')}-01`;
      const last = new Date(ym.year, ym.month, 0);
      const to = this.isoDateLocal(last);
      return this.loadRange(from, to);
    },
  };

  const worktime = {
    async load() {
      ui.showMsg($('#wtMsg'), '', true);
      const from = $('#wtFrom').value;
      const to = $('#wtTo').value;
      const tb = $('#wtTable tbody');
      tb.innerHTML = '';
      if (!from || !to) return ui.showMsg($('#wtMsg'), 'Chọn khoảng ngày', false);
      try {
        const rows = await global.Api.api('/api/attendance/scope-history?' + util.padRange(from, to));

        // Gom theo employeeId + date để chỉ có 1 dòng cho mỗi ngày.
        const map = new Map();
        (Array.isArray(rows) ? rows : []).forEach((r) => {
          const empId = r.employeeId;
          const d = r.date;
          const key = `${empId}|${d}`;
          const existing = map.get(key);
          if (!existing) {
            map.set(key, {
              employeeId: empId,
              employeeCode: r.employeeCode || '—',
              fullName: r.fullName || '—',
              date: d,
              checkInTs: r.checkIn ? new Date(r.checkIn).getTime() : null,
              checkIn: r.checkIn,
              checkOutTs: r.checkOut ? new Date(r.checkOut).getTime() : null,
              checkOut: r.checkOut,
              status: r.status || '—',
            });
            return;
          }

          // checkIn: sớm nhất
          if (r.checkIn) {
            const ts = new Date(r.checkIn).getTime();
            if (existing.checkInTs == null || ts < existing.checkInTs) {
              existing.checkInTs = ts;
              existing.checkIn = r.checkIn;
              existing.status = r.status || existing.status;
            }
          }
          // checkOut: muộn nhất
          if (r.checkOut) {
            const ts = new Date(r.checkOut).getTime();
            if (existing.checkOutTs == null || ts > existing.checkOutTs) {
              existing.checkOutTs = ts;
              existing.checkOut = r.checkOut;
            }
          }
        });

        Array.from(map.values())
          .sort((a, b) => String(a.date).localeCompare(String(b.date)) || (a.employeeCode || '').localeCompare(b.employeeCode || ''))
          .forEach((r) => {
            const tr = document.createElement('tr');
            tr.innerHTML = `<td>${r.employeeCode || '—'}</td>
              <td>${r.fullName || '—'}</td>
              <td>${r.date}</td>
              <td>${r.checkIn ? new Date(r.checkIn).toLocaleString('vi-VN') : '—'}</td>
              <td>${r.checkOut ? new Date(r.checkOut).toLocaleString('vi-VN') : '—'}</td>
              <td>${r.status || '—'}</td>`;
            tb.appendChild(tr);
          });

        const isManager = state.hasRole('ROLE_MANAGER');
        const scopeText = isManager ? 'Dữ liệu phòng ban của bạn' : 'Dữ liệu cá nhân của bạn';
        ui.showMsg($('#wtMsg'), scopeText, true);
      } catch (e) {
        ui.showMsg($('#wtMsg'), e.message, false);
      }
    },
  };

  const leaves = {
    async refreshMine() {
      const tb = $('#leaveMyTable tbody');
      tb.innerHTML = '';
      ui.showMsg($('#leaveMyMsg'), '', true);
      try {
        const rows = await global.Api.api('/api/leave-requests');
        rows.forEach((r) => {
          const tr = document.createElement('tr');
          tr.innerHTML = `<td>${r.id}</td><td>${r.leaveTypeName}</td><td>${r.startDate}</td><td>${r.endDate}</td><td>${r.status}</td>`;
          tb.appendChild(tr);
        });
      } catch (e) {
        ui.showMsg($('#leaveMyMsg'), e.message, false);
      }
    },
    async create() {
      ui.showMsg($('#leaveCreateMsg'), '', true);
      const u = state.user;
      const body = {
        employeeId: u.employeeId,
        leaveTypeId: parseInt($('#leaveTypeId').value, 10),
        startDate: $('#leaveStart').value,
        endDate: $('#leaveEnd').value,
        reason: $('#leaveReason').value.trim() || null,
      };
      try {
        await global.Api.api('/api/leave-requests', { method: 'POST', json: body });
        ui.showMsg($('#leaveCreateMsg'), 'Đã gửi đơn', true);
        await this.refreshMine();
      } catch (e) {
        ui.showMsg($('#leaveCreateMsg'), e.message, false);
      }
    },
    async refreshPending() {
      const tb = $('#pendingTable tbody');
      tb.innerHTML = '';
      ui.showMsg($('#pendingMsg'), '', true);
      try {
        const rows = await global.Api.api('/api/leave-requests/pending');
        rows.forEach((r) => {
          const tr = document.createElement('tr');
          tr.innerHTML = `<td>${r.id}</td><td>${r.employeeName}</td><td>${r.leaveTypeName}</td><td>${r.startDate}</td><td>${r.endDate}</td>
            <td>
              <button type="button" class="btn btn-small btn-primary" data-apr="${r.id}">Duyệt</button>
              <button type="button" class="btn btn-small btn-danger" data-rej="${r.id}">Từ chối</button>
            </td>`;
          tb.appendChild(tr);
        });
      } catch (e) {
        ui.showMsg($('#pendingMsg'), e.message, false);
      }
    },
    async approve(id) {
      try {
        await global.Api.api('/api/leave-requests/' + id + '/approve', { method: 'POST' });
        await this.refreshPending();
      } catch (e) {
        ui.showMsg($('#pendingMsg'), e.message, false);
      }
    },
    async reject(id) {
      try {
        await global.Api.api('/api/leave-requests/' + id + '/reject', { method: 'POST' });
        await this.refreshPending();
      } catch (e) {
        ui.showMsg($('#pendingMsg'), e.message, false);
      }
    },
  };

  const faceEnrollment = {
    async submit() {
      const file = $('#faceImageFile')?.files?.[0];
      ui.showMsg($('#faceEnrollMsg'), '', true);
      ui.hide($('#faceEnrollOut'));
      if (!file) return ui.showMsg($('#faceEnrollMsg'), 'Chọn file ảnh trước khi gửi', false);
      try {
        const imageBase64 = await util.fileToDataUrl(file);
        const data = await global.Api.api('/api/face-enrollment/me', {
          method: 'POST',
          json: { faceImageBase64: imageBase64 },
        });
        ui.showMsg($('#faceEnrollMsg'), 'Đã gửi ảnh, chờ admin duyệt', true);
        $('#faceEnrollOut').textContent = JSON.stringify(data, null, 2);
        ui.show($('#faceEnrollOut'));
      } catch (e) {
        ui.showMsg($('#faceEnrollMsg'), e.message, false);
      }
    },
    async myStatus() {
      ui.showMsg($('#faceEnrollMsg'), '', true);
      ui.hide($('#faceEnrollOut'));
      try {
        const data = await global.Api.api('/api/face-enrollment/me');
        const message =
          data.status === 'APPROVED'
            ? 'Đã duyệt khuôn mặt'
            : data.status === 'PENDING'
              ? 'Ảnh đang chờ duyệt'
              : data.status === 'REJECTED'
                ? 'Ảnh bị từ chối'
                : 'Chưa gửi ảnh';
        ui.showMsg($('#faceEnrollMsg'), message, data.status !== 'REJECTED');
        $('#faceEnrollOut').textContent = JSON.stringify(data, null, 2);
        ui.show($('#faceEnrollOut'));
      } catch (e) {
        ui.showMsg($('#faceEnrollMsg'), e.message, false);
      }
    },
    async reportLostNfc() {
      ui.showMsg($('#faceEnrollMsg'), '', true);
      try {
        await global.Api.api('/api/employee-self/report-lost-nfc', { method: 'POST' });
        ui.showMsg(
          $('#faceEnrollMsg'),
          'Đã báo mất thẻ NFC. Thẻ cũ đã bị vô hiệu hoá, vui lòng liên hệ Admin để cấp thẻ mới.',
          true
        );
      } catch (e) {
        ui.showMsg($('#faceEnrollMsg'), e.message, false);
      }
    },
    async loadPending() {
      const tb = $('#facePendingTable tbody');
      tb.innerHTML = '';
      ui.showMsg($('#facePendingMsg'), '', true);
      try {
        const rows = await global.Api.api('/api/face-enrollment/pending');
        rows.forEach((r) => {
          const tr = document.createElement('tr');
          tr.innerHTML = `<td>${r.employeeId}</td><td>${r.employeeCode}</td><td>${r.fullName}</td>
            <td>${r.faceImageBase64 ? `<img src="${r.faceImageBase64}" alt="face" style="max-width:120px;max-height:120px;border-radius:8px;border:1px solid #334155"/>` : '—'}</td>
            <td><button type="button" class="btn btn-small btn-primary" data-fa-a="${r.employeeId}">Duyệt</button></td>
            <td><button type="button" class="btn btn-small btn-danger" data-fa-r="${r.employeeId}">Từ chối</button></td>`;
          tb.appendChild(tr);
        });
      } catch (e) {
        ui.showMsg($('#facePendingMsg'), e.message, false);
      }
    },
    async approve(id) {
      try {
        const manual = ($('#faceApproveEmbedJson')?.value || '').trim();
        await global.Api.api('/api/face-enrollment/' + id + '/review', {
          method: 'POST',
          json: {
            approved: true,
            faceEmbeddingJson: manual.length > 0 ? manual : null,
            rejectReason: null,
          },
        });
        ui.showMsg(
          $('#facePendingMsg'),
          manual.length > 0
            ? 'Đã duyệt và lưu embedding từ JSON bạn dán.'
            : 'Đã duyệt. Vector đã lưu (gọi model Python /embed hoặc từ JSON).',
          true
        );
        await this.loadPending();
      } catch (e) {
        ui.showMsg($('#facePendingMsg'), e.message, false);
      }
    },
    async reject(id) {
      try {
        const reason = prompt('Lý do từ chối:', 'Ảnh chưa rõ khuôn mặt');
        if (reason == null) return;
        await global.Api.api('/api/face-enrollment/' + id + '/review', {
          method: 'POST',
          json: { approved: false, faceEmbeddingJson: null, rejectReason: reason },
        });
        await this.loadPending();
      } catch (e) {
        ui.showMsg($('#facePendingMsg'), e.message, false);
      }
    },
  };

  const admin = {
    allEmployeesCache: [],
    employeesCache: [],
    employeePage: 1,
    employeePageSize: 10,
    resetEmpForm() {
      $('#empEditId').value = '';
      $('#empCode').value = '';
      $('#empName').value = '';
      $('#empEmail').value = '';
      $('#empDept').value = '';
      $('#empStatus').value = 'ACTIVE';
      $('#empRole').value = 'ROLE_EMPLOYEE';
      $('#empPass').value = '';
      $('#empBaseSalary').value = '';
      $('#empAllowance').value = '';
      $('#empStandardDays').value = '26';
      $('#empFormTitle').textContent = 'Thêm nhân viên';
      $('#btnEmpCancel').classList.add('hidden');
    },
    async loadDeptOptions() {
      const sel = $('#empDept');
      const filterSel = $('#empFilterDept');
      if (sel) sel.innerHTML = '<option value="">—</option>';
      if (filterSel) filterSel.innerHTML = '<option value="">Tất cả</option>';
      try {
        const rows = await global.Api.api('/api/departments');
        rows.forEach((r) => {
          if (sel) {
            const o = document.createElement('option');
            o.value = r.id;
            o.textContent = r.name;
            sel.appendChild(o);
          }
          if (filterSel) {
            const o2 = document.createElement('option');
            o2.value = String(r.id);
            o2.textContent = r.name;
            filterSel.appendChild(o2);
          }
        });
      } catch {
        // ignore
      }
    },
    async refreshEmployees() {
      const tb = $('#empTable tbody');
      if (!tb) return;
      tb.innerHTML = '';
      ui.showMsg($('#empListMsg'), '', true);
      try {
        const rows = await global.Api.api('/api/employees');
        this.allEmployeesCache = Array.isArray(rows) ? rows : [];
        this.applyEmployeeFilters();
      } catch (e) {
        ui.showMsg($('#empListMsg'), e.message, false);
      }
    },
    applyEmployeeFilters() {
      const deptVal = ($('#empFilterDept')?.value || '').trim();
      const roleVal = ($('#empFilterRole')?.value || '').trim();
      const keyword = ($('#empFilterKeyword')?.value || '').trim().toLowerCase();

      this.employeesCache = this.allEmployeesCache.filter((r) => {
        const deptId = r.departmentId != null ? String(r.departmentId) : '';
        const role = r.role != null ? String(r.role) : '';
        const code = r.employeeCode != null ? String(r.employeeCode).toLowerCase() : '';
        const name = r.fullName != null ? String(r.fullName).toLowerCase() : '';

        const okDept = !deptVal || deptVal === deptId;
        const okRole = !roleVal || roleVal === role;
        const okKeyword = !keyword || code.includes(keyword) || name.includes(keyword);
        return okDept && okRole && okKeyword;
      });
      this.employeePage = 1;
      this.renderEmployeePage();
    },
    clearEmployeeFilters() {
      if ($('#empFilterDept')) $('#empFilterDept').value = '';
      if ($('#empFilterRole')) $('#empFilterRole').value = '';
      if ($('#empFilterKeyword')) $('#empFilterKeyword').value = '';
      this.applyEmployeeFilters();
    },
    renderEmployeePage() {
      const tb = $('#empTable tbody');
      if (!tb) return;
      tb.innerHTML = '';
      const total = this.employeesCache.length;
      const size = Math.max(1, parseInt($('#empPageSize')?.value || this.employeePageSize, 10));
      this.employeePageSize = size;
      const totalPages = Math.max(1, Math.ceil(total / size));
      this.employeePage = Math.min(Math.max(1, this.employeePage), totalPages);
      const start = (this.employeePage - 1) * size;
      const pageRows = this.employeesCache.slice(start, start + size);

      pageRows.forEach((r) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `<td>${r.id}</td><td>${r.employeeCode}</td><td>${r.fullName}</td><td>${r.departmentName || '—'}</td><td>${r.role || '—'}</td>
          <td>${r.nfcUid || '—'}</td>
          <td><button type="button" class="btn btn-small btn-secondary" data-emp-e="${r.id}">Sửa</button>
              <button type="button" class="btn btn-small btn-primary" data-emp-r="${r.id}">Reset MK</button>
              <button type="button" class="btn btn-small btn-danger" data-emp-l="${r.id}">Mất thẻ</button></td>`;
        tb.appendChild(tr);
      });

      if ($('#empPageInfo')) {
        $('#empPageInfo').textContent = `Trang ${this.employeePage}/${totalPages} · ${total} nhân viên`;
      }
      if ($('#btnEmpPrev')) $('#btnEmpPrev').disabled = this.employeePage <= 1;
      if ($('#btnEmpNext')) $('#btnEmpNext').disabled = this.employeePage >= totalPages;
    },
    nextEmployeePage() {
      this.employeePage += 1;
      this.renderEmployeePage();
    },
    prevEmployeePage() {
      this.employeePage -= 1;
      this.renderEmployeePage();
    },
    changeEmployeePageSize() {
      this.employeePage = 1;
      this.renderEmployeePage();
    },
    async startEditEmployee(id) {
      ui.showMsg($('#empFormMsg'), '', true);
      try {
        const r = await global.Api.api('/api/employees/' + id);
        $('#empEditId').value = String(r.id);
        $('#empCode').value = r.employeeCode || '';
        $('#empName').value = r.fullName || '';
        $('#empEmail').value = r.email || '';
        $('#empDept').value = r.departmentId || '';
        $('#empStatus').value = r.status || 'ACTIVE';
        $('#empRole').value = r.role || 'ROLE_EMPLOYEE';
        $('#empPass').value = '';
        $('#empBaseSalary').value = r.baseSalary ?? '';
        $('#empAllowance').value = r.allowance ?? '';
        $('#empStandardDays').value = r.standardWorkDays ?? 26;
        $('#empFormTitle').textContent = 'Sửa nhân viên #' + r.id;
        $('#btnEmpCancel').classList.remove('hidden');
      } catch (e) {
        ui.showMsg($('#empFormMsg'), e.message, false);
      }
    },
    async saveEmployee() {
      const id = $('#empEditId').value.trim();
      const body = {
        employeeCode: $('#empCode').value.trim() || null,
        fullName: $('#empName').value.trim(),
        email: $('#empEmail').value.trim() || null,
        departmentId: $('#empDept').value ? parseInt($('#empDept').value, 10) : null,
        status: $('#empStatus').value,
        role: $('#empRole').value,
        password: $('#empPass').value || null,
        baseSalary: $('#empBaseSalary').value ? parseFloat($('#empBaseSalary').value) : null,
        allowance: $('#empAllowance').value ? parseFloat($('#empAllowance').value) : null,
        standardWorkDays: $('#empStandardDays').value ? parseInt($('#empStandardDays').value, 10) : null,
      };
      if (!body.fullName) return ui.showMsg($('#empFormMsg'), 'Thiếu họ tên', false);
      try {
        if (id) {
          await global.Api.api('/api/employees/' + id, { method: 'PUT', json: body });
          ui.showMsg($('#empFormMsg'), 'Đã cập nhật nhân viên', true);
        } else {
          const data = await global.Api.api('/api/employees', { method: 'POST', json: body });
          const temp = data?.temporaryPassword ? ` | MK tạm: ${data.temporaryPassword}` : '';
          ui.showMsg($('#empFormMsg'), 'Đã tạo nhân viên' + temp, true);
        }
        this.resetEmpForm();
        await this.refreshEmployees();
      } catch (e) {
        ui.showMsg($('#empFormMsg'), e.message, false);
      }
    },
    async resetEmployeePassword(id) {
      ui.showMsg($('#empListMsg'), '', true);
      try {
        const data = await global.Api.api('/api/employees/' + id + '/reset-password', { method: 'POST' });
        ui.showMsg($('#empListMsg'), `Mật khẩu mới: ${data.newPassword || data.password || data}`, true);
      } catch (e) {
        ui.showMsg($('#empListMsg'), e.message, false);
      }
    },
    async reportLostNfcByAdmin(id) {
      ui.showMsg($('#empListMsg'), '', true);
      if (!confirm('Đánh dấu mất thẻ cho nhân viên này? Thẻ NFC cũ sẽ bị vô hiệu hoá.')) return;
      try {
        await global.Api.api('/api/employees/' + id + '/report-lost-nfc', { method: 'POST' });
        ui.showMsg($('#empListMsg'), 'Đã vô hiệu hoá thẻ NFC cũ của nhân viên', true);
        await this.refreshEmployees();
      } catch (e) {
        ui.showMsg($('#empListMsg'), e.message, false);
      }
    },
    resetDeptForm() {
      if ($('#deptEditId')) $('#deptEditId').value = '';
      if ($('#deptName')) $('#deptName').value = '';
      if ($('#deptDesc')) $('#deptDesc').value = '';
      $('#btnDeptCancel')?.classList.add('hidden');
      if ($('#btnDeptSave')) $('#btnDeptSave').textContent = 'Lưu';
    },
    async startEditDepartment(id) {
      ui.showMsg($('#deptMsg'), '', true);
      try {
        const d = await global.Api.api('/api/departments/' + id);
        $('#deptEditId').value = String(d.id);
        $('#deptName').value = d.name || '';
        $('#deptDesc').value = d.description || '';
        $('#btnDeptCancel')?.classList.remove('hidden');
        $('#btnDeptSave').textContent = 'Cập nhật';
      } catch (e) {
        ui.showMsg($('#deptMsg'), e.message, false);
      }
    },
    async saveDepartment() {
      ui.showMsg($('#deptMsg'), '', true);
      const name = $('#deptName').value.trim();
      const description = ($('#deptDesc')?.value || '').trim() || null;
      if (!name) return ui.showMsg($('#deptMsg'), 'Nhập tên phòng ban', false);
      try {
        const id = ($('#deptEditId')?.value || '').trim();
        if (id) {
          await global.Api.api('/api/departments/' + encodeURIComponent(id), {
            method: 'PUT',
            json: { name, description },
          });
          ui.showMsg($('#deptMsg'), 'Đã cập nhật phòng ban', true);
        } else {
          await global.Api.api('/api/departments', { method: 'POST', json: { name, description } });
          ui.showMsg($('#deptMsg'), 'Đã thêm phòng ban', true);
        }
        this.resetDeptForm();
        await this.loadDeptOptions();
        await this.refreshDepartments();
      } catch (e) {
        ui.showMsg($('#deptMsg'), e.message, false);
      }
    },
    async deleteDepartment(id) {
      ui.showMsg($('#deptMsg'), '', true);
      if (!confirm('Xoá phòng ban này?')) return;
      try {
        await global.Api.api('/api/departments/' + encodeURIComponent(id), { method: 'DELETE' });
        ui.showMsg($('#deptMsg'), 'Đã xoá phòng ban', true);
        await this.loadDeptOptions();
        await this.refreshDepartments();
      } catch (e) {
        ui.showMsg($('#deptMsg'), e.message, false);
      }
    },
    resetLtForm() {
      if ($('#ltEditId')) $('#ltEditId').value = '';
      if ($('#ltName')) $('#ltName').value = '';
      if ($('#ltMax')) $('#ltMax').value = '';
      if ($('#ltPaid')) $('#ltPaid').checked = true;
      if ($('#ltPayPercentage')) $('#ltPayPercentage').value = '1';
      $('#btnLtCancel')?.classList.add('hidden');
      if ($('#btnLtSave')) $('#btnLtSave').textContent = 'Lưu';
    },
    async startEditLeaveType(id) {
      ui.showMsg($('#ltMsg'), '', true);
      try {
        const lt = await global.Api.api('/api/leave-types/' + id);
        $('#ltEditId').value = String(lt.id);
        $('#ltName').value = lt.name || '';
        $('#ltMax').value = lt.maxDaysPerYear == null ? '' : String(lt.maxDaysPerYear);
        const paid = lt.paid === true || (lt.payPercentage != null && lt.payPercentage > 0);
        $('#ltPaid').checked = paid;
        $('#ltPayPercentage').value =
          lt.payPercentage == null ? (paid ? '1' : '0') : String(lt.payPercentage);
        $('#btnLtCancel')?.classList.remove('hidden');
        $('#btnLtSave').textContent = 'Cập nhật';
      } catch (e) {
        ui.showMsg($('#ltMsg'), e.message, false);
      }
    },
    async saveLeaveType() {
      ui.showMsg($('#ltMsg'), '', true);
      const body = {
        name: $('#ltName').value.trim(),
        maxDaysPerYear: ($('#ltMax').value || '').trim() ? parseInt($('#ltMax').value, 10) : 0,
        paid: $('#ltPaid').checked,
        payPercentage: parseFloat($('#ltPayPercentage').value || ($('#ltPaid').checked ? '1' : '0')),
      };
      if (!body.name) return ui.showMsg($('#ltMsg'), 'Nhập tên loại nghỉ', false);
      try {
        const id = ($('#ltEditId')?.value || '').trim();
        if (id) {
          await global.Api.api('/api/leave-types/' + encodeURIComponent(id), { method: 'PUT', json: body });
          ui.showMsg($('#ltMsg'), 'Đã cập nhật loại nghỉ', true);
        } else {
          await global.Api.api('/api/leave-types', { method: 'POST', json: body });
          ui.showMsg($('#ltMsg'), 'Đã thêm loại nghỉ', true);
        }
        this.resetLtForm();
        await commonData.loadLeaveTypesSelect();
        await this.refreshLeaveTypes();
      } catch (e) {
        ui.showMsg($('#ltMsg'), e.message, false);
      }
    },
    async deleteLeaveType(id) {
      ui.showMsg($('#ltMsg'), '', true);
      if (!confirm('Xoá loại nghỉ này?')) return;
      try {
        await global.Api.api('/api/leave-types/' + encodeURIComponent(id), { method: 'DELETE' });
        ui.showMsg($('#ltMsg'), 'Đã xoá loại nghỉ', true);
        await commonData.loadLeaveTypesSelect();
        await this.refreshLeaveTypes();
      } catch (e) {
        ui.showMsg($('#ltMsg'), e.message, false);
      }
    },
    async refreshDepartments() {
      const tb = $('#deptTable tbody');
      if (!tb) return;
      tb.innerHTML = '';
      ui.showMsg($('#deptMsg'), '', true);
      try {
        const rows = await global.Api.api('/api/departments');
        rows.forEach((r) => {
          const tr = document.createElement('tr');
          tr.innerHTML = `<td>${r.id}</td><td>${r.name || '—'}</td><td>${r.description || '—'}</td>
            <td>
              <button type="button" class="btn btn-small btn-secondary" data-dept-e="${r.id}">Sửa</button>
              <button type="button" class="btn btn-small btn-danger" data-dept-d="${r.id}">Xoá</button>
            </td>`;
          tb.appendChild(tr);
        });
      } catch (e) {
        ui.showMsg($('#deptMsg'), e.message, false);
      }
    },
    async refreshLeaveTypes() {
      const tb = $('#ltTable tbody');
      if (!tb) return;
      tb.innerHTML = '';
      ui.showMsg($('#ltMsg'), '', true);
      try {
        const rows = await global.Api.api('/api/leave-types');
        rows.forEach((r) => {
          const tr = document.createElement('tr');
          tr.innerHTML = `<td>${r.id}</td><td>${r.name || '—'}</td><td>${r.maxDaysPerYear ?? '—'}</td>
            <td>
              <button type="button" class="btn btn-small btn-secondary" data-lt-e="${r.id}">Sửa</button>
              <button type="button" class="btn btn-small btn-danger" data-lt-d="${r.id}">Xoá</button>
            </td>`;
          tb.appendChild(tr);
        });
      } catch (e) {
        ui.showMsg($('#ltMsg'), e.message, false);
      }
    },
  };

  const reports = {
    async downloadMonthly() {
      ui.showMsg($('#repMsg'), '', true);
      const y = parseInt($('#repYear').value, 10);
      const m = parseInt($('#repMonth').value, 10);
      if (!y || !m) return ui.showMsg($('#repMsg'), 'Nhập năm và tháng', false);
      try {
        await global.Api.downloadCsv(
          `/api/reports/attendance/monthly.csv?year=${y}&month=${m}`,
          `bang-cong-${y}-${String(m).padStart(2, '0')}.csv`
        );
        ui.showMsg($('#repMsg'), 'Đã tải file', true);
      } catch (e) {
        ui.showMsg($('#repMsg'), e.message, false);
      }
    },
  };

  const calendar = {
    async load() {
      const day = $('#calDay').value;
      const tb = $('#myCalTable tbody');
      tb.innerHTML = '';
      ui.hide($('#myCalSummary'));
      $('#myCalSummary').innerHTML = '';
      ui.showMsg($('#myCalMsg'), '', true);
      if (!day) return ui.showMsg($('#myCalMsg'), 'Chọn ngày', false);
      try {
        const [rows, overtimeRows] = await Promise.all([
          global.Api.api(
            '/api/attendance/scope-calendar?' + new URLSearchParams({ from: day, to: day }).toString()
          ),
          global.Api.api('/api/overtime/logs?' + new URLSearchParams({ from: day, to: day }).toString()),
        ]);
        const isAdmin = state.hasRole('ROLE_ADMIN');
        const isManager = state.hasRole('ROLE_MANAGER');
        const canApproveOt = isAdmin || isManager;
        const who = isAdmin ? 'Đang xem toàn hệ thống' : isManager ? 'Đang xem phòng ban của bạn' : 'Đang xem theo tài khoản đăng nhập';
        ui.showMsg($('#myCalMsg'), who, true);

        const otMap = new Map();
        const otApprovedKeys = new Set();
        (Array.isArray(overtimeRows) ? overtimeRows : []).forEach((o) => {
          const key = `${o.employeeId}|${o.date}`;
          otApprovedKeys.add(key);
          const prev = otMap.get(key) || 0;
          otMap.set(key, prev + Number(o.hours || 0));
        });

        rows.forEach((r) => {
          const tr = document.createElement('tr');
          const note = r.note && String(r.note).trim() ? r.note : '—';
          const totalText = r.dayStatus === 'PRESENT' ? util.formatDurationSeconds(r.totalPresentSeconds) : '—';
          const totalStyle = r.shortPresent ? 'color:#dc2626;font-weight:700' : '';
          const rowKey = `${r.employeeId}|${r.date}`;
          const otHours = otMap.get(rowKey);
          const otChecked = otApprovedKeys.has(rowKey);
          const otDisabled = r.dayStatus !== 'PRESENT';
          const otCell = canApproveOt
            ? `<input type="checkbox" ${otChecked ? 'checked' : ''} ${otDisabled ? 'disabled' : ''} data-ot-employee-id="${r.employeeId}" data-ot-date="${r.date}" />`
            : (otChecked ? 'Đã duyệt' : '—');
          tr.innerHTML = `<td>${r.employeeCode || '—'}</td>
            <td>${r.fullName || '—'}</td>
            <td>${r.date}</td>
            <td>${util.dayStatusVi(r.dayStatus)}</td>
            <td style="${totalStyle}">${totalText}</td>
            <td>${otCell}</td>
            <td>${note}</td>`;
          tr.innerHTML += `<td>
            <button type="button" class="btn btn-small btn-secondary" data-cal-detail-emp="${r.employeeId}" data-cal-detail-date="${r.date}">
              Xem log
            </button>
          </td>`;
          tb.appendChild(tr);
        });
      } catch (e) {
        ui.hide($('#myCalSummary'));
        ui.showMsg($('#myCalMsg'), e.message, false);
      }
    },
  };

  global.AppFeatures = {
    dashboard,
    attendance,
    leaves,
    faceEnrollment,
    admin,
    reports,
    calendar,
    payroll,
    overtimeLogs,
    worktime,
  };
})(window);
