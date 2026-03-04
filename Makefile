.PHONY: dev-up dev-down dev-logs dev-restart dev-clean

# === Development Commands (Run these instead of manual docker) ===

# Start Postgres (persistent) in background
dev-up:
	docker compose up -d postgres

# Stop everything cleanly
dev-down:
	docker compose down

# Follow DB logs (Ctrl+C to exit)
dev-logs:
	docker compose logs -f postgres

# Quick restart of DB
dev-restart:
	docker compose restart postgres

# ⚠️ Clean everything INCLUDING database data (use only when you want to reset)
dev-clean:
	docker compose down -v