# ═══════════════════════════════════════════════════════════════
# Phase 6 + FINAL Verification - bff-service + complete platform
# Run: powershell -ExecutionPolicy Bypass -File verify-phase6-final.ps1
# ═══════════════════════════════════════════════════════════════

$ErrorActionPreference = "Continue"
$BASE = "http://localhost:8080"

function Write-Header($text) {
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
    Write-Host "  $text" -ForegroundColor Cyan
    Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan
}

function Check-Health($name, $url) {
    try {
        $res = Invoke-RestMethod -Uri $url -TimeoutSec 5
        if ($res.status -eq "UP") { Write-Host "  [OK] $name" -ForegroundColor Green }
        else { Write-Host "  [!!] $name - $($res.status)" -ForegroundColor Yellow }
    } catch { Write-Host "  [FAIL] $name - not reachable" -ForegroundColor Red }
}

# ── ALL Services Health ────────────────────────────────────────
Write-Header "STEP 1 - All 12 Services Health Check"
Check-Health "discovery-server    :8761" "http://localhost:8761/actuator/health"
Check-Health "api-gateway         :8080" "http://localhost:8080/actuator/health"
Check-Health "auth-service        :9000" "http://localhost:9000/actuator/health"
Check-Health "user-service        :8082" "http://localhost:8082/actuator/health"
Check-Health "product-service     :8083" "http://localhost:8083/actuator/health"
Check-Health "inventory-service   :8084" "http://localhost:8084/actuator/health"
Check-Health "cart-service        :8085" "http://localhost:8085/actuator/health"
Check-Health "order-service       :8086" "http://localhost:8086/actuator/health"
Check-Health "payment-service     :8087" "http://localhost:8087/actuator/health"
Check-Health "notification-service:8088" "http://localhost:8088/actuator/health"
Check-Health "batch-service       :8089" "http://localhost:8089/actuator/health"
Check-Health "bff-service         :8081" "http://localhost:8081/actuator/health"

# ── Eureka Registry ────────────────────────────────────────────
Write-Header "STEP 2 - Eureka Registry"
try {
    $eureka = Invoke-RestMethod "http://localhost:8761/eureka/apps" `
        -Headers @{Accept="application/json"} -TimeoutSec 5
    $apps = $eureka.applications.application
    Write-Host "  Registered: $($apps.Count) services" -ForegroundColor Green
    foreach ($app in $apps) {
        Write-Host "    [+] $($app.name)" -ForegroundColor Gray
    }
} catch { Write-Host "  [FAIL] Cannot reach Eureka" -ForegroundColor Red }

# ── Get Token ──────────────────────────────────────────────────
Write-Header "STEP 3 - Authentication"
$loginBody = @{ email = "testuser@ecom.com"; password = "password123" }
try {
    $login = Invoke-RestMethod -Method POST -Uri "$BASE/api/auth/login" `
        -Body ($loginBody | ConvertTo-Json) `
        -Headers @{ "Content-Type" = "application/json" } -TimeoutSec 10
    $TOKEN = $login.data.accessToken
    Write-Host "  [OK] JWT obtained" -ForegroundColor Green
} catch { Write-Host "  [FAIL] Login failed" -ForegroundColor Red; $TOKEN = "" }

$headers = @{ "Authorization" = "Bearer $TOKEN"; "Content-Type" = "application/json" }

# ── BFF Home (public) ──────────────────────────────────────────
Write-Header "STEP 4 - BFF Home Page (public, no auth)"
try {
    $home = Invoke-RestMethod "http://localhost:8081/api/bff/home" -TimeoutSec 10
    Write-Host "  [OK] Home assembled:" -ForegroundColor Green
    Write-Host "    Featured products: $($home.featuredProducts.Count)" -ForegroundColor Gray
    Write-Host "    Categories: $($home.categories -join ', ')" -ForegroundColor Gray
    Write-Host "    Banners: $($home.banners.Count)" -ForegroundColor Gray
    Write-Host "    From cache: $($home.fromCache)" -ForegroundColor Gray
} catch { Write-Host "  [FAIL] $($_.Exception.Message)" -ForegroundColor Red }

# Second call to test caching
Start-Sleep -Seconds 1
try {
    $home2 = Invoke-RestMethod "http://localhost:8081/api/bff/home" -TimeoutSec 10
    Write-Host "  [OK] Second call (from cache): fromCache=$($home2.fromCache)" -ForegroundColor Green
} catch {}

# ── BFF Dashboard (authenticated) ─────────────────────────────
Write-Header "STEP 5 - BFF Dashboard (authenticated)"
try {
    $dash = Invoke-RestMethod "http://localhost:8081/api/bff/dashboard" `
        -Headers $headers -TimeoutSec 15
    Write-Host "  [OK] Dashboard assembled:" -ForegroundColor Green
    Write-Host "    Profile: $($dash.profile.fullName) (loaded=$($dash.profileLoaded))" -ForegroundColor Gray
    Write-Host "    Orders:  $($dash.recentOrders.Count) (loaded=$($dash.ordersLoaded))" -ForegroundColor Gray
    Write-Host "    Recs:    $($dash.recommendations.Count) (loaded=$($dash.recommendationsLoaded))" -ForegroundColor Gray
} catch { Write-Host "  [FAIL] $($_.Exception.Message)" -ForegroundColor Red }

