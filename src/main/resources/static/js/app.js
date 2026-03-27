(() => {
  const { LS_USER, $, $all, ui, state, util, shell, commonData } = window.AppCore;
  const { dashboard, attendance, leaves, faceEnrollment, admin, reports, calendar, payroll, overtimeLogs, worktime } =
    window.AppFeatures;

  const auth = {
    async forceChangeInitialPassword(employeeCode, currentPassword) {
      while (true) {
        const next = prompt('Tài khoản đăng nhập lần đầu. Nhập mật khẩu mới (ít nhất 6 ký tự):', '');
        if (next == null) return false;
        const confirm = prompt('Nhập lại mật khẩu mới:', '');
        if (confirm == null) return false;
        if (!next || next.length < 6) {
          alert('Mật khẩu mới phải từ 6 ký tự trở lên.');
          continue;
        }
        if (next !== confirm) {
          alert('Mật khẩu nhập lại không khớp.');
          continue;
        }
        await window.Api.api('/api/auth/change-initial-password', {
          method: 'POST',
          json: {
            employeeCode,
            currentPassword,
            newPassword: next,
          },
        });
        return true;
      }
    },
    async login() {
      ui.showMsg($('#loginMsg'), '', true);
      const payload = { employeeCode: $('#loginCode').value.trim(), password: $('#loginPass').value };
      if (!payload.employeeCode || !payload.password) return ui.showMsg($('#loginMsg'), 'Nhập tài khoản và mật khẩu', false);
      try {
        const data = await window.Api.api('/api/auth/login', { method: 'POST', json: payload });
        const token = data.accessToken;
        if (!token) throw new Error('Phản hồi đăng nhập thiếu accessToken');
        localStorage.setItem('accessToken', token);
        localStorage.setItem('token', token);
        localStorage.setItem(
          LS_USER,
          JSON.stringify({
            employeeId: data.employeeId,
            employeeCode: data.employeeCode,
            fullName: data.fullName,
            departmentId: data.departmentId,
            roles: data.roles || [],
          })
        );
        if (data.mustChangePassword) {
          const changed = await this.forceChangeInitialPassword(payload.employeeCode, payload.password);
          if (!changed) {
            this.logout();
            return ui.showMsg($('#loginMsg'), 'Bạn cần đổi mật khẩu lần đầu để tiếp tục sử dụng.', false);
          }
          ui.showMsg($('#loginMsg'), 'Đổi mật khẩu thành công. Đang vào hệ thống...', true);
        }
        shell.showApp();
        await app.afterLogin();
      } catch (e) {
        ui.showMsg($('#loginMsg'), e.message, false);
      }
    },
    logout() {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('token');
      localStorage.removeItem(LS_USER);
      shell.showLogin();
    },
  };

  const app = {
    defaultPanelByRole() {
      if (state.hasRole('ROLE_ADMIN')) return 'dashboard';
      if (state.hasRole('ROLE_MANAGER')) return 'dashboard';
      return 'my-calendar';
    },
    async afterLogin() {
      shell.applyRoleVisibility();
      attendance.updateHistoryScopeUI();
      payroll.syncScopeUI();
      if ($('#payrollEmpId') && state.user?.employeeId && !$('#payrollEmpId').value) {
        $('#payrollEmpId').value = String(state.user.employeeId);
      }
      await Promise.allSettled([commonData.loadLeaveTypesSelect(), admin.loadDeptOptions()]);
      admin.resetEmpForm();
      const panel = this.defaultPanelByRole();
      shell.showPanel(panel);
      if (panel === 'dashboard') await dashboard.load();
      if (panel === 'my-calendar') await calendar.load();
    },
    bindEvents() {
      $('#btnSaveApi')?.addEventListener('click', shell.saveApiBase);
      $('#btnLogin')?.addEventListener('click', () => auth.login());
      $('#btnLogout')?.addEventListener('click', () => auth.logout());
      $('#btnMenu')?.addEventListener('click', () => document.body.classList.toggle('nav-open'));
      $('#sidebarBackdrop')?.addEventListener('click', () => ui.closeMobileNav());

      $all('.nav-btn[data-panel]').forEach((btn) =>
        btn.addEventListener('click', async () => {
          const p = btn.getAttribute('data-panel');
          shell.showPanel(p);
          if (p === 'dashboard') await dashboard.load();
          if (p === 'history') attendance.updateHistoryScopeUI();
          if (p === 'leaves') {
            await leaves.refreshMine();
            if (state.hasRole('ROLE_MANAGER') || state.hasRole('ROLE_ADMIN')) await leaves.refreshPending();
          }
          if (p === 'face-approval') await faceEnrollment.loadPending();
          if (p === 'my-calendar') await calendar.load();
          if (p === 'worktime') await worktime.load();
          if (p === 'employee-list') await admin.refreshEmployees();
          if (p === 'departments') await admin.refreshDepartments();
          if (p === 'leave-types') await admin.refreshLeaveTypes();
          if (p === 'payroll') await payroll.load();
        })
      );

      $('#dashDate') && ($('#dashDate').value = util.todayISO());
      $('#btnDashLoad')?.addEventListener('click', () => dashboard.load());

      $('#btnNfcPreview')?.addEventListener('click', () => attendance.previewNfc());
      $('#btnSampleEmbed')?.addEventListener('click', () => attendance.fillSampleEmbedding());
      $('#btnCheckIn')?.addEventListener('click', () => attendance.checkIn());
      $('#btnCheckOut')?.addEventListener('click', () => attendance.checkOut());
      $('#btnHistory')?.addEventListener('click', () => attendance.loadHistory());

      $('#btnLeaveCreate')?.addEventListener('click', () => leaves.create());
      $('#btnLeaveList')?.addEventListener('click', () => leaves.refreshMine());
      $('#btnPendingLoad')?.addEventListener('click', () => leaves.refreshPending());
      $('#pendingTable')?.addEventListener('click', async (e) => {
        const t = e.target;
        const aid = t.getAttribute('data-apr');
        const rid = t.getAttribute('data-rej');
        if (aid) await leaves.approve(aid);
        if (rid) await leaves.reject(rid);
      });

      $('#btnMyCalLoad')?.addEventListener('click', () => calendar.load());
      $('#btnWtLoad')?.addEventListener('click', () => worktime.load());
      $('#btnPayrollLoad')?.addEventListener('click', () => payroll.load());
      $('#btnPayrollExport')?.addEventListener('click', () => payroll.exportCsv());
      $('#btnOtLoadDay')?.addEventListener('click', () => overtimeLogs.loadDay());
      $('#btnOtLoadMonth')?.addEventListener('click', () => overtimeLogs.loadMonth());
      $('#calDay') && ($('#calDay').value = util.todayISO());
      $('#wtFrom') && ($('#wtFrom').value = util.todayISO().slice(0, 8) + '01');
      $('#wtTo') && ($('#wtTo').value = util.todayISO());

      // Default month/day for OT & payroll
      const now = new Date();
      if ($('#payrollMonth')) {
        const ym = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
        $('#payrollMonth').value = ym;
      }
      if ($('#otMonth')) {
        const ym = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
        $('#otMonth').value = ym;
      }
      if ($('#otDay')) $('#otDay').value = util.todayISO();

      $('#btnReportDl')?.addEventListener('click', () => reports.downloadMonthly());
      $('#repYear') && ($('#repYear').value = String(now.getFullYear()));
      $('#repMonth') && ($('#repMonth').value = String(now.getMonth() + 1));

      $('#btnFaceSubmit')?.addEventListener('click', () => faceEnrollment.submit());
      $('#btnFaceStatus')?.addEventListener('click', () => faceEnrollment.myStatus());
      $('#btnReportLostNfc')?.addEventListener('click', () => faceEnrollment.reportLostNfc());
      $('#btnFacePendingLoad')?.addEventListener('click', () => faceEnrollment.loadPending());
      $('#facePendingTable')?.addEventListener('click', async (e) => {
        const t = e.target;
        const approveId = t.getAttribute('data-fa-a');
        const rejectId = t.getAttribute('data-fa-r');
        if (approveId) await faceEnrollment.approve(approveId);
        if (rejectId) await faceEnrollment.reject(rejectId);
      });

      $('#empTable')?.addEventListener('click', async (e) => {
        const t = e.target;
        const eid = t.getAttribute('data-emp-e');
        const pid = t.getAttribute('data-emp-r');
        const lid = t.getAttribute('data-emp-l');
        if (eid) {
          await admin.startEditEmployee(eid);
          shell.showPanel('employee-create');
        }
        if (pid) await admin.resetEmployeePassword(pid);
        if (lid) await admin.reportLostNfcByAdmin(lid);
      });
      $('#btnEmpSave')?.addEventListener('click', () => admin.saveEmployee());
      $('#btnEmpCancel')?.addEventListener('click', () => admin.resetEmpForm());
      $('#btnEmpReload')?.addEventListener('click', () => admin.refreshEmployees());
      $('#btnEmpPrev')?.addEventListener('click', () => admin.prevEmployeePage());
      $('#btnEmpNext')?.addEventListener('click', () => admin.nextEmployeePage());
      $('#empPageSize')?.addEventListener('change', () => admin.changeEmployeePageSize());
      $('#empFilterDept')?.addEventListener('change', () => admin.applyEmployeeFilters());
      $('#empFilterRole')?.addEventListener('change', () => admin.applyEmployeeFilters());
      $('#empFilterKeyword')?.addEventListener('input', () => admin.applyEmployeeFilters());
      $('#btnEmpFilterClear')?.addEventListener('click', () => admin.clearEmployeeFilters());
      $('#btnDeptSave')?.addEventListener('click', () => admin.saveDepartment());
      $('#btnDeptCancel')?.addEventListener('click', () => admin.resetDeptForm());
      $('#deptTable')?.addEventListener('click', async (e) => {
        const t = e.target;
        const eid = t.getAttribute('data-dept-e');
        const did = t.getAttribute('data-dept-d');
        if (eid) await admin.startEditDepartment(eid);
        if (did) await admin.deleteDepartment(did);
      });

      $('#btnLtSave')?.addEventListener('click', () => admin.saveLeaveType());
      $('#btnLtCancel')?.addEventListener('click', () => admin.resetLtForm());
      $('#ltTable')?.addEventListener('click', async (e) => {
        const t = e.target;
        const eid = t.getAttribute('data-lt-e');
        const did = t.getAttribute('data-lt-d');
        if (eid) await admin.startEditLeaveType(eid);
        if (did) await admin.deleteLeaveType(did);
      });

      // Hiển thị chi tiết log vào/ra của nhân viên trong đúng ngày đã chọn
      $('#myCalTable')?.addEventListener('click', async (e) => {
        const btn = e.target?.closest('button[data-cal-detail-emp]');
        if (!btn) return;
        const empId = btn.getAttribute('data-cal-detail-emp');
        const date = btn.getAttribute('data-cal-detail-date');
        if (!empId || !date) return;

        const tr = btn.closest('tr');
        if (!tr) return;

        // Toggle (mở/đóng)
        const next = tr.nextElementSibling;
        if (next && next.classList && next.classList.contains('cal-detail-row')) {
          next.remove();
          return;
        }

        try {
          ui.showMsg($('#myCalMsg'), 'Đang tải log...', true);
          const url =
            '/api/attendance/history?' +
            util.padRange(date, date) +
            '&employeeId=' +
            encodeURIComponent(empId);
          const rows = await window.Api.api(url);

          const logsHtml = (Array.isArray(rows) ? rows : [])
            .map((r, i) => {
              const ci = r.checkIn ? new Date(r.checkIn).toLocaleString('vi-VN') : '—';
              const co = r.checkOut ? new Date(r.checkOut).toLocaleString('vi-VN') : 'đang trong ca';
              const st = r.status ? ` · ${r.status}` : '';
              return `${i + 1}) Vào: ${ci} | Ra: ${co}${st}`;
            })
            .join('<br/>');

          const detailTr = document.createElement('tr');
          detailTr.className = 'cal-detail-row';
          detailTr.innerHTML = `<td colspan="8">
              <div style="padding:10px 0; font-size:12px; color:#334155; line-height:1.5">
                <strong>Log in/out</strong> (${date})
                <div class="hint" style="margin:4px 0 0">Tổng lượt: ${(Array.isArray(rows) ? rows.length : 0)}</div>
                <div>${logsHtml || '—'}</div>
              </div>
            </td>`;
          tr.parentNode.insertBefore(detailTr, tr.nextSibling);
        } catch (err) {
          ui.showMsg($('#myCalMsg'), err.message, false);
        }
      });

      // Duyệt OT ngay tại màn Có mặt / vắng / nghỉ
      $('#myCalTable')?.addEventListener('change', async (e) => {
        const cb = e.target?.closest('input[type="checkbox"][data-ot-employee-id]');
        if (!cb) return;
        const employeeId = parseInt(cb.getAttribute('data-ot-employee-id'), 10);
        const date = cb.getAttribute('data-ot-date');
        if (!employeeId || !date) return;

        const approved = cb.checked;
        const ok = window.confirm(
          approved
            ? `Duyệt tăng ca cho nhân viên #${employeeId} ngày ${date}?`
            : `Hủy duyệt tăng ca cho nhân viên #${employeeId} ngày ${date}?`
        );
        if (!ok) {
          cb.checked = !approved;
          return;
        }
        try {
          cb.disabled = true;
          ui.showMsg($('#myCalMsg'), 'Đang cập nhật OT...', true);
          await window.Api.api('/api/overtime/approve', {
            method: 'POST',
            json: { employeeId, date, approved },
          });
          ui.showMsg(
            $('#myCalMsg'),
            approved ? 'Đã duyệt tăng ca' : 'Đã bỏ duyệt tăng ca',
            true
          );
          // Đồng bộ ngay bảng "Danh sách tăng ca" để thấy thay đổi tức thì.
          if ($('#otDay')) {
            $('#otDay').value = date;
            await overtimeLogs.loadDay();
          }
        } catch (err) {
          ui.showMsg($('#myCalMsg'), err.message, false);
          cb.checked = !approved;
        } finally {
          cb.disabled = false;
        }
      });
    },
    async init() {
      shell.loadApiBase();
      this.bindEvents();
      if ((localStorage.getItem('accessToken') || localStorage.getItem('token')) && state.user) {
        shell.showApp();
        await this.afterLogin();
      } else {
        shell.showLogin();
      }
    },
  };

  document.addEventListener('DOMContentLoaded', () => app.init());
})();
