package com.capstone.confhub.dto.request;

import com.capstone.confhub.utils.enums.TicketCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TicketTypeRequest {

    @NotBlank(message = "Ticket type name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0", message = "Price must be >= 0")
    private BigDecimal price;

    private String currency = "VND";

    private LocalDateTime deadline;

    private Integer maxQuantity;

    @NotNull(message = "Category is required")
    private TicketCategory category;

    private Boolean isActive = true;
}
