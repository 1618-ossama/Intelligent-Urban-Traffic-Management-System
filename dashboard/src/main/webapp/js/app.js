/* ═══════════════════════════════════════════════════════════════
   IUTMS Dashboard - Main Application Logic
   Polls central engine REST API and updates UI in real-time
   ═══════════════════════════════════════════════════════════════ */

const API_BASE = window.location.hostname === 'localhost'
    ? 'http://localhost:8080/api'
    : `http://${window.location.hostname}:8080/api`;

const POLL_INTERVAL = 5000; // 5 seconds

// ── State ──
let flowHistory = [];
let pollutionHistory = [];
let lastUpdated = null;
let geography = null;  // ← Load from API
let ZONE_IDS = [];     // ← Set from API
let ROAD_IDS = [];     // ← Set from API

function updateLastUpdatedDisplay() {
    if (lastUpdated === null) return;
    const seconds = Math.floor((Date.now() - lastUpdated) / 1000);
    document.getElementById('last-updated').textContent = 'Last updated: ' + seconds + 's ago';
}
setInterval(updateLastUpdatedDisplay, 1000);

// ── Clock ──
function updateClock() {
    document.getElementById('clock').textContent =
        new Date().toLocaleTimeString('en-US', { hour12: false });
}
setInterval(updateClock, 1000);
updateClock();

// ── Charts Setup ──
const flowCtx = document.getElementById('flowChart').getContext('2d');
const flowChart = new Chart(flowCtx, {
    type: 'line',
    data: {
        labels: [],
        datasets: [
            { label: 'Road A', data: [], borderColor: '#2196F3', tension: 0.3, fill: false },
            { label: 'Road B', data: [], borderColor: '#4CAF50', tension: 0.3, fill: false },
            { label: 'Road C', data: [], borderColor: '#FF9800', tension: 0.3, fill: false },
            { label: 'Road D', data: [], borderColor: '#f44336', tension: 0.3, fill: false },
        ]
    },
    options: {
        responsive: true,
        animation: { duration: 300 },
        scales: {
            x: { ticks: { color: '#8899a6' }, grid: { color: '#2d3d4f' } },
            y: { ticks: { color: '#8899a6' }, grid: { color: '#2d3d4f' },
                 title: { display: true, text: 'Vehicles/min', color: '#8899a6' } }
        },
        plugins: {
            legend: { labels: { color: '#e0e6ed' } },
        }
    }
});

const pollCtx = document.getElementById('pollutionChart').getContext('2d');
const pollutionChart = new Chart(pollCtx, {
    type: 'bar',
    data: {
        labels: ['Zone Center', 'Zone North', 'Zone South', 'Zone Industrial'],
        datasets: [
            { label: 'CO₂ (ppm)', data: [0, 0, 0, 0], backgroundColor: 'rgba(244,67,54,0.6)' },
            { label: 'NOx (ppb)', data: [0, 0, 0, 0], backgroundColor: 'rgba(255,193,7,0.6)' },
            { label: 'PM2.5 (µg/m³)', data: [0, 0, 0, 0], backgroundColor: 'rgba(33,150,243,0.6)' },
        ]
    },
    options: {
        responsive: true,
        animation: { duration: 300 },
        scales: {
            x: { ticks: { color: '#8899a6' }, grid: { color: '#2d3d4f' } },
            y: { ticks: { color: '#8899a6' }, grid: { color: '#2d3d4f' } }
        },
        plugins: { legend: { labels: { color: '#e0e6ed' } } }
    }
});

