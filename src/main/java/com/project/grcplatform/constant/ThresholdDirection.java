package com.project.grcplatform.constant;

public enum ThresholdDirection {
    /**
     * BREACH when currentValue > breachThreshold
     * WARNING when currentValue > warningThreshold
     * Example: number of open vulnerabilities (more = worse)
     */
    ABOVE,

    /**
     * BREACH when currentValue < breachThreshold
     * WARNING when currentValue < warningThreshold
     * Example: control effectiveness % (less = worse)
     */
    BELOW
}