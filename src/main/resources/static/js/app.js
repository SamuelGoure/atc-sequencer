const API = '';
let allFlights = [];
let conflicts = [];
let addModal;

document.addEventListener('DOMContentLoaded', () => {
  addModal = new bootstrap.Modal(document.getElementById('addFlightModal'));
  refresh();
  setInterval(refresh, 10000);
});

async function refresh() {
  await Promise.all([loadSequence(), loadConflicts()]);
  updateLastUpdate();
}

async function loadSequence() {
  try {
    const res = await fetch(`${API}/api/sequence`);
    allFlights = await res.json();
    renderTable(allFlights);
    updateStats();
  } catch (e) {
    console.error('Erreur chargement séquence', e);
  }
}

async function loadConflicts() {
  try {
    const res = await fetch(`${API}/api/sequence/conflicts`);
    conflicts = await res.json();
    renderAlerts(conflicts);
    document.getElementById('stat-conflicts').textContent = conflicts.length;
  } catch (e) {
    console.error('Erreur chargement conflits', e);
  }
}

function renderTable(flights) {
  const tbody = document.getElementById('flights-table');
  if (!flights.length) {
    tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted py-4">Aucun vol en séquence</td></tr>';
    return;
  }
  tbody.innerHTML = flights.map((f, i) => `
    <tr>
      <td><span class="seq-num">${f.sequenceNumber ?? i + 1}</span></td>
      <td><strong>${f.flightId}</strong><br><small class="text-muted">${f.callSign ?? ''}</small></td>
      <td>${f.origin ?? '—'} <i class="bi bi-arrow-right text-muted"></i> ${f.destination ?? '—'}</td>
      <td>${categoryBadge(f.category)}</td>
      <td>${delayBadge(f.delayMinutes)}</td>
      <td>${statusBadge(f.status)}</td>
      <td><span class="runway-badge">${f.runway ?? '—'}</span></td>
      <td>
        <div class="d-flex gap-1">
          <select class="form-select form-select-sm bg-secondary text-white border-0"
            style="width:130px;font-size:0.75rem"
            onchange="updateStatus('${f.flightId}', this.value)">
            ${[
              ['SCHEDULED', 'Planifié'],
              ['ACTIVE',    'En approche'],
              ['DELAYED',   'En retard'],
              ['LANDED',    'Atterri'],
              ['CANCELLED', 'Annulé']
            ].map(([val, label]) =>
              `<option value="${val}" ${f.status === val ? 'selected' : ''}>${label}</option>`
            ).join('')}
          </select>
          <button class="btn btn-danger btn-xs" onclick="deleteFlight('${f.flightId}')">
            <i class="bi bi-trash"></i>
          </button>
        </div>
      </td>
    </tr>
  `).join('');
}

function renderAlerts(conflictFlights) {
  const panel = document.getElementById('alerts-panel');
  let html = '';

  if (conflictFlights.length) {
    const pairs = groupConflicts(conflictFlights);
    pairs.forEach(pair => {
      html += `<div class="alert-item conflict pulse">
        <i class="bi bi-exclamation-triangle-fill me-2 text-danger"></i>
        <strong>Conflit piste ${pair[0].runway}</strong><br>
        <small>${pair.map(f => f.flightId).join(' ↔ ')}</small>
      </div>`;
    });
  }

  allFlights
    .filter(f => f.delayMinutes > 30)
    .forEach(f => {
      html += `<div class="alert-item warning">
        <i class="bi bi-clock-fill me-2 text-warning"></i>
        <strong>${f.flightId}</strong> — retard critique : ${f.delayMinutes} min
      </div>`;
    });

  panel.innerHTML = html || '<p class="text-muted text-center mt-3"><i class="bi bi-check-circle me-1"></i>Aucune alerte</p>';
}

function groupConflicts(flights) {
  const groups = {};
  flights.forEach(f => {
    if (!groups[f.runway]) groups[f.runway] = [];
    groups[f.runway].push(f);
  });
  return Object.values(groups);
}

function updateStats() {
  document.getElementById('stat-total').textContent =
    allFlights.filter(f => f.status === 'ACTIVE' || f.status === 'SCHEDULED' || f.status === 'DELAYED').length;
  document.getElementById('stat-delayed').textContent =
    allFlights.filter(f => f.delayMinutes > 0).length;
  document.getElementById('stat-landed').textContent =
    allFlights.filter(f => f.status === 'LANDED').length;
}

