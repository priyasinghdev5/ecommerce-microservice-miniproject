# ═══════════════════════════════════════════════════════════════
# Phase 4 Verification - order-service + payment-service
# Tests the complete checkout Saga flow end-to-end
# Run: powershell -ExecutionPolicy Bypass -File verify-phase4.ps1
# ═══════════════════════════════════════════════════════════════

$ErrorActionPreference = "Continue"
$BASE = "http://localhost:8080"

function Write-Header($text) {
    Write-Host ""
    Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  $text" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
}

function Check-Health($name, $url) {
    try {
        $res = Invoke-RestMethod -Uri $url -TimeoutSec 5
        if ($res.status -eq "UP") { Write-Host "  [OK] $name" -ForegroundColor Green }
        else { Write-Host "  [!!] $name → $($res.status)" -ForegroundColor Yellow }
    } catch { Write-Host "  [FAIL] $name → not reachable" -ForegroundColor Red }
}

# ── Health checks ──────────────────────────────────────────
Write-Header "STEP 1 — Service Health"
Check-Health "order-service   :8086" "http://localhost:8086/actuator/health"
Check-Health "payment-service :8087" "http://localhost:8087/actuator/health"

# ── Get token ──────────────────────────────────────────────
Write-Header "STEP 2 — Auth"
$loginBody = @{ email = "testuser@ecom.com"; password = "password123" }
try {
    $login = Invoke-RestMethod -Method POST -Uri "$BASE/api/auth/login" `
        -Body ($loginBody | ConvertTo-Json) `
        -Headers @{ "Content-Type" = "application/json" } -TimeoutSec 10
    $TOKEN = $login.data.accessToken
    Write-Host "  [OK] Token obtained" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Login failed" -ForegroundColor Red; $TOKEN = ""
}

# ── Create inventory first ─────────────────────────────────
Write-Header "STEP 3 — Create Inventory"
$headers = @{ "Content-Type" = "application/json"; "Authorization" = "Bearer $TOKEN" }

$inv = @{ productId = "550e8400-e29b-41d4-a716-446655440001"; sku = "TEST-PROD-001"; initialQuantity = 50; reorderPoint = 5; warehouseLoc = "WH-A" }
try {
    $invRes = Invoke-RestMethod -Method POST -Uri "$BASE/api/inventory" `
        -Body ($inv | ConvertTo-Json) -Headers $headers -TimeoutSec 10
    Write-Host "  [OK] Inventory created: $($invRes.data.sku) qty=$($invRes.data.totalQuantity)" -ForegroundColor Green
} catch { Write-Host "  [WARN] Inventory may already exist: $($_.Exception.Message)" -ForegroundColor Yellow }

# ── Place order ────────────────────────────────────────────
Write-Header "STEP 4 — Place Order (Full Saga Test)"
$orderBody = @{
    shippingAddress = "123 MG Road, Jhansi, UP 284001"
    currency = "INR"
    items = @(
        @{ productId = "550e8400-e29b-41d4-a716-446655440001"; sku = "TEST-PROD-001"; quantity = 2; unitPrice = 999.99 }
    )
}

try {
    $orderRes = Invoke-RestMethod -Method POST -Uri "$BASE/api/orders" `
        -Body ($orderBody | ConvertTo-Json -Depth 5) -Headers $headers -TimeoutSec 15
    $ORDER_ID = $orderRes.data.id
    Write-Host "  [OK] Order created: id=$ORDER_ID status=$($orderRes.data.status)" -ForegroundColor Green
    Write-Host "  Total: INR $($orderRes.data.totalAmount)" -ForegroundColor Gray
} catch {
    Write-Host "  [FAIL] Order creation failed: $($_.Exception.Message)" -ForegroundColor Red
    $ORDER_ID = $null
}

# ── Wait for Saga to complete ──────────────────────────────
if ($ORDER_ID) {
    Write-Host ""
    Write-Host "  Waiting 5s for Saga to complete (Outbox → Kafka → Payment)..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5

    # ── Check order status ─────────────────────────────────
    Write-Header "STEP 5 — Verify Order Status After Saga"
    try {
        $orderCheck = Invoke-RestMethod -Method GET -Uri "$BASE/api/orders/$ORDER_ID" `
            -Headers $headers -TimeoutSec 10
        $finalStatus = $orderCheck.data.status
        if ($finalStatus -eq "CONFIRMED") {
            Write-Host "  [PASS] Order CONFIRMED — payment succeeded!" -ForegroundColor Green
        } elseif ($finalStatus -eq "PAYMENT_FAILED") {
            Write-Host "  [PASS] Order PAYMENT_FAILED — compensation triggered (10% chance)" -ForegroundColor Yellow
        } else {
            Write-Host "  [INFO] Order status: $finalStatus (Saga may still be processing)" -ForegroundColor Cyan
        }
    } catch { Write-Host "  [FAIL] Cannot check order status" -ForegroundColor Red }

    # ── Check payment record ───────────────────────────────
    Write-Header "STEP 6 — Verify Payment Record"
    try {
        $payment = Invoke-RestMethod -Method GET -Uri "$BASE/api/payments/order/$ORDER_ID" `
            -Headers $headers -TimeoutSec 10
        Write-Host "  [PASS] Payment found: status=$($payment.data.status) ref=$($payment.data.gatewayRef)" -ForegroundColor Green
    } catch { Write-Host "  [WARN] Payment not found yet (may still be processing)" -ForegroundColor Yellow }

    # ── List all orders ────────────────────────────────────
    Write-Header "STEP 7 — List My Orders"
    try {
        $orders = Invoke-RestMethod -Method GET -Uri "$BASE/api/orders" -Headers $headers -TimeoutSec 10
        Write-Host "  [PASS] Found $($orders.data.Count) order(s)" -ForegroundColor Green
        foreach ($o in $orders.data) {
            Write-Host "    → id=$($o.id) status=$($o.status) total=$($o.totalAmount)" -ForegroundColor Gray
        }
    } catch { Write-Host "  [FAIL] Cannot list orders" -ForegroundColor Red }
}

# ── Summary ────────────────────────────────────────────────
Write-Header "SAGA FLOW SUMMARY"
Write-Host ""
Write-Host "  Expected flow:" -ForegroundColor Yellow
Write-Host "    POST /api/orders"
Write-Host "      → gRPC ReserveStock (inventory-service:9090)"
Write-Host "      → INSERT order + outbox_event (same DB transaction)"
Write-Host "      → OutboxScheduler publishes to order.created"
Write-Host "      → payment-service consumes → processes payment"
Write-Host "      → publishes payment.success OR payment.failed"
Write-Host "      → order-service updates order status"
Write-Host ""
Write-Host "  Check Zipkin: http://localhost:9411 for full trace" -ForegroundColor Cyan
Write-Host "  Check Kafka:  docker exec ecom-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic order.created --from-beginning" -ForegroundColor Cyan
Write-Host ""
Read-Host "Press Enter to exit"
