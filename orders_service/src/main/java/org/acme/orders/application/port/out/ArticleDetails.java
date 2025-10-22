package org.acme.orders.application.port.out;

import java.math.BigDecimal;

public record ArticleDetails(Long id, String name, BigDecimal price, int stock) {
}
