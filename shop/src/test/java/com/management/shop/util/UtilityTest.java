package com.management.shop.util;

import com.management.shop.dto.InvoiceData;
import com.management.shop.dto.InvoiceDetails;
import com.management.shop.dto.OrderItem;
import com.management.shop.dto.UpdateUserDTO;
import com.management.shop.entity.BillingGstEntity;
import com.management.shop.entity.CustomerEntity;
import com.management.shop.entity.UserSettingsEntity;
import com.management.shop.repository.BillingGstRepository;
import com.management.shop.repository.ShopRepository;
import com.management.shop.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilityTest {

    private TestUtility utility;
    private List<BillingGstEntity> gstEntries;
    private UserSettingsEntity settings;
    private CustomerEntity customer;

    @BeforeEach
    void setUp() {
        utility = new TestUtility();
        BillingGstRepository billingGstRepository = repositoryProxy(
                BillingGstRepository.class,
                (methodName, arguments) -> "findByUserIdAndOrderId".equals(methodName) ? gstEntries : null);
        UserSettingsRepository userSettingsRepository = repositoryProxy(
                UserSettingsRepository.class,
                (methodName, arguments) -> "findByUsername".equals(methodName) ? settings : null);
        ShopRepository customerRepository = repositoryProxy(
                ShopRepository.class,
                (methodName, arguments) -> "findByIdAndUserId".equals(methodName) ? customer : null);

        ReflectionTestUtils.setField(utility, "billGstRepo", billingGstRepository);
        ReflectionTestUtils.setField(utility, "userSettingsRepo", userSettingsRepository);
        ReflectionTestUtils.setField(utility, "custRepo", customerRepository);
    }

    @Test
    void buildsInvoiceUsingDefaultsWhenDatabaseValuesAreNull() throws Exception {
        UpdateUserDTO profile = UpdateUserDTO.builder().build();
        OrderItem item = OrderItem.builder()
                .productName(null)
                .details(null)
                .cgstPercentage(null)
                .sgstPercentage(null)
                .igstPercentage(null)
                .discount(null)
                .quantity(1)
                .unitPrice(100d)
                .build();
        InvoiceDetails order = InvoiceDetails.builder()
                .invoiceId("INV-1")
                .customerId(42)
                .items(Arrays.asList(null, item))
                .build();
        BillingGstEntity gstEntry = BillingGstEntity.builder()
                .gstType(null)
                .gstPercentage(null)
                .gstAmount(null)
                .build();

        utility.profile = profile;
        utility.order = order;
        gstEntries = Arrays.asList(null, gstEntry);
        settings = UserSettingsEntity.builder().build();
        customer = null;

        InvoiceData invoice = assertDoesNotThrow(
                () -> utility.getFullInvoiceDetails("user-1", "INV-1"));

        assertEquals("", invoice.getShopName());
        assertEquals("", invoice.getCustomerBillingAddress());
        assertEquals("", invoice.getProducts().get(0).getDescription());
        assertEquals(0d, invoice.getProducts().get(0).getDiscountPercentage());
        assertEquals(0d, invoice.getGstSummary().get(0).get("percentage"));
        assertTrue(invoice.getPrintDueAmount());
        assertTrue(invoice.getShowHsnColumn());
        assertTrue(invoice.getEnableDecimalPlace());
        assertFalse(invoice.getShowQrcode());
    }

    @Test
    void buildsEmptyInvoiceWhenIdentifiersAreNull() {
        InvoiceData invoice = assertDoesNotThrow(
                () -> utility.getFullInvoiceDetails(null, null));

        assertEquals("", invoice.getInvoiceId());
        assertEquals(List.of(), invoice.getProducts());
        assertEquals(List.of(), invoice.getGstSummary());
    }

    @SuppressWarnings("unchecked")
    private <T> T repositoryProxy(Class<T> repositoryType, RepositoryCall repositoryCall) {
        return (T) Proxy.newProxyInstance(
                repositoryType.getClassLoader(),
                new Class<?>[]{repositoryType},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> repositoryType.getSimpleName() + "TestProxy";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    return repositoryCall.invoke(method.getName(), args);
                });
    }

    @FunctionalInterface
    private interface RepositoryCall {
        Object invoke(String methodName, Object[] arguments);
    }

    private static final class TestUtility extends Utility {
        private UpdateUserDTO profile;
        private InvoiceDetails order;

        @Override
        public UpdateUserDTO getUserProfile(String username) {
            return profile;
        }

        @Override
        public InvoiceDetails getOrderDetails(String orderReferenceNumber) {
            return order;
        }

        @Override
        public byte[] getShopLogoOracle(String username) throws IOException {
            return null;
        }

        @Override
        public byte[] getShopSignature(String username) throws IOException {
            return null;
        }
    }
}