// ── Simulated Data (fallback when API is not available) ──
function generateSimData() {
    const roads = ROAD_IDS.length > 0 ? ROAD_IDS : ['road-A', 'road-B', 'road-C', 'road-D'];
    const zones = ZONE_IDS.length > 0 ? ZONE_IDS : ['zone-center', 'zone-north', 'zone-south', 'zone-industrial'];
    const signals = geography?.intersections || ['INT-01', 'INT-02', 'INT-03', 'INT-04', 'INT-05', 'INT-06'];
    const colors = ['GREEN', 'RED', 'GREEN', 'RED', 'GREEN', 'YELLOW'];

    return {
        traffic: roads.map(r => ({
            roadId: r,
            vehicleCount: Math.floor(20 + Math.random() * 150),
            flowRate: Math.round((30 + Math.random() * 120) * 10) / 10
        })),
        pollution: zones.map(z => ({
            zoneId: z,
            co2: Math.round((300 + Math.random() * 200) * 10) / 10,
            nox: Math.round((10 + Math.random() * 90) * 10) / 10,
            pm25: Math.round((5 + Math.random() * 45) * 10) / 10
        })),
        signals: signals.slice(0, signals.length).map((s, i) => ({
            intersectionId: s,
            color: colors[i % colors.length],
            durationSec: 30 + Math.floor(Math.random() * 30)
        })),
        alerts: [
            { alertType: 'CONGESTION', zoneId: zones[0], severity: 'HIGH',
              message: 'Congestion on ' + roads[0] + ': 125 veh/min', createdAt: new Date().toISOString() },
            { alertType: 'POLLUTION', zoneId: zones[1], severity: 'MEDIUM',
              message: 'CO2 level elevated: 450 ppm', createdAt: new Date().toISOString() },
        ],
        recommendations: [
            { actionType: 'EXTEND_GREEN', description: 'Extend green light at INT-01 by 15s', status: 'PENDING' },
            { actionType: 'REDUCE_TRAFFIC', description: 'Divert traffic from ' + zones[1], status: 'PENDING' },
        ]
    };
}

// ── API Fetch ──
async function fetchGeography() {
    try {
        const response = await fetch(`${API_BASE}/geography`);
        if (response.ok) {
            geography = await response.json();
            ZONE_IDS = geography.zones || [];
            ROAD_IDS = geography.roads || [];
            console.log('Geography loaded: ' + ZONE_IDS.length + ' zones, ' + ROAD_IDS.length + ' roads');
            return true;
        }
    } catch (e) {
        console.log('Failed to fetch geography: ' + e.message);
    }
    return false;
}

async function fetchDashboardData() {
    try {
        const response = await fetch(`${API_BASE}/dashboard`);
        if (response.ok) {
            const data = await response.json();
            updateStatus(true);
            lastUpdated = Date.now();
            return data;
        }
    } catch (e) {
        // API not available, use simulated data
    }
    updateStatus(false);
    return null;
}

async function fetchAlerts() {
    try {
        const response = await fetch(`${API_BASE}/alerts`);
        if (response.ok) return await response.json();
    } catch (e) {}
    return null;
}

async function fetchRecommendations() {
    try {
        const response = await fetch(`${API_BASE}/recommendations`);
        if (response.ok) return await response.json();
    } catch (e) {}
    return null;
}

async function fetchZoneSummary() {
    try {
        const response = await fetch(`${API_BASE}/zones`);
        if (response.ok) return await response.json();
    } catch (e) {}
    return null;
}

function renderZoneCards(zones) {
    const container = document.getElementById('zone-cards');
    const zoneList = ZONE_IDS.length > 0 ? ZONE_IDS : ['zone-center', 'zone-north', 'zone-south', 'zone-industrial'];
    const data = zones || {};
    container.innerHTML = zoneList.map(zoneId => {
        const z = data[zoneId] || {};
        const alertCount = z.alertCount || 0;
        return `<div class="zone-card">
            <div class="zone-name">${zoneId}</div>
            <div class="zone-metric">Noise: ${z.noise !== undefined ? z.noise.toFixed(1) + ' dB' : '--'}</div>
            <div class="zone-metric">Flow: ${z.flowRate !== undefined ? z.flowRate.toFixed(1) + ' v/min' : '--'}</div>
            <div class="zone-metric">CO\u2082: ${z.co2 !== undefined ? z.co2.toFixed(1) + ' ppm' : '--'}</div>
            <div class="zone-metric">Alerts: <span class="${alertCount > 0 ? 'alert-badge' : ''}">${alertCount}</span></div>
        </div>`;
    }).join('');
}

