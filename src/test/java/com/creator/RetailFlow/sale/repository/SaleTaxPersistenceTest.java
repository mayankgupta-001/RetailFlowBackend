package com.creator.RetailFlow.sale.repository;

import com.creator.RetailFlow.product.entity.Product;
import com.creator.RetailFlow.product.repository.ProductRepository;
import com.creator.RetailFlow.sale.entity.PaymentMethod;
import com.creator.RetailFlow.sale.entity.Sale;
import com.creator.RetailFlow.sale.entity.SaleItem;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * Guards the GST snapshot columns on sales and sale_items, and proves
 * that pre-GST rows (null tax columns) still load.
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:sale_tax_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
class SaleTaxPersistenceTest {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsTaxSnapshotOnSaleAndItems() {

        Product product = new Product();
        product.setName("Coffee");
        product.setBarcode("8900001");
        product.setSellingPrice(new BigDecimal("118.00"));
        product.setStock(10);
        product.setMinStock(0);
        product.setTaxRate(new BigDecimal("18"));
        product.setHsnCode("1001");

        productRepository.saveAndFlush(product);

        Sale sale = new Sale();
        sale.setInvoiceNumber("INV-TAX-1");
        sale.setSubtotal(new BigDecimal("118.00"));
        sale.setDiscount(BigDecimal.ZERO);
        sale.setTotal(new BigDecimal("118.00"));
        sale.setTaxableAmount(new BigDecimal("100.00"));
        sale.setTaxAmount(new BigDecimal("18.00"));
        sale.setCgstAmount(new BigDecimal("9.00"));
        sale.setSgstAmount(new BigDecimal("9.00"));
        sale.setPaymentMethod(PaymentMethod.CASH);

        SaleItem item = new SaleItem();
        item.setSale(sale);
        item.setProduct(product);
        item.setProductName("Coffee");
        item.setBarcode("8900001");
        item.setPrice(new BigDecimal("118.00"));
        item.setQuantity(1);
        item.setSubtotal(new BigDecimal("118.00"));
        item.setTaxRate(new BigDecimal("18"));
        item.setHsnCode("1001");
        item.setTaxableAmount(new BigDecimal("100.00"));
        item.setTaxAmount(new BigDecimal("18.00"));
        item.setCgstAmount(new BigDecimal("9.00"));
        item.setSgstAmount(new BigDecimal("9.00"));

        sale.getItems().add(item);

        Sale saved = saleRepository.saveAndFlush(sale);
        entityManager.clear();

        Sale reloaded = saleRepository.findWithItemsById(saved.getId())
                .orElseThrow();

        assertThat(reloaded.getTotal()).isEqualByComparingTo("118.00");
        assertThat(reloaded.getTaxableAmount())
                .isEqualByComparingTo("100.00");
        assertThat(reloaded.getTaxAmount()).isEqualByComparingTo("18.00");
        assertThat(reloaded.getCgstAmount()).isEqualByComparingTo("9.00");
        assertThat(reloaded.getSgstAmount()).isEqualByComparingTo("9.00");

        SaleItem reloadedItem = reloaded.getItems().get(0);

        assertThat(reloadedItem.getTaxRate()).isEqualByComparingTo("18");
        assertThat(reloadedItem.getHsnCode()).isEqualTo("1001");
        assertThat(reloadedItem.getTaxableAmount())
                .isEqualByComparingTo("100.00");
        assertThat(reloadedItem.getTaxAmount())
                .isEqualByComparingTo("18.00");
        assertThat(reloadedItem.getCgstAmount())
                .isEqualByComparingTo("9.00");
        assertThat(reloadedItem.getSgstAmount())
                .isEqualByComparingTo("9.00");
    }

    @Test
    void preGstSaleLoadsWithNullTaxFields() {

        Product product = new Product();
        product.setName("Old Item");
        product.setBarcode("8900002");
        product.setSellingPrice(new BigDecimal("50.00"));
        product.setStock(5);
        product.setMinStock(0);

        productRepository.saveAndFlush(product);

        Sale sale = new Sale();
        sale.setInvoiceNumber("INV-PRE-GST-1");
        sale.setSubtotal(new BigDecimal("50.00"));
        sale.setDiscount(BigDecimal.ZERO);
        sale.setTotal(new BigDecimal("50.00"));
        sale.setPaymentMethod(PaymentMethod.CASH);

        SaleItem item = new SaleItem();
        item.setSale(sale);
        item.setProduct(product);
        item.setProductName("Old Item");
        item.setBarcode("8900002");
        item.setPrice(new BigDecimal("50.00"));
        item.setQuantity(1);
        item.setSubtotal(new BigDecimal("50.00"));

        sale.getItems().add(item);

        Sale saved = saleRepository.saveAndFlush(sale);
        entityManager.clear();

        Sale reloaded = saleRepository.findWithItemsById(saved.getId())
                .orElseThrow();

        assertThat(reloaded.getTaxableAmount()).isNull();
        assertThat(reloaded.getTaxAmount()).isNull();
        assertThat(reloaded.getCgstAmount()).isNull();
        assertThat(reloaded.getSgstAmount()).isNull();

        assertThat(reloaded.getItems().get(0).getTaxRate()).isNull();
        assertThat(reloaded.getItems().get(0).getTaxAmount()).isNull();
    }
}
