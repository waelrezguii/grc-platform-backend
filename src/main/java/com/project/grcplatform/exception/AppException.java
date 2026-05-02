package com.project.grcplatform.exception;

import com.project.grcplatform.constant.ErrorCode;

import java.util.Map;
import java.util.Collections;

public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getInternalCode());
        this.errorCode = errorCode;
        this.details = null;
    }

    public AppException(ErrorCode errorCode, Map<String, Object> details) {
        super(errorCode.getInternalCode());
        this.errorCode = errorCode;
        this.details = details != null ? Map.copyOf(details) : null;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details != null ? Collections.unmodifiableMap(details) : null;
    }
}
