package mikhailov.dev.dto;

import java.math.BigDecimal;

public record ClientBalanceSummary(
        String fullName,
        long accountCount,
        BigDecimal totalBalance,
        BigDecimal averageBalance
) {}
