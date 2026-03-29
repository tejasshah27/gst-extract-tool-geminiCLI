package com.gstextract.model;

import java.math.BigDecimal;

public record SummaryReturn(
    BigDecimal totalTaxableValue,
    BigDecimal totalIgst,
    BigDecimal totalCgst,
    BigDecimal totalSgst,
    BigDecimal totalUtgst,
    BigDecimal totalCess
) {}
