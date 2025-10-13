package org.acme.orders.domain;

public enum OrderStatus {
    PENDING,      // Commande en attente de confirmation
    CONFIRMED,    // Commande confirmée
    SHIPPED,      // Commande expédiée
    DELIVERED,    // Commande livrée
    CANCELLED     // Commande annulée
}