function getSeverityClass(alert) {
    const pct = alert.percentOfThreshold;
    if (pct === undefined || pct === null) return 'severity-yellow';
    if (pct > 150) return 'severity-red';
    if (pct >= 100) return 'severity-orange';
    return 'severity-yellow';
}

const acknowledgedIds = new Set();     // session-scoped
const acknowledgedAlerts = new Map();  // id -> alert object for session persistence

async function acknowledgeAlert(id) {
    const alertEl = document.querySelector(`[data-alert-id="${id}"]`);
    if (alertEl) alertEl.classList.add('acknowledged');
    try {
        await fetch(`${API_BASE}/alerts/${id}/acknowledge`, { method: 'POST' });
    } catch (e) {}
    acknowledgedIds.add(id);
}

function renderAlertsWithAck(alerts) {
    const list = document.getElementById('alert-list');
    // Keep acknowledged alerts visible from previous polls
    (alerts || []).forEach(a => { if (!acknowledgedIds.has(a.id)) acknowledgedAlerts.delete(a.id); });

    const active = (alerts || []).filter(a => !acknowledgedIds.has(a.id));
    const acked  = Array.from(acknowledgedAlerts.values());

    if (active.length === 0 && acked.length === 0) {
        list.innerHTML = '<p class="empty-state">No active alerts</p>';
        return;
    }

    const renderOne = (a, isAcked) => {
        const cls = isAcked ? 'acknowledged' : getSeverityClass(a);
        return `<div class="alert-item ${cls}" data-alert-id="${a.id}">
            <div class="alert-type">${a.alertType} \u2014 ${a.severity}</div>
            <div class="alert-msg">${a.message}</div>
            <div class="alert-time">${a.createdAt || 'just now'} \xb7 ${a.zoneId}</div>
            ${!isAcked ? `<button class="ack-btn" onclick="acknowledgeAlert(${a.id})">Acknowledge</button>`
                       : '<span class="ack-label">Acknowledged</span>'}
        </div>`;
    };

    list.innerHTML = [
        ...active.slice(0, 8).map(a => renderOne(a, false)),
        ...acked.slice(0, 2).map(a => renderOne(a, true))
    ].join('');
}

// ── UI Update Functions ──
function updateStatus(connected) {
    const dot = document.getElementById('status-indicator');
    const text = document.getElementById('status-text');
    dot.className = connected ? 'status-dot green' : 'status-dot red';
    text.textContent = connected ? 'Connected' : 'Simulated Mode';
}

function updateTrafficGrid(trafficData) {
    const grid = document.getElementById('traffic-grid');
    grid.innerHTML = trafficData.map(t => {
        const status = t.flowRate > 100 ? 'congested' : t.flowRate > 60 ? 'moderate' : '';
        return `<div class="road-card ${status}">
            <div class="road-name">${t.roadId}</div>
            <div class="road-count">${t.vehicleCount} 🚗</div>
            <div class="road-rate">${t.flowRate} vehicles/min</div>
        </div>`;
    }).join('');
}

function updateSignalsGrid(signalData) {
    const grid = document.getElementById('signals-grid');
    const icons = { GREEN: '🟢', RED: '🔴', YELLOW: '🟡' };
    grid.innerHTML = signalData.map(s => `
        <div class="signal-card">
            <div class="signal-light">${icons[s.color] || '⚪'}</div>
            <div class="signal-id">${s.intersectionId}</div>
            <div class="signal-duration">${s.durationSec}s</div>
        </div>
    `).join('');
}

