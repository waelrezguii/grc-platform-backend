package com.project.grcplatform.constant;

public enum ReportType {
    ASSESSMENT_SUMMARY,      // Full assessment with all scenarios, scores, treatment plans
    RISK_MATRIX,             // All scenarios plotted by likelihood vs impact
    TREATMENT_STATUS,        // Status of all treatment plans and actions
    CONTROL_EFFECTIVENESS,   // All controls with effectiveness scores
    VULNERABILITY_SUMMARY,   // All vulnerabilities grouped by criticality
    EXECUTIVE_SUMMARY        // High-level KPIs for management
}