package com.ecom.inventory.controller;

import com.ecom.common.dto.ApiResponse;
import com.ecom.inventory.dto.*;
import com.ecom.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> create(
            @Valid @RequestBody CreateInventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Inventory created", inventoryService.createInventory(request)));
    }

    @GetMapping("/{sku}")
    public ResponseEntity<ApiResponse<StockResponse>> checkStock(@PathVariable String sku) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.checkStock(sku)));
    }

    @PatchMapping("/{sku}/add")
    public ResponseEntity<ApiResponse<InventoryResponse>> addStock(
            @PathVariable String sku,
            @RequestParam int quantity) {
        return ResponseEntity.ok(ApiResponse.ok("Stock added", inventoryService.addStock(sku, quantity)));
    }
}