function updateKPIs(traffic, pollution, alerts) {
    const totalFlow = traffic.reduce((sum, t) => sum + t.flowRate, 0);
    const avgCO2 = pollution.reduce((sum, p) => sum + p.co2, 0) / pollution.length;
    const avgNoise = Math.round(50 + Math.random() * 40);

    document.getElementById('total-vehicles').textContent = Math.round(totalFlow);
    document.getElementById('avg-pollution').textContent = Math.round(avgCO2);
    document.getElementById('avg-noise').textContent = avgNoise;
    document.getElementById('active-alerts').textContent = alerts.length;

    // Color code KPIs
    document.getElementById('kpi-vehicles').style.borderColor =
        totalFlow > 400 ? '#f44336' : totalFlow > 200 ? '#FF9800' : '#4CAF50';
    document.getElementById('kpi-pollution').style.borderColor =
        avgCO2 > 400 ? '#f44336' : avgCO2 > 350 ? '#FF9800' : '#4CAF50';
}

function updateFlowChart(traffic) {
    const time = new Date().toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' });
    flowChart.data.labels.push(time);
    traffic.forEach((t, i) => {
        if (flowChart.data.datasets[i]) {
            flowChart.data.datasets[i].data.push(t.flowRate);
        }
    });
    // Keep last 20 data points
    if (flowChart.data.labels.length > 20) {
        flowChart.data.labels.shift();
        flowChart.data.datasets.forEach(ds => ds.data.shift());
    }
    flowChart.update();
}

function updatePollutionChart(pollution) {
    pollution.forEach((p, i) => {
        pollutionChart.data.datasets[0].data[i] = p.co2;
        pollutionChart.data.datasets[1].data[i] = p.nox;
        pollutionChart.data.datasets[2].data[i] = p.pm25;
    });
    pollutionChart.update();
}

function updateAlerts(alerts) {
    const list = document.getElementById('alert-list');
    if (!alerts || alerts.length === 0) {
        list.innerHTML = '<p class="empty-state">No active alerts</p>';
        return;
    }
    list.innerHTML = alerts.slice(0, 10).map(a => `
        <div class="alert-item ${a.severity}">
            <div class="alert-type">${a.alertType} — ${a.severity}</div>
            <div class="alert-msg">${a.message}</div>
            <div class="alert-time">${a.createdAt || 'just now'} · ${a.zoneId}</div>
        </div>
    `).join('');
}

function updateRecommendations(recs) {
    const list = document.getElementById('rec-list');
    if (!recs || recs.length === 0) {
        list.innerHTML = '<p class="empty-state">No recommendations</p>';
        return;
    }
    list.innerHTML = recs.slice(0, 10).map(r => `
        <div class="rec-item">
            <div class="rec-action">💡 ${r.actionType} [${r.status}]</div>
            <div class="alert-msg">${r.description}</div>
        </div>
    `).join('');
}

// ── Main Polling Loop ──
async function poll() {
    const apiData = await fetchDashboardData();
    const zoneSummary = await fetchZoneSummary();

    // Use API data or fall back to simulation
    const sim = generateSimData();
    const traffic  = (apiData && apiData.traffic  && apiData.traffic.length  > 0) ? apiData.traffic  : sim.traffic;
    const pollution = (apiData && apiData.pollution && apiData.pollution.length > 0) ? apiData.pollution : sim.pollution;
    const signals  = (apiData && apiData.signals  && apiData.signals.length  > 0) ? apiData.signals  : sim.signals;
    const alerts = apiData ? (apiData.alerts || []) : sim.alerts;
    const recs = apiData ? (apiData.recommendations || []) : sim.recommendations;

    // Track current active alerts for acknowledged rendering
    (alerts || []).forEach(a => { if (!acknowledgedIds.has(a.id)) { /* keep in active */ } else { acknowledgedAlerts.set(a.id, a); } });

    updateTrafficGrid(traffic);
    updateSignalsGrid(signals);
    updateKPIs(traffic, pollution, alerts);
    updateFlowChart(traffic);
    updatePollutionChart(pollution);
    renderAlertsWithAck(alerts);
    updateRecommendations(recs);
    renderZoneCards(zoneSummary);
}

async function initialize() {
    const geoLoaded = await fetchGeography();
    if (!geoLoaded) {
        console.log('Geography API not available, using defaults');
    }
    poll();
    setInterval(poll, POLL_INTERVAL);
    console.log('IUTMS Dashboard initialized. Polling every ' + POLL_INTERVAL + 'ms');
}

initialize();
