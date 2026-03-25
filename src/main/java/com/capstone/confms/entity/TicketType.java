package com.capstone.confms.entity;

import com.capstone.confms.utils.enums.TicketCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ticket_types")
public class TicketType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conference_id", nullable = false)
    private Conference conference;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal price;

    @Column(nullable = false, length = 3, columnDefinition = "VARCHAR(3) DEFAULT 'VND'")
    private String currency = "VND";

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "max_quantity")
    private Integer maxQuantity;

    @Column(name = "quantity_sold", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer quantitySold = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketCategory category;

    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isActive = true;
}
