.PHONY: dev-up dev-down dev-logs dev-restart dev-clean app-run build test

# Load .env file and export all variables so Spring Boot inherits them
ifneq (,$(wildcard .env))
  include .env
  export
endif

# Start DB, wait for it to be ready, then start app
dev-up:
	docker compose up -d postgres
	@echo "Waiting for Postgres to be healthy..."
	@until [ "$$(docker inspect -f '{{.State.Health.Status}}' $${CONTAINER_NAME})" = "healthy" ]; do \
		sleep 1; \
	done
	$(MAKE) app-run

# Run the Spring Boot app from the app module (not root — root has no main class)
app-run:
	./mvnw spring-boot:run -pl app -am

# Build all modules from root
build:
	./mvnw clean install -DskipTests

# Run all tests
test:
	./mvnw test

# Kill the Java app and stop the DB
dev-down:
	@echo "Stopping Spring Boot application..."
	-pkill -f 'spring-boot:run|coreledger' || true
	docker compose down

# Follow DB logs
dev-logs:
	docker compose logs -f postgres

# Quick restart of DB
dev-restart:
	docker compose restart postgres

# Clean everything including volumes
dev-clean:
	-pkill -f 'spring-boot:run|coreledger' || true
	docker compose down -v
	./mvnw clean