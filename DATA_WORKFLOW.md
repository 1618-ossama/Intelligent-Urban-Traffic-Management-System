# IUTMS Data Workflow Architecture

## Overview
The Intelligent Urban Traffic Management System processes multi-source sensor data through a Kafka event streaming pipeline to generate real-time traffic alerts and recommendations.

---

## Data Flow Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    SENSOR DATA SOURCES                           │
├─────────────────────────────────────────────────────────────────┤
│  • Cameras        → Camera Events (accidents, anomalies)         │
│  • Flow Sensors   → Vehicle Flow (count, speed, rate)            │
│  • Pollution      → Pollution Levels (CO2, NOx, PM2.5)          │
│  • Noise Sensors  → Noise Readings (decibel levels)             │
│  • Traffic Lights → Signal States (RED/GREEN/YELLOW)           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    KAFKA MESSAGE BROKER                          │
├─────────────────────────────────────────────────────────────────┤
│  Topics:                                                         │
│  • camera-events          (service-camera)                       │
│  • vehicle-flow           (service-flux-vehicules)              │
│  • pollution-data         (service-pollution)                    │
│  • noise-data             (service-bruit)                        │
│  • signal-states          (service-feux)                         │
│                                                                   │
│  High-throughput, distributed, fault-tolerant messaging         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                  CENTRAL ENGINE ANALYTICS                        │
├─────────────────────────────────────────────────────────────────┤
│  Analysis Components:                                            │
│  1. Congestion Detection                                        │
│     └─ Correlate vehicle_flow + camera_events                  │
│  2. Pollution Analysis                                          │
│     └─ Trend pollution_data across zones                        │
│  3. Accident Detection                                          │
│     └─ HIGH severity camera_events                              │
│  4. Noise Monitoring                                            │
│     └─ Track noise_data vs thresholds                           │
│  5. Pattern Recognition                                         │
│     └─ Historical analysis for rush hour prediction             │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    ALERT GENERATION                              │
├─────────────────────────────────────────────────────────────────┤
│  Alert Types (stored in alerts table):                          │
│  • CONGESTION   → triggered when flow_rate > threshold         │
│  • POLLUTION    → triggered when pollutant levels high         │
│  • ACCIDENT     → triggered when event_type = ACCIDENT         │
│  • NOISE        → triggered when decibel_level exceeds limit   │
│  • RUSH_HOUR    → triggered by time patterns                   │
│                                                                   │
│  Severity Levels: LOW | MEDIUM | HIGH | CRITICAL               │
│  Active Status: is_active = TRUE until resolved_at set         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                 RECOMMENDATION ENGINE                            │
├─────────────────────────────────────────────────────────────────┤
│  Actions Generated (linked to alerts):                          │
│  • EXTEND_GREEN         → Extend green signal timing           │
│  • REDUCE_TRAFFIC       → Implement alternate routes           │
│  • DEVIATE              → Suggest route diversions             │
│  • SYNC_SIGNALS         → Coordinate signal timing             │
│  • OPEN_LANE            → Activate additional lanes            │
│  • PRIORITIZE_PUBLIC    → Favor public transport               │
│                                                                   │
│  Status Tracking: PENDING → APPLIED | DISMISSED               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                      DASHBOARD UI                                │
├─────────────────────────────────────────────────────────────────┤
│  Real-time visualization of:                                    │
│  • Zone health (aggregated metrics)                             │
│  • Active alerts (by severity)                                  │
│  • Recommendations (pending/applied)                            │
│  • Historical trends (charts/analytics)                         │
│  • Signal state visualization                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Schema: Dimension vs Fact Tables

### Dimension Tables (Master Data - Low Cardinality)
These tables store reference/configuration data that changes infrequently:

| Table | Purpose | Cardinality |
|-------|---------|-------------|
| **zones** | Geographic areas being monitored | ~10-100 zones |
| **intersections** | Traffic intersections within zones | ~50-500 per city |
| **cameras** | Camera sensors deployed at locations | ~100-1000+ sensors |

**Key characteristics:**
- Rarely updated after initial setup
- Used for JOINs and aggregation
- Support spatial queries (latitude, longitude)
- Include metadata (names, descriptions, status)

### Fact Tables (Time-Series Data - High Cardinality)
These tables store measurements and events with timestamps (partitioned by year):

