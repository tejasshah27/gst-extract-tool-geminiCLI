package com.gstextract.validator;

import com.gstextract.model.ValidationError;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class GstinValidatorTest {

    @Test
    public void testValidGstin() {
        // A valid GSTIN (with correct checksum)
        String validGstin = "27AAACR1234A1Z3";
        Optional<ValidationError> error = GstinValidator.validate(validGstin, "Sheet1", 1);
        assertFalse(error.isPresent(), "Should be valid: " + validGstin);
    }

    @Test
    public void testInvalidLength() {
        String invalidGstin = "27AAACR1234A1Z";
        Optional<ValidationError> error = GstinValidator.validate(invalidGstin, "Sheet1", 1);
        assertTrue(error.isPresent());
        assertEquals("GSTIN must be 15 characters", error.get().reason());
    }

    @Test
    public void testInvalidFormat() {
        String invalidGstin = "27AAACR1234A1!1";
        Optional<ValidationError> error = GstinValidator.validate(invalidGstin, "Sheet1", 1);
        assertTrue(error.isPresent());
        assertEquals("Invalid GSTIN format", error.get().reason());
    }

    @Test
    public void testInvalidStateCode() {
        String invalidGstin = "00AAACR1234A1Z5"; // State code 00 is invalid
        Optional<ValidationError> error = GstinValidator.validate(invalidGstin, "Sheet1", 1);
        assertTrue(error.isPresent());
        assertTrue(error.get().reason().contains("Invalid state code"));
    }

    @Test
    public void testInvalidChecksum() {
        String invalidGstin = "27AAACR1234A1Z9"; // Changed last char to 9
        Optional<ValidationError> error = GstinValidator.validate(invalidGstin, "Sheet1", 1);
        assertTrue(error.isPresent());
        assertEquals("Invalid GSTIN checksum", error.get().reason());
    }

    @Test
    public void testNullOrEmpty() {
        assertTrue(GstinValidator.validate(null, "Sheet1", 1).isPresent());
        assertTrue(GstinValidator.validate("", "Sheet1", 1).isPresent());
        assertTrue(GstinValidator.validate("   ", "Sheet1", 1).isPresent());
    }
}
