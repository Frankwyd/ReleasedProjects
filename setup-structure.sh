#!/bin/bash

# Backend structure
mkdir -p backend/java-service/src/main/java/com/trading/pnl
mkdir -p backend/java-service/src/main/resources
mkdir -p backend/java-service/src/test/java
mkdir -p backend/java-service/src/test/resources

# Python service structure
mkdir -p backend/python-service/src/{models,services,utils}
mkdir -p backend/python-service/tests
touch backend/python-service/src/__init__.py
touch backend/python-service/src/models/__init__.py
touch backend/python-service/src/services/__init__.py
touch backend/python-service/src/utils/__init__.py
touch backend/python-service/tests/__init__.py

# Frontend structure
mkdir -p frontend/src/{components,pages,services,utils}
mkdir -p frontend/public

# Docker configurations
mkdir -p docker/prometheus
mkdir -p docker/grafana

# Documentation
mkdir -p docs/{api,architecture,deployment}

# Add .gitkeep to empty directories
find . -type d -empty -exec touch {}/.gitkeep \; 