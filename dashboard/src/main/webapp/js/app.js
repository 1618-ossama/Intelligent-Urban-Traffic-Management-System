/* ═══════════════════════════════════════════════════════════════
   IUTMS Dashboard - Main Application Logic
   Polls central engine REST API and updates UI in real-time
   ═══════════════════════════════════════════════════════════════ */

const API_BASE = window.location.hostname === 'localhost'
    ? 'http://localhost:8080/api'
    : `http://${window.location.hostname}:8080/api`;

const POLL_INTERVAL = 3000; // 3 seconds

// ── State ──
let flowHistory = [];
let pollutionHistory = [];

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
        labels: ['Zone Center', 'Zone North', 'Zone South', 'Zone East'],
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
    const roads = ['road-A', 'road-B', 'road-C', 'road-D'];
    const zones = ['zone-center', 'zone-north', 'zone-south', 'zone-east'];
    const signals = ['INT-01', 'INT-02', 'INT-03', 'INT-04', 'INT-05', 'INT-06'];
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
        signals: signals.map((s, i) => ({
            intersectionId: s,
            color: colors[i],
            durationSec: 30 + Math.floor(Math.random() * 30)
        })),
        alerts: [
            { alertType: 'CONGESTION', zoneId: 'zone-center', severity: 'HIGH',
              message: 'Congestion on road-A: 125 veh/min', createdAt: new Date().toISOString() },
            { alertType: 'POLLUTION', zoneId: 'zone-north', severity: 'MEDIUM',
              message: 'CO2 level elevated: 450 ppm', createdAt: new Date().toISOString() },
        ],
        recommendations: [
            { actionType: 'EXTEND_GREEN', description: 'Extend green light at INT-01 by 15s', status: 'PENDING' },
            { actionType: 'REDUCE_TRAFFIC', description: 'Divert traffic from zone-north', status: 'PENDING' },
        ]
    };
}

// ── API Fetch ──
async function fetchDashboardData() {
    try {
        const response = await fetch(`${API_BASE}/dashboard`);
        if (response.ok) {
            const data = await response.json();
            updateStatus(true);
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

    // Use API data or fall back to simulation
    const sim = generateSimData();
    const traffic = sim.traffic;
    const pollution = sim.pollution;
    const signals = sim.signals;
    const alerts = apiData ? (apiData.alerts || []) : sim.alerts;
    const recs = apiData ? (apiData.recommendations || []) : sim.recommendations;

    updateTrafficGrid(traffic);
    updateSignalsGrid(signals);
    updateKPIs(traffic, pollution, alerts);
    updateFlowChart(traffic);
    updatePollutionChart(pollution);
    updateAlerts(alerts);
    updateRecommendations(recs);
}

// ── Start ──
poll();
setInterval(poll, POLL_INTERVAL);
console.log('IUTMS Dashboard initialized. Polling every ' + POLL_INTERVAL + 'ms');
