package com.gstextract.validator;

import com.gstextract.model.Severity;
import com.gstextract.model.ValidationError;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class InvoiceValidator {

    private static final Pattern INVOICE_NUMBER_PATTERN = Pattern.compile("^[a-zA-Z0-9/-]{1,16}$");

    public static List<ValidationError> validate(String invoiceNumber, LocalDate invoiceDate, String sheet, int row) {
        List<ValidationError> errors = new ArrayList<>();

        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            errors.add(new ValidationError(sheet, row, "Invoice Number", "Invoice number is missing", Severity.ERROR));
        } else {
            String trimmedInvoice = invoiceNumber.trim();
            if (trimmedInvoice.contains(" ")) {
                errors.add(new ValidationError(sheet, row, "Invoice Number", "Invoice number contains spaces", Severity.ERROR));
            }
            if (!INVOICE_NUMBER_PATTERN.matcher(trimmedInvoice).matches()) {
                errors.add(new ValidationError(sheet, row, "Invoice Number", "Invalid invoice number format (max 16 chars, alphanumeric, / or -)", Severity.ERROR));
            }
        }

        if (invoiceDate == null) {
            errors.add(new ValidationError(sheet, row, "Invoice Date", "Invoice date is missing", Severity.ERROR));
        }

        return errors;
    }
}
