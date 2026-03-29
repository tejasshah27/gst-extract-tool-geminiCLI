package com.gstextract.validator;

import com.gstextract.model.Severity;
import com.gstextract.model.ValidationError;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class TaxAmountValidatorTest {

    private final BigDecimal tolerance = new BigDecimal("0.5");

    @Test
    public void testValidIgst() {
        List<ValidationError> errors = TaxAmountValidator.validate(
                new BigDecimal("1000.00"),
                new BigDecimal("18.0"),
                new BigDecimal("180.00"),
                null, null, null,
                tolerance, "27", "28AAAAA0000A1Z5", // Inter-state (28 vs 27)
                "Sheet1", 1);
        assertTrue(errors.isEmpty(), "Should be no errors for correct IGST");
    }

    @Test
    public void testValidCgstSgst() {
        List<ValidationError> errors = TaxAmountValidator.validate(
                new BigDecimal("1000.00"),
                new BigDecimal("18.0"),
                null,
                new BigDecimal("90.00"),
                new BigDecimal("90.00"),
                null,
                tolerance, "27", "27AAAAA0000A1Z5", // Intra-state (27 vs 27)
                "Sheet1", 1);
        assertTrue(errors.isEmpty(), "Should be no errors for correct CGST/SGST");
    }

    @Test
    public void testUnexpectedRate() {
        List<ValidationError> errors = TaxAmountValidator.validate(
                new BigDecimal("1000.00"),
                new BigDecimal("15.0"), // Invalid rate
                new BigDecimal("150.00"),
                null, null, null,
                tolerance, "27", "28AAAAA0000A1Z5",
                "Sheet1", 1);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.reason().contains("Unexpected GST rate")));
    }

    @Test
    public void testMutuallyExclusiveTaxes() {
        List<ValidationError> errors = TaxAmountValidator.validate(
                new BigDecimal("1000.00"),
                new BigDecimal("18.0"),
                new BigDecimal("180.00"),
                new BigDecimal("90.00"), // Has IGST and CGST
                null, null,
                tolerance, "27", "28AAAAA0000A1Z5",
                "Sheet1", 1);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.reason().contains("both IGST and (CGST/SGST/UTGST)")));
    }

    @Test
    public void testWrongTaxTypeForLocation() {
        // Intra-state (27 vs 27) but has IGST
        List<ValidationError> errors = TaxAmountValidator.validate(
                new BigDecimal("1000.00"),
                new BigDecimal("18.0"),
                new BigDecimal("180.00"),
                null, null, null,
                tolerance, "27", "27AAAAA0000A1Z5",
                "Sheet1", 1);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.reason().contains("Intra-state supply with IGST")));
    }

    @Test
    public void testIncorrectCalculation() {
        List<ValidationError> errors = TaxAmountValidator.validate(
                new BigDecimal("1000.00"),
                new BigDecimal("18.0"),
                new BigDecimal("185.00"), // Incorrect: should be 180.00
                null, null, null,
                tolerance, "27", "28AAAAA0000A1Z5",
                "Sheet1", 1);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.reason().contains("Calculated IGST")));
    }
}
