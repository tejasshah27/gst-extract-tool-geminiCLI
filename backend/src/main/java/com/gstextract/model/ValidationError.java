package com.gstextract.model;

public record ValidationError(
    String sheet,
    int row,
    String field,
    String reason,
    Severity severity
) {}
