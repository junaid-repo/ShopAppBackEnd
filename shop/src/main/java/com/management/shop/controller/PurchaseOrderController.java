package com.management.shop.controller;

import com.management.shop.dto.*;
import com.management.shop.service.PurchaseOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/shop/purchases")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    private String extractUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping
    public ResponseEntity<Page<PurchaseOrderResponseDTO>> getPurchaseOrders(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        String userId = extractUsername();
        Page<PurchaseOrderResponseDTO> result = purchaseOrderService.getPurchaseOrders(status, fromDate, toDate, search, page, size, userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponseDTO> getPurchaseOrderById(@PathVariable Integer id) {
        String userId = extractUsername();
        PurchaseOrderResponseDTO result = purchaseOrderService.getPurchaseOrderById(id, userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/create")
    public ResponseEntity<PurchaseOrderResponseDTO> createPurchaseOrder(@RequestBody PurchaseOrderRequestDTO dto) {
        String userId = extractUsername();
        PurchaseOrderResponseDTO result = purchaseOrderService.createPurchaseOrder(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/{id}/convert-to-ordered")
    public ResponseEntity<PurchaseOrderResponseDTO> convertDraftToOrdered(@PathVariable Integer id) {
        String userId = extractUsername();
        PurchaseOrderResponseDTO result = purchaseOrderService.convertDraftToOrdered(id, userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<PurchaseOrderResponseDTO> receivePurchaseOrder(
            @PathVariable Integer id,
            @RequestBody(required = false) ReceivePurchaseOrderRequestDTO receiveDto) {

        String userId = extractUsername();
        if (receiveDto == null) {
            receiveDto = new ReceivePurchaseOrderRequestDTO();
        }
        PurchaseOrderResponseDTO result = purchaseOrderService.receivePurchaseOrder(id, receiveDto, userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseOrderResponseDTO> cancelPurchaseOrder(@PathVariable Integer id) {
        String userId = extractUsername();
        PurchaseOrderResponseDTO result = purchaseOrderService.cancelPurchaseOrder(id, userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<PurchaseOrderResponseDTO> duplicatePurchaseOrder(@PathVariable Integer id) {
        String userId = extractUsername();
        PurchaseOrderResponseDTO result = purchaseOrderService.duplicatePurchaseOrder(id, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/summary")
    public ResponseEntity<PurchaseSummaryDTO> getPurchaseSummary() {
        String userId = extractUsername();
        PurchaseSummaryDTO result = purchaseOrderService.getPurchaseSummary(userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/recent-products")
    public ResponseEntity<List<Map<String, Object>>> getRecentProducts(
            @RequestParam(required = false) Integer supplierId,
            @RequestParam(defaultValue = "30") int limit) {
        String userId = extractUsername();
        List<Map<String, Object>> result = purchaseOrderService.getRecentProducts(supplierId, limit, userId);
        return ResponseEntity.ok(result);
    }

    // ──────────────────────────────────────────────
    // SUPPLIER ENDPOINTS
    // ──────────────────────────────────────────────

    @GetMapping("/suppliers")
    public ResponseEntity<List<SupplierDTO>> getSuppliers() {
        String userId = extractUsername();
        List<SupplierDTO> result = purchaseOrderService.getSuppliers(userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/suppliers/save")
    public ResponseEntity<SupplierDTO> saveSupplier(@RequestBody SupplierDTO dto) {
        String userId = extractUsername();
        SupplierDTO result = purchaseOrderService.saveSupplier(dto, userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/suppliers/payment")
    public ResponseEntity<SupplierPaymentDTO> recordSupplierPayment(@RequestBody SupplierPaymentDTO dto) {
        String userId = extractUsername();
        SupplierPaymentDTO result = purchaseOrderService.recordSupplierPayment(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
