package com.acenexus.tata.nexusbot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 集保分散表實體
 */
@Entity
@Table(name = "shareholding_distribution",
        uniqueConstraints = @UniqueConstraint(name = "uk_distribution_stock_date_level",
                columnNames = {"stock_symbol", "data_date", "holding_shares_level"}),
        indexes = {
                @Index(name = "idx_distribution_data_date", columnList = "data_date"),
                @Index(name = "idx_distribution_stock_symbol", columnList = "stock_symbol")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareholdingDistribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_symbol", nullable = false, length = 20)
    private String stockSymbol;

    @Column(name = "data_date", nullable = false)
    private LocalDate dataDate;

    @Column(name = "holding_shares_level", nullable = false, length = 50)
    private String holdingSharesLevel;

    @Column(name = "people")
    private Long people;

    @Column(name = "percent", precision = 8, scale = 4)
    private BigDecimal percent;

    @Column(name = "unit")
    private Long unit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
