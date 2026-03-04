.PHONY: dev-up dev-down dev-logs dev-restart dev-clean app-run

# Start DB, WAIT for it to be ready, then start App
dev-up:
	docker compose up -d postgres
	@echo "Waiting for Postgres to be healthy..."
	@until [ "$$(docker inspect -f '{{.State.Health.Status}}' corebanking-postgres)" = "healthy" ]; do \
		sleep 1; \
	done
	$(MAKE) app-run

# Just run the Spring Boot app
app-run:
	./mvnw spring-boot:run

# Kill the Java app and stop the DB
dev-down:
	@echo "Stopping Spring Boot application..."
	-pkill -f 'spring-boot:run|core-banking-api' || true
	docker compose down

# Follow DB logs
dev-logs:
	docker compose logs -f postgres

# Quick restart of DB
dev-restart:
	docker compose restart postgres

# Clean everything including volumes
dev-clean:
	-pkill -f 'spring-boot:run|core-banking-api' || true
	docker compose down -v
