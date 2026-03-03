# ═══════════════════════════════════════════════════════════════
# Phase 3 Verification Script — inventory-service + cart-service
# Run: powershell -ExecutionPolicy Bypass -File verify-phase3.ps1
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
        if ($res.status -eq "UP") {
            Write-Host "  [OK] $name → UP" -ForegroundColor Green
        } else {
            Write-Host "  [!!] $name → $($res.status)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  [FAIL] $name → Not reachable" -ForegroundColor Red
    }
}

function Test-Api($label, $method, $url, $body = $null, $token = $null) {
    Write-Host "  TEST: $label" -ForegroundColor Yellow
    try {
        $headers = @{ "Content-Type" = "application/json" }
        if ($token) { $headers["Authorization"] = "Bearer $token" }
        if ($body) {
            $res = Invoke-RestMethod -Method $method -Uri $url -Body ($body | ConvertTo-Json) -Headers $headers -TimeoutSec 10
        } else {
            $res = Invoke-RestMethod -Method $method -Uri $url -Headers $headers -TimeoutSec 10
        }
        if ($res.success -ne $false) {
            Write-Host "  [PASS] $label" -ForegroundColor Green
            return $res.data
        } else {
            Write-Host "  [FAIL] $label → $($res | ConvertTo-Json -Compress)" -ForegroundColor Red
            return $null
        }
    } catch {
        Write-Host "  [FAIL] $label → $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

# ─────────────────────────────────────────────────────────
Write-Header "STEP 1 — Service Health Checks"
# ─────────────────────────────────────────────────────────

Check-Health "inventory-service :8084" "http://localhost:8084/actuator/health"
Check-Health "cart-service       :8085" "http://localhost:8085/actuator/health"

# ─────────────────────────────────────────────────────────
Write-Header "STEP 2 — Get Auth Token"
# ─────────────────────────────────────────────────────────

$loginBody = @{ email = "testuser@ecom.com"; password = "password123" }
try {
    $loginRes = Invoke-RestMethod -Method POST -Uri "$BASE/api/auth/login" `
        -Body ($loginBody | ConvertTo-Json) `
        -Headers @{ "Content-Type" = "application/json" } -TimeoutSec 10
    $TOKEN = $loginRes.data.accessToken
    Write-Host "  [OK] Token obtained" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Login failed - run verify-phase2.ps1 first to register" -ForegroundColor Red
    $TOKEN = ""
}

# ─────────────────────────────────────────────────────────
Write-Header "STEP 3 — Inventory Service Tests"
# ─────────────────────────────────────────────────────────

# Create inventory for existing product
$inv1 = @{
    productId       = "00000000-0000-0000-0000-000000000001"
    sku             = "SHOE-RED-42"
    initialQuantity = 100
    reorderPoint    = 10
    warehouseLoc    = "WAREHOUSE-A"
}
$invResult = Test-Api "Create inventory (SHOE-RED-42)" "POST" "$BASE/api/inventory" $inv1
Write-Host "  Inventory ID: $($invResult.id)" -ForegroundColor Gray

$inv2 = @{
    productId       = "00000000-0000-0000-0000-000000000002"
    sku             = "TSHIRT-BLUE-L"
    initialQuantity = 200
    reorderPoint    = 20
    warehouseLoc    = "WAREHOUSE-B"
}
Test-Api "Create inventory (TSHIRT-BLUE-L)" "POST" "$BASE/api/inventory" $inv2 | Out-Null

# Check stock
$stock = Test-Api "Check stock: SHOE-RED-42" "GET" "$BASE/api/inventory/SHOE-RED-42"
Write-Host "  Available: $($stock.availableQty) / Total: $($stock.totalQuantity)" -ForegroundColor Gray

# Add more stock
$addStock = Test-Api "Add 50 more units" "PATCH" "$BASE/api/inventory/SHOE-RED-42/add?quantity=50"
Write-Host "  New total: $($addStock.totalQuantity)" -ForegroundColor Gray

# ─────────────────────────────────────────────────────────
Write-Header "STEP 4 — Cart Service Tests"
# ─────────────────────────────────────────────────────────

# Add item to cart
$item1 = @{
    productId  = "shoe-prod-001"
    sku        = "SHOE-RED-42"
    name       = "Red Running Shoe"
    quantity   = 2
    unitPrice  = 1999.99
    imageUrl   = "https://example.com/shoe.jpg"
}
try {
    $headers = @{ "Content-Type" = "application/json"; "Authorization" = "Bearer $TOKEN" }
    Invoke-RestMethod -Method POST -Uri "$BASE/api/cart/items" `
        -Body ($item1 | ConvertTo-Json) -Headers $headers -TimeoutSec 10 | Out-Null
    Write-Host "  [PASS] Add item to cart" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Add item → $($_.Exception.Message)" -ForegroundColor Red
}

# Add second item
$item2 = @{
    productId = "tshirt-prod-002"
    sku       = "TSHIRT-BLUE-L"
    name      = "Blue Cotton T-Shirt"
    quantity  = 3
    unitPrice = 499.99
    imageUrl  = "https://example.com/tshirt.jpg"
}
try {
    Invoke-RestMethod -Method POST -Uri "$BASE/api/cart/items" `
        -Body ($item2 | ConvertTo-Json) -Headers $headers -TimeoutSec 10 | Out-Null
    Write-Host "  [PASS] Add second item to cart" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Add second item → $($_.Exception.Message)" -ForegroundColor Red
}

# Get cart
try {
    $cart = Invoke-RestMethod -Method GET -Uri "$BASE/api/cart" -Headers $headers -TimeoutSec 10
    Write-Host "  [PASS] Get cart → $($cart.Count) items" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Get cart → $($_.Exception.Message)" -ForegroundColor Red
}

# Get cart summary
try {
    $summary = Invoke-RestMethod -Method GET -Uri "$BASE/api/cart/summary" -Headers $headers -TimeoutSec 10
    Write-Host "  [PASS] Cart summary → $($summary.totalItems) items, total = INR $($summary.totalAmount)" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Cart summary → $($_.Exception.Message)" -ForegroundColor Red
}

# Remove one item
try {
    Invoke-RestMethod -Method DELETE -Uri "$BASE/api/cart/items/shoe-prod-001" -Headers $headers -TimeoutSec 10 | Out-Null
    Write-Host "  [PASS] Remove item from cart" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Remove item → $($_.Exception.Message)" -ForegroundColor Red
}

# Verify item removed
try {
    $cartAfter = Invoke-RestMethod -Method GET -Uri "$BASE/api/cart" -Headers $headers -TimeoutSec 10
    Write-Host "  [PASS] Cart after remove → $($cartAfter.Count) items remaining" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Get cart after remove failed" -ForegroundColor Red
}

# Clear cart
try {
    Invoke-RestMethod -Method DELETE -Uri "$BASE/api/cart" -Headers $headers -TimeoutSec 10 | Out-Null
    Write-Host "  [PASS] Clear cart" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Clear cart → $($_.Exception.Message)" -ForegroundColor Red
}

# ─────────────────────────────────────────────────────────
Write-Header "SUMMARY"
# ─────────────────────────────────────────────────────────

Write-Host ""
Write-Host "  Phase 3 tests complete!" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Services running:" -ForegroundColor Yellow
Write-Host "    inventory-service → http://localhost:8084 (REST)"
Write-Host "    inventory-service → localhost:9090         (gRPC)"
Write-Host "    cart-service      → http://localhost:8085  (WebFlux)"
Write-Host ""
Write-Host "  Note: gRPC port 9090 is tested by order-service in Phase 4" -ForegroundColor Gray
Write-Host ""
Read-Host "Press Enter to exit"