async function resequence() {
  try {
    const res = await fetch(`${API}/api/sequence/resequence`, { method: 'POST' });
    const result = await res.json();
    renderTable(result);
    showToast('Reséquencement effectué', 'success');
  } catch (e) {
    showToast('Erreur reséquencement', 'danger');
  }
}

async function updateStatus(flightId, status) {
  try {
    await fetch(`${API}/api/flights/${flightId}/status?status=${status}`, { method: 'PUT' });
    await loadSequence();
  } catch (e) {
    showToast('Erreur mise à jour statut', 'danger');
  }
}

async function deleteFlight(flightId) {
  if (!confirm(`Supprimer le vol ${flightId} ?`)) return;
  try {
    await fetch(`${API}/api/flights/${flightId}`, { method: 'DELETE' });
    await refresh();
    showToast(`Vol ${flightId} supprimé`, 'warning');
  } catch (e) {
    showToast('Erreur suppression', 'danger');
  }
}

function openAddModal() {
  document.getElementById('add-flight-form').reset();
  const now = new Date();
  now.setHours(now.getHours() + 2);
  document.getElementById('f-eta').value = now.toISOString().slice(0, 16);
  addModal.show();
}

async function submitAddFlight() {
  const payload = {
    flightId:     document.getElementById('f-id').value.trim().toUpperCase(),
    callSign:     document.getElementById('f-callsign').value.trim(),
    origin:       document.getElementById('f-origin').value.trim().toUpperCase(),
    destination:  document.getElementById('f-dest').value.trim().toUpperCase(),
    eta:          document.getElementById('f-eta').value || null,
    category:     document.getElementById('f-category').value,
    runway:       document.getElementById('f-runway').value.trim().toUpperCase(),
    delayMinutes: parseInt(document.getElementById('f-delay').value) || 0
  };

  if (!payload.flightId || !payload.callSign || !payload.origin || !payload.destination) {
    showToast('Remplis tous les champs obligatoires', 'warning');
    return;
  }

  try {
    const res = await fetch(`${API}/api/flights`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    if (res.status === 201) {
      addModal.hide();
      await refresh();
      showToast(`Vol ${payload.flightId} ajouté`, 'success');
    } else {
      showToast('Erreur : vol déjà existant ?', 'danger');
    }
  } catch (e) {
    showToast('Erreur connexion API', 'danger');
  }
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  const id = 'toast-' + Date.now();
  const colors = { success: '#3fb950', danger: '#f85149', warning: '#f0883e', info: '#58a6ff' };
  const toast = document.createElement('div');
  toast.id = id;
  toast.style.cssText = `background:#161b22;border:1px solid ${colors[type]};color:#e6edf3;
    padding:12px 16px;border-radius:8px;margin-bottom:8px;font-size:0.9rem;
    animation:fadeIn 0.3s ease;min-width:250px`;
  toast.innerHTML = `<i class="bi bi-info-circle me-2" style="color:${colors[type]}"></i>${message}`;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 3500);
}

function updateLastUpdate() {
  const el = document.getElementById('last-update');
  if (el) el.textContent = 'Mis à jour : ' + new Date().toLocaleTimeString('fr-FR');
}

function categoryBadge(cat) {
  const map = {
    MEDICAL:    ['badge-medical',    'bi-heart-pulse', 'MED'],
    MILITARY:   ['badge-military',   'bi-shield-fill', 'MIL'],
    CARGO:      ['badge-cargo',      'bi-box-seam',    'CARGO'],
    COMMERCIAL: ['badge-commercial', 'bi-airplane',    'COM']
  };
  const [cls, icon, label] = map[cat] || ['badge-cargo', 'bi-question', cat];
  return `<span class="cat-badge ${cls}"><i class="bi ${icon} me-1"></i>${label}</span>`;
}

function statusBadge(status) {
  const labels = {
    SCHEDULED: 'Planifié',
    ACTIVE:    'En approche',
    DELAYED:   'En retard',
    LANDED:    'Atterri',
    CANCELLED: 'Annulé'
  };
  return `<span class="status-badge status-${status}">${labels[status] ?? status}</span>`;
}

function delayBadge(minutes) {
  if (!minutes || minutes === 0) return '<span class="delay-low">—</span>';
  if (minutes > 30) return `<span class="delay-critical"><i class="bi bi-clock-fill me-1"></i>${minutes} min</span>`;
  if (minutes > 15) return `<span class="delay-medium">${minutes} min</span>`;
  return `<span class="delay-low">${minutes} min</span>`;
}
