package com.management.shop.service;

import com.management.shop.dto.*;
import com.management.shop.entity.*;
import com.management.shop.repository.*;
import com.management.shop.util.PDFGSTInvoiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepo;

    @Autowired
    private PurchaseOrderItemRepository purchaseOrderItemRepo;

    @Autowired
    private SupplierRepository supplierRepo;

    @Autowired
    private SupplierPaymentRepository supplierPaymentRepo;

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private SalesCacheService salesCacheService;

    @Autowired
    private ShopBasicRepository shopBasicRepo;

    @Autowired
    private ShopFinanceRepository shopFinanceRepo;

    @Autowired
    private PDFGSTInvoiceUtil pdfgstutil;

    // ──────────────────────────────────────────────
    // SUPPLIER OPERATIONS
    // ──────────────────────────────────────────────

    public List<SupplierDTO> getSuppliers(String userId) {
        return supplierRepo.findByUserIdAndIsActiveTrueOrderByNameAsc(userId)
                .stream()
                .map(this::mapSupplierToDTO)
                .collect(Collectors.toList());
    }

    public Page<SupplierDTO> searchSuppliers(String search, int page, int size, String userId) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size);
        return supplierRepo.searchSuppliers(userId, search == null ? "" : search.trim(), pageable)
                .map(this::mapSupplierToDTO);
    }

    @Transactional
    public SupplierDTO saveSupplier(SupplierDTO dto, String userId) {
        SupplierEntity entity;
        if (dto.getId() != null) {
            entity = supplierRepo.findByIdAndUserId(dto.getId(), userId)
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found with id: " + dto.getId()));
        } else {
            entity = new SupplierEntity();
            entity.setUserId(userId);
            entity.setBalanceDue(BigDecimal.ZERO);
            entity.setIsActive(true);
        }

        entity.setName(dto.getName());
        entity.setCompanyName(dto.getCompanyName());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setGstin(dto.getGstin());
        entity.setAddress(dto.getAddress());
        entity.setUpiId(dto.getUpiId());

        SupplierEntity saved = supplierRepo.save(entity);
        return mapSupplierToDTO(saved);
    }

    // ──────────────────────────────────────────────
    // PURCHASE ORDER OPERATIONS
    // ──────────────────────────────────────────────

    public Page<PurchaseOrderResponseDTO> getPurchaseOrders(String status, LocalDate fromDate, LocalDate toDate,
                                                            String search, int page, int size, String userId) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size);
        LocalDate from = fromDate != null ? fromDate : LocalDate.now().minusMonths(4);
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        String normalizedStatus = (status == null || status.equalsIgnoreCase("ALL")) ? null : status.toUpperCase();
        String normalizedSearch = search == null ? "" : search.trim();

        return purchaseOrderRepo.findPurchaseOrders(userId, normalizedStatus, from, to, normalizedSearch, pageable)
                .map(this::mapPOToResponseDTO);
    }

    public PurchaseOrderResponseDTO getPurchaseOrderById(Integer id, String userId) {
        PurchaseOrderEntity entity = purchaseOrderRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found with id: " + id));
        return mapPOToResponseDTO(entity);
    }

    @Transactional
    public PurchaseOrderResponseDTO createPurchaseOrder(PurchaseOrderRequestDTO dto, String userId) {
        SupplierEntity supplier = supplierRepo.findByIdAndUserId(dto.getSupplierId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found with id: " + dto.getSupplierId()));

        Integer maxId = purchaseOrderRepo.getMaxIdForUser(userId);
        String generatedPoNumber = String.format("PO-%04d", (maxId != null ? maxId : 0) + 1);

        PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;
        if ("ORDERED".equalsIgnoreCase(dto.getOrderStatus())) {
            status = PurchaseOrderStatus.ORDERED;
        }

        BigDecimal subTotal = dto.getSubTotal() != null ? dto.getSubTotal() : BigDecimal.ZERO;
        BigDecimal taxAmount = dto.getTaxAmount() != null ? dto.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = dto.getTotalAmount() != null ? dto.getTotalAmount() : subTotal.add(taxAmount);
        BigDecimal paidAmount = dto.getPaidAmount() != null ? dto.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal dueAmount = totalAmount.subtract(paidAmount).max(BigDecimal.ZERO);

        PurchasePaymentStatus payStatus = PurchasePaymentStatus.UNPAID;
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            payStatus = paidAmount.compareTo(totalAmount) >= 0 ? PurchasePaymentStatus.PAID : PurchasePaymentStatus.PARTIALLY_PAID;
        }

        PurchaseOrderEntity po = PurchaseOrderEntity.builder()
                .userId(userId)
                .poNumber(generatedPoNumber)
                .supplier(supplier)
                .orderStatus(status)
                .paymentStatus(payStatus)
                .orderDate(dto.getOrderDate() != null ? dto.getOrderDate() : LocalDate.now())
                .expectedDeliveryDate(dto.getExpectedDeliveryDate())
                .subTotal(subTotal)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .dueAmount(dueAmount)
                .supplierInvoiceNumber(dto.getSupplierInvoiceNumber())
                .notes(dto.getNotes())
                .billingAddress(dto.getBillingAddress())
                .bankDetails(dto.getBankDetails())
                .paymentMode(dto.getPaymentMode())
                .signatureUrl(dto.getSignatureUrl())
                .items(new ArrayList<>())
                .build();

        if (dto.getItems() != null) {
            for (PurchaseOrderItemDTO itemDto : dto.getItems()) {
                ProductEntity product = null;
                if (itemDto.getProductId() != null) {
                    product = productRepo.findByIdAndUserId(itemDto.getProductId(), userId);
                }
                if (product == null && itemDto.getProductName() != null) {
                    try {
                        product = productRepo.findByNameAndUserId(itemDto.getProductName(), userId);
                    } catch (Exception ignored) {}
                }

                PurchaseOrderItemEntity item = PurchaseOrderItemEntity.builder()
                        .purchaseOrder(po)
                        .product(product)
                        .productName(itemDto.getProductName())
                        .productBarcode(itemDto.getProductBarcode())
                        .orderedQuantity(itemDto.getOrderedQuantity() != null ? itemDto.getOrderedQuantity() : 1)
                        .receivedQuantity(0)
                        .unitPurchasePrice(itemDto.getUnitPurchasePrice() != null ? itemDto.getUnitPurchasePrice() : BigDecimal.ZERO)
                        .taxRate(itemDto.getTaxRate() != null ? itemDto.getTaxRate() : BigDecimal.ZERO)
                        .lineTotal(itemDto.getLineTotal() != null ? itemDto.getLineTotal() : BigDecimal.ZERO)
                        .build();

                po.getItems().add(item);
            }
        }

        if (status == PurchaseOrderStatus.ORDERED && po.getItems() != null) {
            for (PurchaseOrderItemEntity item : po.getItems()) {
                if (item.getProduct() != null) {
                    ProductEntity product = item.getProduct();
                    int currentStock = product.getStock() != null ? product.getStock() : 0;
                    int addQty = item.getOrderedQuantity() != null ? item.getOrderedQuantity() : 0;
                    product.setStock(currentStock + addQty);
                    product.setStatus("In Stock");
                    product.setActive(true);
                    if (item.getUnitPurchasePrice() != null && item.getUnitPurchasePrice().compareTo(BigDecimal.ZERO) > 0) {
                        product.setCostPrice(item.getUnitPurchasePrice().intValue());
                    }
                    product.setUpdatedDate(LocalDateTime.now());
                    product.setUpdatedBy(userId);
                    productRepo.save(product);
                }
            }
            try {
                salesCacheService.evictUserProducts(userId);
            } catch (Exception ignored) {}
        }

        if (status == PurchaseOrderStatus.ORDERED && dueAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentDue = supplier.getBalanceDue() != null ? supplier.getBalanceDue() : BigDecimal.ZERO;
            supplier.setBalanceDue(currentDue.add(dueAmount));
            supplierRepo.save(supplier);
        }

        PurchaseOrderEntity saved = purchaseOrderRepo.save(po);
        return mapPOToResponseDTO(saved);
    }

    @Transactional
    public PurchaseOrderResponseDTO convertDraftToOrdered(Integer id, String userId) {
        PurchaseOrderEntity po = purchaseOrderRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found with id: " + id));

        if (po.getOrderStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT purchase orders can be converted to ORDERED.");
        }

        po.setOrderStatus(PurchaseOrderStatus.ORDERED);

        if (po.getItems() != null) {
            for (PurchaseOrderItemEntity item : po.getItems()) {
                if (item.getProduct() != null) {
                    ProductEntity product = item.getProduct();
                    int currentStock = product.getStock() != null ? product.getStock() : 0;
                    int addQty = item.getOrderedQuantity() != null ? item.getOrderedQuantity() : 0;
                    product.setStock(currentStock + addQty);
                    product.setStatus("In Stock");
                    product.setActive(true);
                    if (item.getUnitPurchasePrice() != null && item.getUnitPurchasePrice().compareTo(BigDecimal.ZERO) > 0) {
                        product.setCostPrice(item.getUnitPurchasePrice().intValue());
                    }
                    product.setUpdatedDate(LocalDateTime.now());
                    product.setUpdatedBy(userId);
                    productRepo.save(product);
                }
            }
            try {
                salesCacheService.evictUserProducts(userId);
            } catch (Exception ignored) {}
        }

        if (po.getDueAmount() != null && po.getDueAmount().compareTo(BigDecimal.ZERO) > 0) {
            SupplierEntity supplier = po.getSupplier();
            if (supplier != null) {
                BigDecimal currentDue = supplier.getBalanceDue() != null ? supplier.getBalanceDue() : BigDecimal.ZERO;
                supplier.setBalanceDue(currentDue.add(po.getDueAmount()));
                supplierRepo.save(supplier);
            }
        }

        PurchaseOrderEntity saved = purchaseOrderRepo.save(po);
        return mapPOToResponseDTO(saved);
    }

    @Transactional
    public PurchaseOrderResponseDTO receivePurchaseOrder(Integer id, ReceivePurchaseOrderRequestDTO receiveDto, String userId) {
        PurchaseOrderEntity po = purchaseOrderRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found with id: " + id));

        if (po.getOrderStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new IllegalStateException("Purchase order is already marked as RECEIVED.");
        }
        if (po.getOrderStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot receive a cancelled purchase order.");
        }

        boolean updateCostPrice = Boolean.TRUE.equals(receiveDto.getUpdateProductCostPrice());
        if (receiveDto.getSupplierInvoiceNumber() != null && !receiveDto.getSupplierInvoiceNumber().trim().isEmpty()) {
            po.setSupplierInvoiceNumber(receiveDto.getSupplierInvoiceNumber().trim());
        }

        Map<Integer, ReceivePurchaseOrderRequestDTO.ItemReceiveDetail> receiveMap = null;
        if (receiveDto.getReceivedItems() != null) {
            receiveMap = receiveDto.getReceivedItems().stream()
                    .filter(d -> d.getItemId() != null)
                    .collect(Collectors.toMap(ReceivePurchaseOrderRequestDTO.ItemReceiveDetail::getItemId, Function.identity(), (a, b) -> b));
        }

        boolean allFullyReceived = true;
        boolean anyReceived = false;

        for (PurchaseOrderItemEntity item : po.getItems()) {
            int receivedNow = 0;
            if (receiveMap != null && receiveMap.containsKey(item.getId())) {
                ReceivePurchaseOrderRequestDTO.ItemReceiveDetail detail = receiveMap.get(item.getId());
                receivedNow = detail.getQuantityReceived() != null ? detail.getQuantityReceived() : item.getOrderedQuantity();
                if (detail.getUnitPurchasePrice() != null) {
                    item.setUnitPurchasePrice(detail.getUnitPurchasePrice());
                }
            } else {
                // Default: Full receipt of ordered quantity if no specific breakdown is provided
                receivedNow = item.getOrderedQuantity() - item.getReceivedQuantity();
            }

            if (receivedNow > 0) {
                anyReceived = true;
                item.setReceivedQuantity(item.getReceivedQuantity() + receivedNow);

                // Auto-increment inventory stock in ProductEntity
                if (item.getProduct() != null) {
                    ProductEntity product = item.getProduct();
                    int currentStock = product.getStock() != null ? product.getStock() : 0;
                    product.setStock(currentStock + receivedNow);

                    if (product.getStock() > 0) {
                        product.setStatus("In Stock");
                    }
                    if (updateCostPrice && item.getUnitPurchasePrice() != null) {
                        product.setCostPrice(item.getUnitPurchasePrice().intValue());
                    }
                    product.setUpdatedDate(LocalDateTime.now());
                    product.setUpdatedBy(userId);
                    productRepo.save(product);
                }
            }

            if (item.getReceivedQuantity() < item.getOrderedQuantity()) {
                allFullyReceived = false;
            }
        }

        if (allFullyReceived) {
            po.setOrderStatus(PurchaseOrderStatus.RECEIVED);
            po.setReceivedDate(LocalDateTime.now());
        } else if (anyReceived) {
            po.setOrderStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        // Update supplier balance due if this PO has unpaid dues
        if (po.getDueAmount() != null && po.getDueAmount().compareTo(BigDecimal.ZERO) > 0) {
            SupplierEntity supplier = po.getSupplier();
            BigDecimal currentDue = supplier.getBalanceDue() != null ? supplier.getBalanceDue() : BigDecimal.ZERO;
            supplier.setBalanceDue(currentDue.add(po.getDueAmount()));
            supplierRepo.save(supplier);
        }

        PurchaseOrderEntity saved = purchaseOrderRepo.save(po);

        // Evict cache so Products & Dashboard pages reflect updated stock counts immediately
        try {
            salesCacheService.evictUserProducts(userId);
            salesCacheService.evictUserDasbhoard(userId);
        } catch (Exception e) {
            log.warn("Cache eviction failed after receiving PO: {}", e.getMessage());
        }

        return mapPOToResponseDTO(saved);
    }

    @Transactional
    public PurchaseOrderResponseDTO cancelPurchaseOrder(Integer id, String userId) {
        PurchaseOrderEntity po = purchaseOrderRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found with id: " + id));

        if (po.getOrderStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new IllegalStateException("Cannot cancel a completed/received purchase order.");
        }

        po.setOrderStatus(PurchaseOrderStatus.CANCELLED);
        PurchaseOrderEntity saved = purchaseOrderRepo.save(po);
        return mapPOToResponseDTO(saved);
    }

    @Transactional
    public PurchaseOrderResponseDTO duplicatePurchaseOrder(Integer id, String userId) {
        PurchaseOrderEntity original = purchaseOrderRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found with id: " + id));

        PurchaseOrderRequestDTO req = PurchaseOrderRequestDTO.builder()
                .supplierId(original.getSupplier().getId())
                .orderStatus("DRAFT")
                .orderDate(LocalDate.now())
                .expectedDeliveryDate(LocalDate.now().plusDays(3))
                .subTotal(original.getSubTotal())
                .taxAmount(original.getTaxAmount())
                .totalAmount(original.getTotalAmount())
                .notes("Duplicated from " + original.getPoNumber())
                .items(original.getItems().stream().map(it -> PurchaseOrderItemDTO.builder()
                        .productId(it.getProduct() != null ? it.getProduct().getId() : null)
                        .productName(it.getProductName())
                        .productBarcode(it.getProductBarcode())
                        .orderedQuantity(it.getOrderedQuantity())
                        .unitPurchasePrice(it.getUnitPurchasePrice())
                        .taxRate(it.getTaxRate())
                        .lineTotal(it.getLineTotal())
                        .build()).collect(Collectors.toList()))
                .build();

        return createPurchaseOrder(req, userId);
    }

    @Transactional
    public PurchaseOrderResponseDTO updatePurchaseOrder(Integer id, PurchaseOrderRequestDTO dto, String userId) {
        PurchaseOrderEntity po = purchaseOrderRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found with id: " + id));

        if (po.getOrderStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalStateException("Only draft purchase orders can be edited. Current status: " + po.getOrderStatus());
        }

        if (po.getPaidAmount() != null && po.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Cannot edit purchase order with recorded payments. Recorded payment: ₹" + po.getPaidAmount());
        }

        SupplierEntity supplier = supplierRepo.findByIdAndUserId(dto.getSupplierId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found with id: " + dto.getSupplierId()));

        PurchaseOrderStatus newStatus = PurchaseOrderStatus.DRAFT;
        if ("ORDERED".equalsIgnoreCase(dto.getOrderStatus())) {
            newStatus = PurchaseOrderStatus.ORDERED;
        }

        BigDecimal subTotal = dto.getSubTotal() != null ? dto.getSubTotal() : BigDecimal.ZERO;
        BigDecimal taxAmount = dto.getTaxAmount() != null ? dto.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = dto.getTotalAmount() != null ? dto.getTotalAmount() : subTotal.add(taxAmount);
        BigDecimal paidAmount = dto.getPaidAmount() != null ? dto.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal dueAmount = totalAmount.subtract(paidAmount).max(BigDecimal.ZERO);

        PurchasePaymentStatus payStatus = PurchasePaymentStatus.UNPAID;
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            payStatus = paidAmount.compareTo(totalAmount) >= 0 ? PurchasePaymentStatus.PAID : PurchasePaymentStatus.PARTIALLY_PAID;
        }

        po.setSupplier(supplier);
        po.setOrderStatus(newStatus);
        po.setPaymentStatus(payStatus);
        po.setOrderDate(dto.getOrderDate() != null ? dto.getOrderDate() : po.getOrderDate());
        po.setExpectedDeliveryDate(dto.getExpectedDeliveryDate());
        po.setSubTotal(subTotal);
        po.setTaxAmount(taxAmount);
        po.setTotalAmount(totalAmount);
        po.setPaidAmount(paidAmount);
        po.setDueAmount(dueAmount);
        po.setSupplierInvoiceNumber(dto.getSupplierInvoiceNumber());
        po.setNotes(dto.getNotes());
        po.setBillingAddress(dto.getBillingAddress());
        po.setBankDetails(dto.getBankDetails());
        po.setPaymentMode(dto.getPaymentMode());
        po.setSignatureUrl(dto.getSignatureUrl());

        // Replace items
        po.getItems().clear();
        if (dto.getItems() != null) {
            for (PurchaseOrderItemDTO itemDto : dto.getItems()) {
                ProductEntity product = null;
                if (itemDto.getProductId() != null) {
                    product = productRepo.findByIdAndUserId(itemDto.getProductId(), userId);
                }
                if (product == null && itemDto.getProductName() != null) {
                    try {
                        product = productRepo.findByNameAndUserId(itemDto.getProductName(), userId);
                    } catch (Exception ignored) {}
                }

                PurchaseOrderItemEntity item = PurchaseOrderItemEntity.builder()
                        .purchaseOrder(po)
                        .product(product)
                        .productName(itemDto.getProductName())
                        .productBarcode(itemDto.getProductBarcode())
                        .orderedQuantity(itemDto.getOrderedQuantity() != null ? itemDto.getOrderedQuantity() : 1)
                        .receivedQuantity(0)
                        .unitPurchasePrice(itemDto.getUnitPurchasePrice() != null ? itemDto.getUnitPurchasePrice() : BigDecimal.ZERO)
                        .taxRate(itemDto.getTaxRate() != null ? itemDto.getTaxRate() : BigDecimal.ZERO)
                        .lineTotal(itemDto.getLineTotal() != null ? itemDto.getLineTotal() : BigDecimal.ZERO)
                        .build();

                po.getItems().add(item);
            }
        }

        // If newly transitioned to ORDERED, increment stock & update supplier balance due
        if (newStatus == PurchaseOrderStatus.ORDERED) {
            for (PurchaseOrderItemEntity item : po.getItems()) {
                if (item.getProduct() != null) {
                    ProductEntity product = item.getProduct();
                    int currentStock = product.getStock() != null ? product.getStock() : 0;
                    int addQty = item.getOrderedQuantity() != null ? item.getOrderedQuantity() : 0;
                    product.setStock(currentStock + addQty);
                    product.setStatus("In Stock");
                    product.setActive(true);
                    if (item.getUnitPurchasePrice() != null && item.getUnitPurchasePrice().compareTo(BigDecimal.ZERO) > 0) {
                        product.setCostPrice(item.getUnitPurchasePrice().intValue());
                    }
                    product.setUpdatedDate(LocalDateTime.now());
                    product.setUpdatedBy(userId);
                    productRepo.save(product);
                }
            }
            try {
                salesCacheService.evictUserProducts(userId);
            } catch (Exception ignored) {}

            if (dueAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentDue = supplier.getBalanceDue() != null ? supplier.getBalanceDue() : BigDecimal.ZERO;
                supplier.setBalanceDue(currentDue.add(dueAmount));
                supplierRepo.save(supplier);
            }
        }

        PurchaseOrderEntity saved = purchaseOrderRepo.save(po);
        return mapPOToResponseDTO(saved);
    }

    public byte[] generatePurchaseOrderPdf(Integer id, String userId) {
        PurchaseOrderEntity po = purchaseOrderRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found with id: " + id));

        ShopBasicEntity shopBasic = shopBasicRepo.findByUserId(userId);
        ShopFinanceEntity shopFinance = shopFinanceRepo.findByUserId(userId);

        String shopName = shopBasic != null && shopBasic.getShopName() != null && !shopBasic.getShopName().isEmpty()
                ? shopBasic.getShopName() : "My Shop";
        String shopAddress = shopBasic != null ? shopBasic.getAddress() : "";
        String shopPhone = shopBasic != null ? shopBasic.getShopPhone() : "";
        String shopEmail = shopBasic != null ? shopBasic.getShopEmail() : "";
        String shopSlogan = shopBasic != null ? shopBasic.getShopSlogan() : "";
        String shopGstin = shopFinance != null ? shopFinance.getGstin() : "";
        String shopPan = shopFinance != null ? shopFinance.getPanNumber() : "";

        Map<String, Object> vars = new HashMap<>();
        vars.put("po", po);
        vars.put("supplier", po.getSupplier());
        vars.put("items", po.getItems() != null ? po.getItems() : Collections.emptyList());
        vars.put("shopName", shopName);
        vars.put("shopAddress", shopAddress);
        vars.put("shopPhone", shopPhone);
        vars.put("shopEmail", shopEmail);
        vars.put("shopSlogan", shopSlogan);
        vars.put("shopGstin", shopGstin);
        vars.put("shopPan", shopPan);

        BigDecimal grandTotal = po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO;
        vars.put("amountInWords", pdfgstutil.amountInWords(grandTotal, false));

        return pdfgstutil.generatePurchaseOrderPdf(vars);
    }

    @Transactional
    public SupplierPaymentDTO recordSupplierPayment(SupplierPaymentDTO dto, String userId) {
        SupplierEntity supplier = supplierRepo.findByIdAndUserId(dto.getSupplierId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found with id: " + dto.getSupplierId()));

        PurchaseOrderEntity po = null;
        if (dto.getPurchaseOrderId() != null) {
            po = purchaseOrderRepo.findByIdAndUserId(dto.getPurchaseOrderId(), userId).orElse(null);
        }

        BigDecimal paymentAmount = dto.getAmount() != null ? dto.getAmount() : BigDecimal.ZERO;

        SupplierPaymentEntity payment = SupplierPaymentEntity.builder()
                .userId(userId)
                .supplier(supplier)
                .purchaseOrder(po)
                .amount(paymentAmount)
                .paymentDate(dto.getPaymentDate() != null ? dto.getPaymentDate() : LocalDate.now())
                .paymentMode(dto.getPaymentMode() != null ? dto.getPaymentMode() : "CASH")
                .transactionReference(dto.getTransactionReference())
                .remarks(dto.getRemarks())
                .build();

        SupplierPaymentEntity saved = supplierPaymentRepo.save(payment);

        // Update supplier balance due
        BigDecimal currentDue = supplier.getBalanceDue() != null ? supplier.getBalanceDue() : BigDecimal.ZERO;
        supplier.setBalanceDue(currentDue.subtract(paymentAmount).max(BigDecimal.ZERO));
        supplierRepo.save(supplier);

        // Update PO paid and due amount if linked
        if (po != null) {
            BigDecimal paid = po.getPaidAmount() != null ? po.getPaidAmount() : BigDecimal.ZERO;
            po.setPaidAmount(paid.add(paymentAmount));
            BigDecimal remainingDue = po.getTotalAmount().subtract(po.getPaidAmount()).max(BigDecimal.ZERO);
            po.setDueAmount(remainingDue);

            if (remainingDue.compareTo(BigDecimal.ZERO) == 0) {
                po.setPaymentStatus(PurchasePaymentStatus.PAID);
            } else {
                po.setPaymentStatus(PurchasePaymentStatus.PARTIALLY_PAID);
            }
            purchaseOrderRepo.save(po);
        }

        return SupplierPaymentDTO.builder()
                .id(saved.getId())
                .supplierId(supplier.getId())
                .supplierName(supplier.getName())
                .purchaseOrderId(po != null ? po.getId() : null)
                .poNumber(po != null ? po.getPoNumber() : null)
                .amount(saved.getAmount())
                .paymentDate(saved.getPaymentDate())
                .paymentMode(saved.getPaymentMode())
                .transactionReference(saved.getTransactionReference())
                .remarks(saved.getRemarks())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public PurchaseSummaryDTO getPurchaseSummary(String userId) {
        Double totalPurchases = purchaseOrderRepo.getTotalPurchasesSum(userId);
        Integer totalOrders = purchaseOrderRepo.getTotalOrdersCount(userId);
        Integer pendingOrders = purchaseOrderRepo.getPendingOrdersCount(userId);
        Double orderDues = purchaseOrderRepo.getTotalDueAmountSum(userId);
        Double supplierDues = supplierRepo.getTotalSupplierDues(userId);
        double effectiveDues = Math.max(orderDues != null ? orderDues : 0.0, supplierDues != null ? supplierDues : 0.0);

        return PurchaseSummaryDTO.builder()
                .totalPurchases(BigDecimal.valueOf(totalPurchases != null ? totalPurchases : 0.0))
                .totalOrdersCount(totalOrders != null ? totalOrders : 0)
                .pendingOrdersCount(pendingOrders != null ? pendingOrders : 0)
                .totalDueToSuppliers(BigDecimal.valueOf(effectiveDues))
                .build();
    }

    // ──────────────────────────────────────────────
    // MAPPING HELPERS
    // ──────────────────────────────────────────────

    private SupplierDTO mapSupplierToDTO(SupplierEntity s) {
        return SupplierDTO.builder()
                .id(s.getId())
                .name(s.getName())
                .companyName(s.getCompanyName())
                .phone(s.getPhone())
                .email(s.getEmail())
                .gstin(s.getGstin())
                .address(s.getAddress())
                .upiId(s.getUpiId())
                .balanceDue(s.getBalanceDue())
                .isActive(s.getIsActive())
                .createdDate(s.getCreatedDate())
                .build();
    }

    private PurchaseOrderResponseDTO mapPOToResponseDTO(PurchaseOrderEntity po) {
        List<PurchaseOrderItemResponseDTO> items = new ArrayList<>();
        if (po.getItems() != null) {
            for (PurchaseOrderItemEntity it : po.getItems()) {
                items.add(PurchaseOrderItemResponseDTO.builder()
                        .id(it.getId())
                        .productId(it.getProduct() != null ? it.getProduct().getId() : null)
                        .productName(it.getProductName())
                        .productBarcode(it.getProductBarcode())
                        .orderedQuantity(it.getOrderedQuantity())
                        .receivedQuantity(it.getReceivedQuantity())
                        .unitPurchasePrice(it.getUnitPurchasePrice())
                        .taxRate(it.getTaxRate())
                        .lineTotal(it.getLineTotal())
                        .build());
            }
        }

        BigDecimal effectiveDue = po.getDueAmount();
        if (effectiveDue == null) {
            BigDecimal total = po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal paid = po.getPaidAmount() != null ? po.getPaidAmount() : BigDecimal.ZERO;
            effectiveDue = total.subtract(paid).max(BigDecimal.ZERO);
        }

        return PurchaseOrderResponseDTO.builder()
                .id(po.getId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplier() != null ? po.getSupplier().getId() : null)
                .supplierName(po.getSupplier() != null ? po.getSupplier().getName() : "-")
                .supplierPhone(po.getSupplier() != null ? po.getSupplier().getPhone() : "")
                .supplierGstin(po.getSupplier() != null ? po.getSupplier().getGstin() : "")
                .orderStatus(po.getOrderStatus() != null ? po.getOrderStatus().name() : "DRAFT")
                .paymentStatus(po.getPaymentStatus() != null ? po.getPaymentStatus().name() : "UNPAID")
                .orderDate(po.getOrderDate())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .receivedDate(po.getReceivedDate())
                .subTotal(po.getSubTotal())
                .taxAmount(po.getTaxAmount())
                .totalAmount(po.getTotalAmount())
                .paidAmount(po.getPaidAmount())
                .dueAmount(effectiveDue)
                .supplierInvoiceNumber(po.getSupplierInvoiceNumber())
                .notes(po.getNotes())
                .billingAddress(po.getBillingAddress())
                .bankDetails(po.getBankDetails())
                .paymentMode(po.getPaymentMode())
                .signatureUrl(po.getSignatureUrl())
                .items(items)
                .itemCount(items.size())
                .createdDate(po.getCreatedDate())
                .build();
    }

    public List<Map<String, Object>> getRecentProducts(Integer supplierId, int limit, String userId) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (supplierId != null) {
            List<PurchaseOrderItemEntity> items = purchaseOrderItemRepo.findRecentItemsBySupplier(userId, supplierId, limit);
            Set<Integer> seenProductIds = new HashSet<>();
            for (PurchaseOrderItemEntity it : items) {
                if (it.getProduct() != null && seenProductIds.add(it.getProduct().getId())) {
                    ProductEntity p = it.getProduct();
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.getId());
                    map.put("name", p.getName());
                    map.put("sku", p.getSku());
                    map.put("price", p.getPrice());
                    map.put("costPrice", it.getUnitPurchasePrice() != null ? it.getUnitPurchasePrice() : p.getCostPrice());
                    map.put("tax", it.getTaxRate() != null ? it.getTaxRate() : (p.getTaxPercent() != null ? BigDecimal.valueOf(p.getTaxPercent()) : BigDecimal.ZERO));
                    map.put("unit", "PCS");
                    map.put("hsn", p.getHsn());
                    map.put("source", "SUPPLIER_RECENT");
                    results.add(map);
                } else if (it.getProduct() == null && it.getProductName() != null && seenProductIds.add(it.getProductName().hashCode())) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", it.getProductName());
                    map.put("sku", it.getProductBarcode());
                    map.put("costPrice", it.getUnitPurchasePrice());
                    map.put("tax", it.getTaxRate());
                    map.put("source", "SUPPLIER_RECENT");
                    results.add(map);
                }
            }
        }

        // If no supplier items found or supplierId was not provided, fallback to active products
        if (results.isEmpty()) {
            List<ProductEntity> activeProducts = productRepo.findAllActiveProducts(true, userId);
            int count = 0;
            for (ProductEntity p : activeProducts) {
                if (count++ >= limit) break;
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("name", p.getName());
                map.put("sku", p.getSku());
                map.put("price", p.getPrice());
                map.put("costPrice", p.getCostPrice());
                map.put("tax", p.getTaxPercent() != null ? BigDecimal.valueOf(p.getTaxPercent()) : BigDecimal.ZERO);
                map.put("unit", "PCS");
                map.put("hsn", p.getHsn());
                map.put("source", "CATALOG_POPULAR");
                results.add(map);
            }
        }
        return results;
    }
}
