package com.gstextract.model;

public record MatchResult(
    InwardInvoice portalInvoice,
    InwardInvoice registerInvoice,
    MatchStatus status,
    String mismatches
) {}
