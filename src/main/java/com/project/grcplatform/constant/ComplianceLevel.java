package com.project.grcplatform.constant;

public enum ComplianceLevel {
    COMPLIANT(100.0),
    PARTIALLY_COMPLIANT(50.0),
    NON_COMPLIANT(0.0),
    NOT_APPLICABLE(null);  // excluded from overall score calculation

    public final Double score;

    ComplianceLevel(Double score) {
        this.score = score;
    }
}