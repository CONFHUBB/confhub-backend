package com.capstone.confhub.dto.response;

import com.capstone.confhub.utils.enums.TicketCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketTypeResponse {
    private Integer id;
    private Integer conferenceId;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private LocalDateTime deadline;
    private Integer maxQuantity;
    private Integer quantitySold;
    private Integer availableSlots; // null = unlimited
    private TicketCategory category;
    private Boolean isActive;
    private Boolean isSoldOut;
    private Boolean isDeadlinePassed;
}
