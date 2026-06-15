let stompClient = null;
let reconnectTimer = null;

function connectWebSocket() {
  const socket = new SockJS('/ws');
  stompClient = new StompJs.Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,
    onConnect: onConnected,
    onDisconnect: onDisconnected,
    onStompError: onDisconnected
  });
  stompClient.activate();
}

function onConnected() {
  setWsStatus(true);

  stompClient.subscribe('/topic/sequence', (message) => {
    const flights = JSON.parse(message.body);
    renderTable(flights);
    updateStats();
  });

  stompClient.subscribe('/topic/conflicts', (message) => {
    const conflictFlights = JSON.parse(message.body);
    renderAlerts(conflictFlights);
    document.getElementById('stat-conflicts').textContent = conflictFlights.length;
    if (conflictFlights.length > 0) {
      showToast(`${conflictFlights.length} conflit(s) détecté(s) !`, 'danger');
    }
  });

  stompClient.subscribe('/topic/alerts', (message) => {
    showToast(message.body, 'warning');
  });
}

function onDisconnected() {
  setWsStatus(false);
}

function setWsStatus(connected) {
  const el = document.getElementById('ws-indicator');
  if (connected) {
    el.className = 'ws-badge ws-connected';
    el.innerHTML = '<span class="dot"></span> WebSocket connecté';
  } else {
    el.className = 'ws-badge ws-disconnected';
    el.innerHTML = '<span class="dot"></span> WebSocket déconnecté';
  }
}

connectWebSocket();
