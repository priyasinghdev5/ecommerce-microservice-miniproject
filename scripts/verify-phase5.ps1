# Phase 5 Verification - notification-service + batch-service
# Run: powershell -ExecutionPolicy Bypass -File verify-phase5.ps1

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
        else { Write-Host "  [!!] $name - $($res.status)" -ForegroundColor Yellow }
    } catch { Write-Host "  [FAIL] $name - not reachable" -ForegroundColor Red }
}

Write-Header "STEP 1 - Service Health"
Check-Health "notification-service :8088" "http://localhost:8088/actuator/health"
Check-Health "batch-service        :8089" "http://localhost:8089/actuator/health"

Write-Header "STEP 2 - Auth Token"
$loginBody = @{ email = "testuser@ecom.com"; password = "password123" }
try {
    $login = Invoke-RestMethod -Method POST -Uri "$BASE/api/auth/login" `
        -Body ($loginBody | ConvertTo-Json) -Headers @{ "Content-Type" = "application/json" } -TimeoutSec 10
    $TOKEN = $login.data.accessToken
    Write-Host "  [OK] Token obtained" -ForegroundColor Green
} catch { Write-Host "  [FAIL] Login failed" -ForegroundColor Red; $TOKEN = "" }

$headers = @{ "Authorization" = "Bearer $TOKEN"; "Content-Type" = "application/json" }

Write-Header "STEP 3 - Trigger Notification via Order"
Write-Host "  Placing order to trigger payment + notification chain..." -ForegroundColor Yellow
$orderBody = @{
    shippingAddress = "456 Lake Road, Jhansi, UP 284001"
    currency = "INR"
    items = @(@{ productId = "550e8400-e29b-41d4-a716-446655440001"; sku = "TEST-PROD-001"; quantity = 1; unitPrice = 500.00 })
}
try {
    $order = Invoke-RestMethod -Method POST -Uri "$BASE/api/orders" `
        -Body ($orderBody | ConvertTo-Json -Depth 5) -Headers $headers -TimeoutSec 15
    Write-Host "  [OK] Order placed: $($order.data.id)" -ForegroundColor Green
    Write-Host "  Waiting 6s for Saga + Notification chain..." -ForegroundColor Yellow
    Start-Sleep -Seconds 6
    Write-Host "  [INFO] Check notification-service terminal for email logs" -ForegroundColor Cyan
} catch { Write-Host "  [WARN] $($_.Exception.Message)" -ForegroundColor Yellow }

Write-Header "STEP 4 - Batch CSV Import"
$csv = "sku,name,description,brand,categories,price,compare_at_price,active`nBATCH-SHOE-001,Batch Test Shoe,Imported via batch,BrandX,footwear,1500.00,2000.00,true`nBATCH-SHIRT-001,Batch Test Shirt,Imported via batch,BrandY,clothing,800.00,1200.00,true`nBAD-ROW,,Missing name skipped,BrandZ,other,-1.00,,true"
$csvPath = "$env:TEMP\test-products.csv"
$csv | Out-File -FilePath $csvPath -Encoding UTF8
Write-Host "  CSV created at: $csvPath" -ForegroundColor Gray

try {
    $form = [System.Net.Http.MultipartFormDataContent]::new()
    $fileBytes = [System.IO.File]::ReadAllBytes($csvPath)
    $fileContent = [System.Net.Http.ByteArrayContent]::new($fileBytes)
    $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("text/csv")
    $form.Add($fileContent, "file", "test-products.csv")

    $client = [System.Net.Http.HttpClient]::new()
    $client.DefaultRequestHeaders.Add("Authorization", "Bearer $TOKEN")
    $resp = $client.PostAsync("http://localhost:8089/api/batch/import", $form).Result
    $body = $resp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    $JOB_ID = $body.data.id
    Write-Host "  [OK] Import job: $JOB_ID" -ForegroundColor Green
    Write-Host "  Waiting 8s for batch processing..." -ForegroundColor Yellow
    Start-Sleep -Seconds 8

    $status = Invoke-RestMethod "http://localhost:8089/api/batch/jobs/$JOB_ID" -TimeoutSec 10
    Write-Host "  Status: $($status.data.status)" -ForegroundColor Green
    Write-Host "  Processed: $($status.data.processedCount) / Total: $($status.data.totalRecords) / Skipped: $($status.data.errorCount)" -ForegroundColor Gray
} catch {
    Write-Host "  [INFO] Manual test: curl -F file=@sample-products.csv http://localhost:8089/api/batch/import" -ForegroundColor Cyan
}

Write-Header "STEP 5 - List All Jobs"
try {
    $jobs = Invoke-RestMethod "http://localhost:8089/api/batch/jobs" -TimeoutSec 10
    Write-Host "  Found $($jobs.data.Count) job(s):" -ForegroundColor Green
    foreach ($j in $jobs.data) {
        Write-Host "    $($j.filename) | $($j.status) | processed=$($j.processedCount)" -ForegroundColor Gray
    }
} catch { Write-Host "  [WARN] Cannot list jobs" -ForegroundColor Yellow }

Write-Header "DONE - Add MailHog for email UI"
Write-Host "  docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog"
Write-Host "  Visit: http://localhost:8025 to see sent emails"
Write-Host ""
Read-Host "Press Enter to exit"
