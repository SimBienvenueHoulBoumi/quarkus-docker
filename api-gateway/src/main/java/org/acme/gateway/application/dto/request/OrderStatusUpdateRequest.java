package org.acme.gateway.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OrderStatusUpdateRequest(
        @NotBlank(message = "status is required")
        String status
) {
}
