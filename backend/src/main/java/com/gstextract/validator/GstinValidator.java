package com.gstextract.validator;

import com.gstextract.model.Severity;
import com.gstextract.model.ValidationError;
import java.util.Optional;
import java.util.regex.Pattern;

public class GstinValidator {

    private static final Pattern GSTIN_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
    private static final String CHAR_SET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static Optional<ValidationError> validate(String gstin, String sheet, int row) {
        if (gstin == null || gstin.trim().isEmpty()) {
            return Optional.of(new ValidationError(sheet, row, "GSTIN", "GSTIN is missing", Severity.ERROR));
        }

        String normalizedGstin = gstin.trim().toUpperCase();

        if (normalizedGstin.length() != 15) {
            return Optional.of(new ValidationError(sheet, row, "GSTIN", "GSTIN must be 15 characters", Severity.ERROR));
        }

        if (!GSTIN_PATTERN.matcher(normalizedGstin).matches()) {
            return Optional.of(new ValidationError(sheet, row, "GSTIN", "Invalid GSTIN format", Severity.ERROR));
        }

        // State code validation
        int stateCode = Integer.parseInt(normalizedGstin.substring(0, 2));
        if (!isValidStateCode(stateCode)) {
            return Optional.of(new ValidationError(sheet, row, "GSTIN", "Invalid state code: " + stateCode, Severity.ERROR));
        }

        // Checksum validation
        if (!isValidChecksum(normalizedGstin)) {
            return Optional.of(new ValidationError(sheet, row, "GSTIN", "Invalid GSTIN checksum", Severity.ERROR));
        }

        return Optional.empty();
    }

    private static boolean isValidStateCode(int code) {
        return (code >= 1 && code <= 37) || code == 97 || code == 99;
    }

    private static boolean isValidChecksum(String gstin) {
        char checkDigit = gstin.charAt(14);
        char calculatedCheckDigit = calculateCheckDigit(gstin.substring(0, 14));
        return checkDigit == calculatedCheckDigit;
    }

    private static char calculateCheckDigit(String gstin14) {
        int sum = 0;
        for (int i = 0; i < 14; i++) {
            int val = CHAR_SET.indexOf(gstin14.charAt(i));
            int weight = (i % 2 == 0) ? 1 : 2;
            int product = val * weight;
            sum += (product / 36) + (product % 36);
        }
        int remainder = sum % 36;
        int checkCode = (36 - remainder) % 36;
        return CHAR_SET.charAt(checkCode);
    }
}
