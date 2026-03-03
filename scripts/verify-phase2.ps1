# ═══════════════════════════════════════════════════════════════
# eCommerce Platform - Phase 1 + 2 Verification Script
# Run: Right-click → Run with PowerShell  OR  powershell -File verify.ps1
# ═══════════════════════════════════════════════════════════════

$ErrorActionPreference = "Continue"
$BASE = "http://localhost:8080"
$GREEN  = "Green"
$RED    = "Red"
$YELLOW = "Yellow"
$CYAN   = "Cyan"

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
        Write-Host "  [FAIL] $name → Not reachable ($url)" -ForegroundColor Red
    }
}

function Test-Api($label, $method, $url, $body = $null, $token = $null) {
    Write-Host ""
    Write-Host "  TEST: $label" -ForegroundColor Yellow
    try {
        $headers = @{ "Content-Type" = "application/json" }
        if ($token) { $headers["Authorization"] = "Bearer $token" }

        if ($body) {
            $res = Invoke-RestMethod -Method $method -Uri $url -Body ($body | ConvertTo-Json) -Headers $headers -TimeoutSec 10
        } else {
            $res = Invoke-RestMethod -Method $method -Uri $url -Headers $headers -TimeoutSec 10
        }

        if ($res.success) {
            Write-Host "  [PASS] $label" -ForegroundColor Green
            return $res.data
        } else {
            Write-Host "  [FAIL] $label → $($res.error.message)" -ForegroundColor Red
            return $null
        }
    } catch {
        Write-Host "  [FAIL] $label → $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

# ─────────────────────────────────────────────────────────
Write-Header "STEP 1 — Infrastructure Health"
# ─────────────────────────────────────────────────────────

Check-Health "PostgreSQL" "http://localhost:9000/actuator/health"
Check-Health "Redis"      "http://localhost:9000/actuator/health"

try {
    $es = Invoke-RestMethod -Uri "http://localhost:9200/_cluster/health" -TimeoutSec 5
    Write-Host "  [OK] Elasticsearch → status=$($es.status)" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Elasticsearch → Not reachable" -ForegroundColor Red
}

try {
    $mongo = Invoke-RestMethod -Uri "http://localhost:8083/actuator/health" -TimeoutSec 5
    Write-Host "  [OK] MongoDB (via product-service health)" -ForegroundColor Green
} catch {
    Write-Host "  [WARN] MongoDB check skipped (product-service may not be running yet)" -ForegroundColor Yellow
}

# ─────────────────────────────────────────────────────────
Write-Header "STEP 2 — Service Health Checks"
# ─────────────────────────────────────────────────────────

Check-Health "discovery-server  :8761" "http://localhost:8761/actuator/health"
Check-Health "api-gateway        :8080" "http://localhost:8080/actuator/health"
Check-Health "auth-service       :9000" "http://localhost:9000/actuator/health"
Check-Health "user-service       :8082" "http://localhost:8082/actuator/health"
Check-Health "product-service    :8083" "http://localhost:8083/actuator/health"

# ─────────────────────────────────────────────────────────
Write-Header "STEP 3 — Eureka Registry"
# ─────────────────────────────────────────────────────────

try {
    $eureka = Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps" -Headers @{Accept="application/json"} -TimeoutSec 5
    $apps = $eureka.applications.application
    Write-Host "  Registered services:" -ForegroundColor Cyan
    foreach ($app in $apps) {
        Write-Host "    ✓ $($app.name)" -ForegroundColor Green
    }
} catch {
    Write-Host "  [FAIL] Cannot reach Eureka" -ForegroundColor Red
}

# ─────────────────────────────────────────────────────────
Write-Header "STEP 4 — Auth Service Tests"
# ─────────────────────────────────────────────────────────

# Register
$regBody = @{ email = "testuser@ecom.com"; password = "password123" }
$regResult = Test-Api "Register new user" "POST" "$BASE/api/auth/register" $regBody
Write-Host "  Response: $($regResult | ConvertTo-Json -Compress)" -ForegroundColor Gray

# Login
$loginBody = @{ email = "testuser@ecom.com"; password = "password123" }
$loginResult = Test-Api "Login" "POST" "$BASE/api/auth/login" $loginBody
$TOKEN = $loginResult.accessToken
if ($TOKEN) {
    Write-Host "  Token obtained: $($TOKEN.Substring(0, [Math]::Min(50, $TOKEN.Length)))..." -ForegroundColor Green
} else {
    Write-Host "  [WARN] No token obtained - remaining tests may fail" -ForegroundColor Yellow
}

# Refresh token test
if ($loginResult.refreshToken) {
    $refreshBody = @{ refreshToken = $loginResult.refreshToken }
    $refreshResult = Test-Api "Refresh token" "POST" "$BASE/api/auth/refresh" $refreshBody
    Write-Host "  New token: $($refreshResult.accessToken.Substring(0, [Math]::Min(30, $refreshResult.accessToken.Length)))..." -ForegroundColor Gray
}

# ─────────────────────────────────────────────────────────
Write-Header "STEP 5 — User Service Tests"
# ─────────────────────────────────────────────────────────

# Create profile
$profileBody = @{
    firstName   = "John"
    lastName    = "Doe"
    phone       = "+91-9876543210"
    dateOfBirth = "1990-05-15"
}
$profileResult = Test-Api "Create user profile" "POST" "$BASE/api/users/profile" $profileBody $TOKEN
Write-Host "  Profile ID: $($profileResult.id)" -ForegroundColor Gray

# Get profile
$getProfile = Test-Api "Get my profile" "GET" "$BASE/api/users/profile" $null $TOKEN
Write-Host "  Full name: $($getProfile.fullName)" -ForegroundColor Gray

# Add address
$addressBody = @{
    label        = "HOME"
    addressLine1 = "123 MG Road"
    city         = "Jhansi"
    state        = "Uttar Pradesh"
    postalCode   = "284001"
    countryCode  = "IN"
    isDefault    = $true
}
$addressResult = Test-Api "Add home address" "POST" "$BASE/api/users/addresses" $addressBody $TOKEN
Write-Host "  Address ID: $($addressResult.id)" -ForegroundColor Gray

# Get addresses
$addresses = Test-Api "Get all addresses" "GET" "$BASE/api/users/addresses" $null $TOKEN
Write-Host "  Addresses count: $($addresses.Count)" -ForegroundColor Gray

# Update preferences
$prefBody = @{
    currency           = "INR"
    language           = "en"
    notificationsEmail = $true
    notificationsSms   = $true
}
Test-Api "Update preferences" "PUT" "$BASE/api/users/preferences" $prefBody $TOKEN | Out-Null

# ─────────────────────────────────────────────────────────
Write-Header "STEP 6 — Product Service Tests"
# ─────────────────────────────────────────────────────────

# Create product
$productBody = @{
    sku            = "SHOE-RED-42"
    name           = "Red Running Shoe"
    description    = "Lightweight running shoe for everyday training"
    brand          = "SpeedX"
    categories     = @("footwear", "sports")
    price          = 1999.99
    compareAtPrice = 2499.99
    imageUrls      = @("https://example.com/shoe-red.jpg")
    active         = $true
    attributes     = @{ color = "red"; size = "42"; material = "mesh" }
}
$productResult = Test-Api "Create product" "POST" "$BASE/api/products" $productBody
Write-Host "  Product ID: $($productResult.id)" -ForegroundColor Gray

# Create second product for search testing
$product2Body = @{
    sku        = "TSHIRT-BLUE-L"
    name       = "Blue Cotton T-Shirt"
    description = "Comfortable everyday cotton t-shirt"
    brand      = "ComfortWear"
    categories = @("clothing", "casual")
    price      = 499.99
    active     = $true
    attributes = @{ color = "blue"; size = "L"; material = "cotton" }
}
Test-Api "Create second product" "POST" "$BASE/api/products" $product2Body | Out-Null

# Get featured
$featured = Test-Api "Get featured products" "GET" "$BASE/api/products/featured"
Write-Host "  Featured count: $($featured.Count)" -ForegroundColor Gray

# Get by SKU
$bySku = Test-Api "Get product by SKU" "GET" "$BASE/api/products/sku/SHOE-RED-42"
Write-Host "  Found: $($bySku.name) - INR $($bySku.price)" -ForegroundColor Gray

# Search
$searchResult = Test-Api "Search: running shoe" "GET" "$BASE/api/products/search?q=running+shoe"
Write-Host "  Search results: $($searchResult.Count) products found" -ForegroundColor Gray

# Get by category
$catResult = Test-Api "Get by category: footwear" "GET" "$BASE/api/products/category/footwear"
Write-Host "  Category results: $($catResult.Count) products" -ForegroundColor Gray

# ─────────────────────────────────────────────────────────
Write-Header "SUMMARY"
# ─────────────────────────────────────────────────────────

Write-Host ""
Write-Host "  All tests completed!" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Useful URLs:" -ForegroundColor Yellow
Write-Host "    Eureka Dashboard:  http://localhost:8761"
Write-Host "    Zipkin Tracing:    http://localhost:9411"
Write-Host "    Elasticsearch:     http://localhost:9200"
Write-Host "    Kibana (Phase 7):  http://localhost:5601"
Write-Host ""
Write-Host "  Next: Check above for any [FAIL] entries and fix before Phase 3" -ForegroundColor Cyan
Write-Host ""
Read-Host "Press Enter to exit"






