package mikhailov.dev.dto;

import java.math.BigDecimal;

public record ClientBalanceSummary(
        String fullName,
        Long accountCount,
        BigDecimal totalBalance,
        BigDecimal averageBalance
) {}
