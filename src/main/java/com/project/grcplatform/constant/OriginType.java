package com.project.grcplatform.constant;

public enum OriginType {
    /** Action triggered by a human via the UI or a direct API call. */
    MANUAL,
    /** Action triggered by an automated scheduled job (cron/batch). */
    BATCH,
    /** Action triggered by an external system through the API. */
    API
}