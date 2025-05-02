# Trading P&L Analysis System

## Project Overview
A comprehensive trading profit and loss analysis system for monitoring and analyzing financial product trading performance in real-time.

## Technology Stack
- Frontend: React + TypeScript + Ant Design
- Backend: Spring Boot (Java) + Python
- Database: PostgreSQL + TimescaleDB + Redis
- Message Queue: RabbitMQ
- Monitoring: Prometheus + Grafana

## Project Structure
trading-pnl-system/
├── backend/
│ ├── java-service/ # Spring Boot backend service
│ └── python-service/ # Python calculation engine
├── frontend/ # React frontend application
├── docker/ # Docker configurations
└── docs/ # Project documentation
```

## Setup Instructions
1. Prerequisites
   - JDK 17+
   - Node.js 18+
   - Python 3.9+
   - Docker & Docker Compose
   - PostgreSQL 14+
   - Redis 6+

2. Development Setup
   ```bash
   # Clone the repository
   git clone [repository-url]
   
   # Setup backend
   cd backend/java-service
   ./mvnw spring-boot:run
   
   # Setup frontend
   cd frontend
   npm install
   npm start
   ```

## Development Guidelines
- Follow Git Flow branching model
- Code formatting: Use provided .editorconfig
- API Documentation: OpenAPI/Swagger
- Testing: Unit tests required for all new features

## Phase 1 Roadmap
- [x] Project setup
- [ ] Basic infrastructure
- [ ] FX product support
- [ ] Basic reporting
- [ ] Basic backtesting

## License
Private and Confidential