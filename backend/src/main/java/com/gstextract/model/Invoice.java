package com.gstextract.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Invoice(
    String gstin,
    String invoiceNumber,
    LocalDate invoiceDate,
    BigDecimal taxableValue,
    BigDecimal rate,
    BigDecimal igst,
    BigDecimal cgst,
    BigDecimal sgst,
    BigDecimal utgst,
    BigDecimal cess,
    String pos,
    boolean isAmendment,
    String hsnSac
) {}
