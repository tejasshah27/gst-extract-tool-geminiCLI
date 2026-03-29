package com.gstextract.validator;

import com.gstextract.model.Severity;
import com.gstextract.model.ValidationError;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class TaxAmountValidator {

    private static final Set<BigDecimal> VALID_RATES = new HashSet<>(Arrays.asList(
            new BigDecimal("0.0"),
            new BigDecimal("0.1"),
            new BigDecimal("0.25"),
            new BigDecimal("1.5"),
            new BigDecimal("3.0"),
            new BigDecimal("5.0"),
            new BigDecimal("7.5"),
            new BigDecimal("12.0"),
            new BigDecimal("18.0"),
            new BigDecimal("28.0")
    ));

    public static List<ValidationError> validate(
            BigDecimal taxableValue,
            BigDecimal rate,
            BigDecimal igst,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal utgst,
            BigDecimal tolerance,
            String pos,
            String supplierGstin,
            String sheet,
            int row) {

        List<ValidationError> errors = new ArrayList<>();

        if (taxableValue == null || rate == null) {
            return errors; // Basic validation should be elsewhere
        }

        // Rate validation
        boolean validRate = false;
        for (BigDecimal validRateVal : VALID_RATES) {
            if (validRateVal.compareTo(rate) == 0) {
                validRate = true;
                break;
            }
        }
        if (!validRate) {
            errors.add(new ValidationError(sheet, row, "Rate", "Unexpected GST rate: " + rate, Severity.WARNING));
        }

        // Mutual exclusivity check
        boolean hasIgst = igst != null && igst.compareTo(BigDecimal.ZERO) > 0;
        boolean hasCgst = cgst != null && cgst.compareTo(BigDecimal.ZERO) > 0;
        boolean hasSgst = sgst != null && sgst.compareTo(BigDecimal.ZERO) > 0;
        boolean hasUtgst = utgst != null && utgst.compareTo(BigDecimal.ZERO) > 0;

        if (hasIgst && (hasCgst || hasSgst || hasUtgst)) {
            errors.add(new ValidationError(sheet, row, "Tax", "Invoice has both IGST and (CGST/SGST/UTGST)", Severity.ERROR));
        }
        if (hasSgst && hasUtgst) {
            errors.add(new ValidationError(sheet, row, "Tax", "Invoice has both SGST and UTGST", Severity.ERROR));
        }

        // Intra-state vs Inter-state determination (if POS and supplierGstin are available)
        if (pos != null && supplierGstin != null && supplierGstin.length() >= 2) {
            String supplierState = supplierGstin.substring(0, 2);
            boolean isIntraState = supplierState.equals(pos);

            if (isIntraState && hasIgst) {
                errors.add(new ValidationError(sheet, row, "Tax", "Intra-state supply with IGST", Severity.WARNING));
            }
            if (!isIntraState && (hasCgst || hasSgst || hasUtgst)) {
                errors.add(new ValidationError(sheet, row, "Tax", "Inter-state supply with CGST/SGST/UTGST", Severity.WARNING));
            }
        }

        // Calculations
        BigDecimal hundred = new BigDecimal("100");
        BigDecimal calculatedIgst = taxableValue.multiply(rate).divide(hundred, 2, RoundingMode.HALF_UP);
        BigDecimal calculatedCgst = taxableValue.multiply(rate).divide(new BigDecimal("200"), 2, RoundingMode.HALF_UP);

        if (hasIgst) {
            if (igst.subtract(calculatedIgst).abs().compareTo(tolerance) > 0) {
                errors.add(new ValidationError(sheet, row, "IGST", String.format("Calculated IGST (%s) differs from actual (%s)", calculatedIgst, igst), Severity.WARNING));
            }
        }

        if (hasCgst) {
            if (cgst.subtract(calculatedCgst).abs().compareTo(tolerance) > 0) {
                errors.add(new ValidationError(sheet, row, "CGST", String.format("Calculated CGST (%s) differs from actual (%s)", calculatedCgst, cgst), Severity.WARNING));
            }
        }

        if (hasSgst) {
            if (sgst.subtract(calculatedCgst).abs().compareTo(tolerance) > 0) {
                errors.add(new ValidationError(sheet, row, "SGST", String.format("Calculated SGST (%s) differs from actual (%s)", calculatedCgst, sgst), Severity.WARNING));
            }
            if (hasCgst && cgst.subtract(sgst).abs().compareTo(tolerance) > 0) {
                errors.add(new ValidationError(sheet, row, "Tax", String.format("CGST (%s) and SGST (%s) differ", cgst, sgst), Severity.WARNING));
            }
        }

        if (hasUtgst) {
            if (utgst.subtract(calculatedCgst).abs().compareTo(tolerance) > 0) {
                errors.add(new ValidationError(sheet, row, "UTGST", String.format("Calculated UTGST (%s) differs from actual (%s)", calculatedCgst, utgst), Severity.WARNING));
            }
            if (hasCgst && cgst.subtract(utgst).abs().compareTo(tolerance) > 0) {
                errors.add(new ValidationError(sheet, row, "Tax", String.format("CGST (%s) and UTGST (%s) differ", cgst, utgst), Severity.WARNING));
            }
        }

        return errors;
    }
}