| Table | Records/Day | Partition Strategy | Retention |
|-------|-------------|-------------------|-----------|
| **vehicle_flow** | ~100K-1M | BY YEAR(recorded_at) | 2+ years |
| **pollution_data** | ~10K-100K | BY YEAR(recorded_at) | 2+ years |
| **camera_events** | ~1K-10K | BY YEAR(recorded_at) | 2+ years |
| **noise_data** | ~10K-100K | BY YEAR(recorded_at) | 2+ years |
| **signal_states** | ~100K-500K | BY YEAR(recorded_at) | 1-2 years |

**Key characteristics:**
- High volume, append-only workload
- Partitioned for efficient query pruning
- Indexed for zone/time-based queries
- Data validation with CHECK constraints
- Foreign key references to dimension tables

### Control Tables

| Table | Purpose | Lifecycle |
|-------|---------|-----------|
| **alerts** | Generated alerts (ACTIVE until resolved) | Partitioned by YEAR |
| **recommendations** | Actionable recommendations linked to alerts | Current state |

---

## Data Pipeline Flow: Step-by-Step

### 1. **Data Ingestion (Sensor → Kafka)**
```
Camera Service
├─ Captures vehicle, accident, anomaly events
└─ Publishes to Kafka topic: camera-events

Vehicle Flow Service
├─ Aggregates vehicle counts from sensors
└─ Publishes to Kafka topic: vehicle-flow

Pollution Service
├─ Collects CO2, NOx, PM2.5 measurements
└─ Publishes to Kafka topic: pollution-data

Noise Service (Bruit)
├─ Records decibel levels from acoustic sensors
└─ Publishes to Kafka topic: noise-data

Traffic Light Service (Feux)
├─ Monitors signal state changes
└─ Publishes to Kafka topic: signal-states
```

### 2. **Stream Processing (Central Engine)**
```
Consumer Groups (consume from Kafka topics):
├─ CameraEventsConsumer  → Writes to camera_events table
├─ VehicleFlowConsumer   → Writes to vehicle_flow table
├─ PollutionConsumer     → Writes to pollution_data table
├─ NoiseConsumer         → Writes to noise_data table
└─ SignalConsumer        → Writes to signal_states table

Analysis Module (reads from database):
├─ CongestionAnalyzer
│  └─ Query: SELECT zone_id, AVG(flow_rate) FROM vehicle_flow
│     GROUP BY zone_id, DATE(recorded_at)
├─ PollutionAnalyzer
│  └─ Query: SELECT zone_id FROM pollution_data 
│     WHERE co2_level > THRESHOLD
├─ AnomalyDetector
│  └─ Query: SELECT * FROM camera_events 
│     WHERE event_type IN ('ACCIDENT','ANOMALY')
├─ NoiseMonitor
│  └─ Query: SELECT zone_id FROM noise_data 
│     WHERE decibel_level > CRITICAL_LEVEL
└─ PatternRecognizer
   └─ Query: SELECT zone_id, HOUR(recorded_at) FROM vehicle_flow
      WHERE DATE(recorded_at) >= DATE_SUB(NOW(), INTERVAL 1 YEAR)
```

### 3. **Alert Generation**
```
Trigger Conditions (written to alerts table):

CONGESTION Alert:
└─ IF flow_rate > zone_threshold AND duration > 5 minutes
   └─ INSERT INTO alerts (alert_type='CONGESTION', severity='HIGH')

POLLUTION Alert:
└─ IF any pollutant level exceeds regulatory standard
   └─ INSERT INTO alerts (alert_type='POLLUTION', severity='MEDIUM')

ACCIDENT Alert:
└─ IF camera_events.event_type='ACCIDENT' AND severity='CRITICAL'
   └─ INSERT INTO alerts (alert_type='ACCIDENT', severity='CRITICAL')

NOISE Alert:
└─ IF decibel_level > 85dB during restricted hours
   └─ INSERT INTO alerts (alert_type='NOISE', severity='LOW')

RUSH_HOUR Alert:
└─ IF historical_pattern detected
   └─ INSERT INTO alerts (alert_type='RUSH_HOUR', severity='MEDIUM')
```

