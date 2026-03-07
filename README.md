# 🚦 IUTMS — Intelligent Urban Traffic Management System

A distributed system for intelligent urban traffic management, built with 5 different Java distributed technologies communicating through Apache Kafka.

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                      Web Dashboard (:3000)                    │
│                        HTML/CSS/JS                            │
└──────────────────────┬───────────────────────────────────────┘
                       │ REST API
┌──────────────────────┴───────────────────────────────────────┐
│              Central Analysis Engine (:8080)                   │
│          Kafka Consumer → Analyzer → MySQL → REST API         │
└──┬──────────┬──────────┬──────────┬──────────┬───────────────┘
   │          │          │          │          │
   ▼          ▼          ▼          ▼          ▼
┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐
│ Flux │  │ Poll │  │ Cam  │  │Noise │  │ Feux │
│JAX-WS│  │JAX-RS│  │ RMI  │  │ TCP  │  │JAX-RPC│
│:8081 │  │:8082 │  │:1099 │  │:9090 │  │:8084 │
└──────┘  └──────┘  └──────┘  └──────┘  └──────┘
              All publish to Apache Kafka (:9092)
```

## Services

| Service | Technology | Port | Kafka Topic |
|---------|-----------|------|-------------|
| ServiceFluxVehicules | JAX-WS (SOAP) | 8081 | traffic-flow |
| ServicePollution | JAX-RS (REST) | 8082 | pollution-data |
| ServiceCamera | Java RMI | 1099 | camera-events |
| ServiceBruit | TCP Socket | 9090 | noise-data |
| ServiceFeuxSignalisation | JAX-RPC (Axis) | 8084 | signal-events |
| Central Engine | Java + Kafka | 8080 | All topics |
| Dashboard | HTML/CSS/JS | 3000 | — |

## Quick Start with Docker

```bash
# Start everything
docker-compose up -d

# View logs
docker-compose logs -f

# Stop
docker-compose down
```

## Manual Build & Run

### Prerequisites
- Java 17+
- Maven 3.9+
- Apache Kafka 3.5+ running on localhost:9092
- MySQL 8.0+ running on localhost:3306

### 1. Create Database
```bash
mysql -u root -p < sql/schema.sql
```

### 2. Create Kafka Topics
```bash
kafka-topics --create --bootstrap-server localhost:9092 --topic traffic-flow --partitions 3
kafka-topics --create --bootstrap-server localhost:9092 --topic pollution-data --partitions 3
kafka-topics --create --bootstrap-server localhost:9092 --topic camera-events --partitions 3
kafka-topics --create --bootstrap-server localhost:9092 --topic noise-data --partitions 3
kafka-topics --create --bootstrap-server localhost:9092 --topic signal-events --partitions 3
```

### 3. Build
```bash
mvn clean package -DskipTests
```

### 4. Run Services (each in a separate terminal)
```bash
java -jar service-flux-vehicules/target/service-flux-vehicules-1.0-SNAPSHOT.jar
java -jar service-pollution/target/service-pollution-1.0-SNAPSHOT.jar
java -jar service-camera/target/service-camera-1.0-SNAPSHOT.jar
java -jar service-bruit/target/service-bruit-1.0-SNAPSHOT.jar
# For service-feux: deploy axis.war to Tomcat on port 8084
java -jar central-engine/target/central-engine-1.0-SNAPSHOT.jar
# Open dashboard/src/main/webapp/index.html in a browser
```

## API Endpoints

### Central Engine REST API (port 8080)
- `GET /api/health` — Health check
- `GET /api/dashboard` — Full dashboard data (alerts + recommendations)
- `GET /api/alerts` — Active alerts
- `GET /api/recommendations` — Recent recommendations

### ServicePollution REST API (port 8082)
- `GET /api/pollution/{zoneId}` — Current pollution for a zone
- `GET /api/pollution/{zoneId}/history?hours=N` — Historical data
- `GET /api/pollution/zones` — All zones data

### ServiceFluxVehicules SOAP (port 8081)
- WSDL: `http://localhost:8081/flux?wsdl`

## Alert Rules

| Condition | Alert Type | Recommendation |
|-----------|-----------|----------------|
| Flow > 100 veh/min | CONGESTION | Extend green light |
| CO₂ > 400 ppm or PM2.5 > 35 µg/m³ | POLLUTION | Reduce traffic |
| Camera detects accident | ACCIDENT | Traffic deviation |
| Noise > 85 dB | NOISE | — |

## Project Structure

```
iutms/
├── pom.xml                    # Parent POM
├── docker-compose.yml         # Full-stack deployment
├── sql/                       # Database scripts
├── common/                    # Shared library (Kafka, JSON, models)
├── service-flux-vehicules/    # JAX-WS (SOAP) service
├── service-pollution/         # JAX-RS (REST) service
├── service-camera/            # Java RMI service
├── service-bruit/             # TCP Socket service
├── service-feux/              # JAX-RPC (Axis) service
├── central-engine/            # Kafka consumer + analysis + REST API
└── dashboard/                 # Web dashboard (HTML/CSS/JS)
```

## Tools & Technologies

- **Java 17** — Core language
- **JAX-WS** — SOAP web services
- **JAX-RS (Jersey)** — RESTful web services
- **Java RMI** — Remote method invocation
- **TCP Sockets** — Low-level network communication
- **JAX-RPC (Apache Axis)** — Legacy XML-RPC services
- **Apache Kafka** — Message broker
- **MySQL 8.0** — Relational database
- **Maven** — Build tool
- **Docker** — Containerization
