#!/usr/bin/env bash
BASE_URL="http://localhost:8080"

echo "=== Create source account ==="
SOURCE=$(curl -s -X POST "$BASE_URL/api/v1/accounts" \
  -H "Content-Type: application/json" \
  -d '{"ownerName": "Ada Obi", "currency": "NGN"}')
echo $SOURCE | jq .
SOURCE_NUMBER=$(echo $SOURCE | jq -r '.accountNumber')

echo "=== Create destination account ==="
DEST=$(curl -s -X POST "$BASE_URL/api/v1/accounts" \
  -H "Content-Type: application/json" \
  -d '{"ownerName": "Chidi Okeke", "currency": "NGN"}')
echo $DEST | jq .
DEST_NUMBER=$(echo $DEST | jq -r '.accountNumber')

echo "=== Deposit 10000 into source ==="
curl -s -X POST "$BASE_URL/api/v1/accounts/$SOURCE_NUMBER/deposit" \
  -H "Content-Type: application/json" \
  -d '{"amount": 10000.00, "reference": "INITIAL-FUNDING"}' | jq .

echo "=== Initiate transfer of 3000 ==="
TRANSFER=$(curl -s -X POST "$BASE_URL/api/v1/transfers" \
  -H "Content-Type: application/json" \
  -d "{
    \"sourceAccountNumber\": \"$SOURCE_NUMBER\",
    \"destinationAccountNumber\": \"$DEST_NUMBER\",
    \"amount\": 3000.00
  }")
echo $TRANSFER | jq .
TRANSFER_ID=$(echo $TRANSFER | jq -r '.transferId')

echo "=== Waiting 2s for Kafka to process ==="
sleep 2

echo "=== Get transfer status (expect COMPLETED) ==="
curl -s "$BASE_URL/api/v1/transfers/$TRANSFER_ID" | jq .

echo "=== Source balance (expect 7000) ==="
curl -s "$BASE_URL/api/v1/accounts/$SOURCE_NUMBER" | jq '.balance'

echo "=== Destination balance (expect 3000) ==="
curl -s "$BASE_URL/api/v1/accounts/$DEST_NUMBER" | jq '.balance'