### 4. **Recommendation Generation**
```
For each ACTIVE alert:
├─ CONGESTION + HIGH severity
│  └─ Recommend: EXTEND_GREEN, SYNC_SIGNALS, OPEN_LANE
├─ POLLUTION detected
│  └─ Recommend: REDUCE_TRAFFIC, PRIORITIZE_PUBLIC
├─ ACCIDENT detected
│  └─ Recommend: DEVIATE, REDUCE_TRAFFIC
├─ NOISE during rush hour
│  └─ Recommend: PRIORITIZE_PUBLIC, EXTEND_GREEN for public transport
└─ RUSH_HOUR predicted
   └─ Recommend: SYNC_SIGNALS, EXTEND_GREEN, OPEN_LANE

All written to recommendations table with status='PENDING'
```

### 5. **Dashboard Consumption**
```
Real-time Dashboard Queries:

Zone Overview:
└─ SELECT zones.*, 
          COUNT(DISTINCT alerts.id) AS active_alerts,
          AVG(vehicle_flow.flow_rate) AS avg_traffic
   FROM zones
   LEFT JOIN alerts ON zones.id = alerts.zone_id AND is_active=TRUE
   LEFT JOIN vehicle_flow ON zones.id = vehicle_flow.zone_id
   GROUP BY zones.id

Active Alerts:
└─ SELECT * FROM alerts 
   WHERE is_active = TRUE 
   ORDER BY severity DESC, created_at DESC

Recommendations Pending:
└─ SELECT r.*, a.alert_type, a.zone_id 
   FROM recommendations r
   JOIN alerts a ON r.alert_id = a.id
   WHERE r.status = 'PENDING'

Historical Trend (last 24h):
└─ SELECT DATE_TRUNC('hour', recorded_at) AS hour,
          zone_id,
          AVG(flow_rate) AS traffic_avg,
          AVG(co2_level) AS pollution_avg
   FROM vehicle_flow vf
   JOIN pollution_data pd ON vf.zone_id = pd.zone_id
   WHERE recorded_at >= NOW() - INTERVAL 1 DAY
   GROUP BY hour, zone_id
```

---

## Performance Optimizations

### Partitioning Strategy
- **Time-series fact tables** partitioned by YEAR for:
  - Efficient pruning of old data
  - Parallel query execution
  - Easier archival/deletion of old partitions
  
```sql
PARTITION BY RANGE (YEAR(recorded_at)) (
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p2027 VALUES LESS THAN (2028),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

### Index Strategy
- **Composite indexes** on (zone_id, recorded_at) for typical queries
- **Single indexes** on foreign keys for JOIN performance
- **Status indexes** for filtered queries (is_active, status)
- **Severity indexes** for alert filtering

### Data Validation
- **CHECK constraints** prevent invalid values:
  - `vehicle_count >= 0`
  - `flow_rate >= 0`
  - `decibel_level >= 0`
  - `duration_sec > 0`
  
- **NOT NULL constraints** for critical fields
- **Foreign keys** enforce referential integrity

---

## Concurrency & Consistency

### Write Patterns
- **Fact tables**: High-volume append-only (non-blocking inserts)
- **Alerts**: Moderate writes, selective updates (is_active → FALSE)
- **Recommendations**: Low-volume writes, status updates (PENDING → APPLIED)

### Read Patterns
- **Dashboard**: Aggregation queries with filters (zone_id, date range)
- **Analytics**: Analytical queries (joins across dimensions)
- **Real-time**: Time-window queries (last hour/day)

### Transaction Isolation
- InnoDB default (REPEATABLE READ) suitable for this workload
- Alert + Recommendation must be atomic (same transaction)

---

## Data Quality Checks

### At Ingestion (Kafka Producers)
- Validate sensor readings are within expected ranges
- Reject malformed events
- Add event metadata (source, timestamp, version)

### At Storage (Database)
- CHECK constraints validate numeric ranges
- Foreign keys validate zone/camera existence
- DEFAULT CURRENT_TIMESTAMP for audit trail

### At Query Time (Central Engine)
- Outlier detection (e.g., vehicle_count > 10K in 1 minute?)
- Cross-validate signals (flow_rate vs camera events)
- Deduplicate events within time windows

---

## Scalability Roadmap

### Current Capacity
- ~100K-1M rows/day per fact table
- Query latency: <1s for aggregations, <5s for joins

### Near-term Growth
- **Partitioning** handles seasonal spikes
- **Archival** moves old data to cold storage
- **Caching** (Redis) for hot zone metrics

### Long-term (2027+)
- **Time-series DB** (InfluxDB/Timescale) for telemetry
- **Data warehouse** (Snowflake/BigQuery) for analytics
- **Stream processing** (Spark/Flink) for complex CEP rules