# ── BFF via API Gateway ────────────────────────────────────────
Write-Header "STEP 6 - BFF via API Gateway"
try {
    $dashViaGateway = Invoke-RestMethod "$BASE/api/bff/dashboard" `
        -Headers $headers -TimeoutSec 15
    Write-Host "  [OK] Dashboard via gateway: profile=$($dashViaGateway.profile.fullName)" -ForegroundColor Green
} catch { Write-Host "  [FAIL] $($_.Exception.Message)" -ForegroundColor Red }

# ── BFF Checkout ───────────────────────────────────────────────
Write-Header "STEP 7 - BFF Checkout"

# Add items to cart first
$cartItem = @{ productId="prod-001"; sku="SHOE-RED-42"; name="Red Shoe"; quantity=2; unitPrice=1999.99 }
try {
    Invoke-RestMethod -Method POST "$BASE/api/cart/items" `
        -Body ($cartItem | ConvertTo-Json) -Headers $headers -TimeoutSec 10 | Out-Null
    Write-Host "  [OK] Cart item added" -ForegroundColor Gray
} catch {}

try {
    $checkout = Invoke-RestMethod "$BASE/api/bff/checkout" -Headers $headers -TimeoutSec 10
    Write-Host "  [OK] Checkout assembled:" -ForegroundColor Green
    Write-Host "    Cart items:  $($checkout.cartItems.Count)" -ForegroundColor Gray
    Write-Host "    Total items: $($checkout.totalItems)" -ForegroundColor Gray
    Write-Host "    Amount:      INR $($checkout.totalAmount)" -ForegroundColor Gray
    Write-Host "    Profile:     $($checkout.userProfile.fullName)" -ForegroundColor Gray
} catch { Write-Host "  [FAIL] $($_.Exception.Message)" -ForegroundColor Red }

# ── Full E2E Flow ──────────────────────────────────────────────
Write-Header "STEP 8 - Complete End-to-End Flow"
Write-Host "  Placing order to test full Saga chain..." -ForegroundColor Yellow
$orderBody = @{
    shippingAddress = "789 Civil Lines, Jhansi, UP 284001"
    currency = "INR"
    items = @(@{ productId = "550e8400-e29b-41d4-a716-446655440001"
                 sku = "TEST-PROD-001"; quantity = 1; unitPrice = 299.99 })
}
try {
    $order = Invoke-RestMethod -Method POST "$BASE/api/orders" `
        -Body ($orderBody | ConvertTo-Json -Depth 5) -Headers $headers -TimeoutSec 15
    $OID = $order.data.id
    Write-Host "  [OK] Order: $OID" -ForegroundColor Green
    Write-Host "  Waiting 6s for Saga (Stock → Payment → Notification)..." -ForegroundColor Yellow
    Start-Sleep -Seconds 6

    $check = Invoke-RestMethod "$BASE/api/orders/$OID" -Headers $headers -TimeoutSec 10
    $status = $check.data.status
    if ($status -eq "CONFIRMED") {
        Write-Host "  [PASS] Order CONFIRMED - full Saga completed!" -ForegroundColor Green
    } elseif ($status -eq "PAYMENT_FAILED") {
        Write-Host "  [PASS] Order PAYMENT_FAILED - compensation triggered (expected ~10%)" -ForegroundColor Yellow
    } else {
        Write-Host "  [INFO] Order status: $status" -ForegroundColor Cyan
    }
} catch { Write-Host "  [FAIL] $($_.Exception.Message)" -ForegroundColor Red }

# ── Final Summary ──────────────────────────────────────────────
Write-Header "PLATFORM COMPLETE - FINAL SUMMARY"
Write-Host ""
Write-Host "  ALL 13 MODULES:" -ForegroundColor Green
Write-Host "    common-lib          shared events, DTOs, Kafka topics"
Write-Host "    discovery-server    Eureka registry              :8761"
Write-Host "    api-gateway         JWT, rate limit, routing     :8080"
Write-Host "    auth-service        OAuth2 + JWT                 :9000"
Write-Host "    user-service        profiles, addresses          :8082"
Write-Host "    product-service     MongoDB + ES + Redis         :8083"
Write-Host "    inventory-service   gRPC + Kafka + PostgreSQL    :8084"
Write-Host "    cart-service        WebFlux + Reactive Redis     :8085"
Write-Host "    order-service       Saga + Outbox + Resilience4j :8086"
Write-Host "    payment-service     Kafka saga participant        :8087"
Write-Host "    notification-service Email + SMS + DLQ           :8088"
Write-Host "    batch-service       Spring Batch + CSV import    :8089"
Write-Host "    bff-service         Mono.zip aggregation         :8081"
Write-Host ""
Write-Host "  USEFUL DASHBOARDS:" -ForegroundColor Cyan
Write-Host "    Eureka:         http://localhost:8761"
Write-Host "    Zipkin:         http://localhost:9411"
Write-Host "    Grafana:        http://localhost:3000 (admin/admin)"
Write-Host "    Kibana:         http://localhost:5601"
Write-Host "    MailHog:        http://localhost:8025"
Write-Host "    Elasticsearch:  http://localhost:9200"
Write-Host ""
Read-Host "Press Enter to exit"
