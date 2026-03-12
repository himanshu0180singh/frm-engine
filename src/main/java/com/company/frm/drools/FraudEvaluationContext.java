package com.company.frm.drools;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Drools fact object inserted into the KieSession for every transaction.
 * All data is pre-fetched before session creation so that rules stay
 * stateless and free of Spring bean calls.
 *
 * <p>Computed getter methods (e.g. {@code getAmountAsDouble()},
 * {@code getTransactionHour()}) are used directly in DRL conditions via
 * JavaBeans property conventions, e.g. {@code amountAsDouble > 500000.0}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudEvaluationContext {

    // ── Transaction fields ────────────────────────────────────────────────

    private String transactionId;
    private String customerId;
    private String channel;
    private BigDecimal amount;
    private LocalDateTime transactionTimestamp;
    private Double latitude;
    private Double longitude;
    private String ipAddress;
    private String deviceId;
    private boolean newPayee;
    private Integer authFailureCount;

    // ── Pre-fetched velocity data (Redis) ────────────────────────────────

    /** Number of transactions this customer made in the last 3600 seconds. */
    private long txnCount1h;

    /** Number of transactions this customer made in the last 86400 seconds. */
    private long txnCount24h;

    // ── Pre-fetched customer profile data (PostgreSQL) ───────────────────

    private BigDecimal avgTxnAmount;
    private long totalTxnCount;
    private LocalDateTime lastTxnAt;

    // ── Computed helpers used in DRL conditions ───────────────────────────

    /**
     * Returns the transaction amount as a primitive double for easy comparison
     * in DRL numeric conditions ({@code amountAsDouble > 500000.0}).
     */
    public double getAmountAsDouble() {
        return amount != null ? amount.doubleValue() : 0.0;
    }

    /**
     * Returns the hour (0-23) of the transaction timestamp, used to detect
     * midnight-window transactions ({@code transactionHour < 4}).
     */
    public int getTransactionHour() {
        return (transactionTimestamp != null ? transactionTimestamp : LocalDateTime.now()).getHour();
    }

    /**
     * Returns how many full days have elapsed since the customer's last
     * transaction, or {@code -1} when no prior transaction exists.
     * Used to detect dormant account activity.
     */
    public long getDaysSinceLastTxn() {
        if (lastTxnAt == null) {
            return -1L;
        }
        return ChronoUnit.DAYS.between(lastTxnAt, LocalDateTime.now());
    }

    /**
     * Returns the ratio of the current transaction amount to the customer's
     * average transaction amount. Returns {@code 0.0} when the average is
     * unavailable or zero, which prevents false positives for new customers.
     */
    public double getAvgAmountMultiplier() {
        if (amount == null || avgTxnAmount == null
                || avgTxnAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return amount.doubleValue() / avgTxnAmount.doubleValue();
    }
}
