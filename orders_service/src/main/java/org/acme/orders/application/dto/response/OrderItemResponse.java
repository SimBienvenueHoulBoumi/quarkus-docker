package org.acme.orders.application.dto.response;

import java.math.BigDecimal;

public class OrderItemResponse {

    private Long id;
    private Long articleId;
    private String articleName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    // Constructors
    public OrderItemResponse() {
    }

    public OrderItemResponse(Long id, Long articleId, String articleName, 
                            Integer quantity, BigDecimal unitPrice, BigDecimal subtotal) {
        this.id = id;
        this.articleId = articleId;
        this.articleName = articleName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public String getArticleName() {
        return articleName;
    }

    public void setArticleName(String articleName) {
        this.articleName = articleName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
