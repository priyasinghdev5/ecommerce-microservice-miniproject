package com.ecom.inventory.grpc;

import com.ecom.inventory.service.InventoryService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import com.ecom.inventory.grpc.InventoryServiceGrpc;
import com.ecom.inventory.grpc.CheckStockRequest;
import com.ecom.inventory.grpc.CheckStockResponse;

import java.util.UUID;

/**
 * gRPC server implementation.
 * Called by order-service during checkout for synchronous stock operations.
 * Port: 9090 (configured in application.yml)
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class InventoryGrpcService extends InventoryServiceGrpc.InventoryServiceImplBase {

    private final InventoryService inventoryService;

    @Override
    public void checkStock(CheckStockRequest request,
                           StreamObserver<CheckStockResponse> responseObserver) {
        log.info("gRPC checkStock: sku={} qty={}", request.getSku(), request.getQuantity());
        try {
            var stockResponse = inventoryService.checkStock(request.getSku());
            boolean available = stockResponse.availableQty() >= request.getQuantity();

            CheckStockResponse response = CheckStockResponse.newBuilder()
                    .setAvailable(available)
                    .setCurrentStock(stockResponse.availableQty())
                    .setProductId(request.getProductId())
                    .setMessage(available ? "Stock available" : "Insufficient stock")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("checkStock failed: {}", e.getMessage());
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void reserveStock(ReserveStockRequest request,
                             StreamObserver<ReserveStockResponse> responseObserver) {
        log.info("gRPC reserveStock: orderId={} sku={} qty={}",
                request.getOrderId(), request.getSku(), request.getQuantity());
        try {
            var result = inventoryService.reserveStock(
                    UUID.fromString(request.getOrderId()),
                    UUID.fromString(request.getProductId()),
                    request.getSku(),
                    request.getQuantity()
            );

            ReserveStockResponse response = ReserveStockResponse.newBuilder()
                    .setSuccess(result.success())
                    .setReservationId(result.reservationId() != null ? result.reservationId() : "")
                    .setMessage(result.message())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("reserveStock failed: {}", e.getMessage());
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void releaseStock(ReleaseStockRequest request,
                             StreamObserver<ReleaseStockResponse> responseObserver) {
        log.info("gRPC releaseStock: orderId={} reservationId={}",
                request.getOrderId(), request.getReservationId());
        try {
            inventoryService.releaseStock(
                    UUID.fromString(request.getOrderId()),
                    UUID.fromString(request.getReservationId())
            );

            ReleaseStockResponse response = ReleaseStockResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Stock released successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("releaseStock failed: {}", e.getMessage());
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
