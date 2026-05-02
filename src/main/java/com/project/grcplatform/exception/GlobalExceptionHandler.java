package com.project.grcplatform.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        e -> e.getField(),
                        e -> e.getDefaultMessage(),
                        (a, b) -> a
                ));
        return ResponseEntity.badRequest().body(Map.of(
                "code", "VALIDATION_ERROR",
                "errors", fieldErrors
        ));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<?> handleAppException(AppException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case INVALID_EMAIL_OR_PASSWORD, INVALID_OTP, OTP_EXPIRED,
                 SESSION_REVOKED, RESET_TOKEN_EXPIRED, RESET_TOKEN_CANCELLED -> HttpStatus.UNAUTHORIZED;
            case SESSION_NOT_FOUND, RESET_TOKEN_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case USER_NOT_FOUND,
                 GOVERNANCE_POLICY_NOT_FOUND, GOVERNANCE_RESPONSIBILITY_NOT_FOUND,
                 GOVERNANCE_DOCUMENT_NOT_FOUND, COMPLIANCE_FRAMEWORK_NOT_FOUND,
                 COMPLIANCE_REQUIREMENT_NOT_FOUND, COMPLIANCE_EVALUATION_NOT_FOUND,
                 COMPLIANCE_EVALUATION_ITEM_NOT_FOUND, COMPLIANCE_ACTION_PLAN_NOT_FOUND,
                 AUDIT_CAMPAIGN_NOT_FOUND, AUDIT_CHECKLIST_ITEM_NOT_FOUND,
                 AUDIT_FINDING_NOT_FOUND, AUDIT_RECOMMENDATION_NOT_FOUND,
                 ASSET_NOT_FOUND, VULNERABILITY_NOT_FOUND, THREAT_NOT_FOUND,
                 SCENARIO_NOT_FOUND, ASSESSMENT_NOT_FOUND, TREATMENT_PLAN_NOT_FOUND,
                 TREATMENT_ACTION_NOT_FOUND, CONTROL_NOT_FOUND, KRI_NOT_FOUND,
                 REPORT_NOT_FOUND, NOTIFICATION_NOT_FOUND, CVE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case EMAIL_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case ACCOUNT_DISABLED -> HttpStatus.FORBIDDEN;
            case ACCOUNT_LOCKED -> HttpStatus.LOCKED;
            case INVALID_EMAIL_DOMAIN,
                 ROLE_NOT_FOUND, PERMISSION_NOT_FOUND,
                 ORGANISATION_NOT_FOUND, GRADE_NOT_FOUND,
                 INVALID_DATA -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        Map<String, Object> body = new HashMap<>();
        body.put("code", ex.getErrorCode().getInternalCode());
        if (ex.getDetails() != null) {
            body.putAll(ex.getDetails());
        }

        return ResponseEntity.status(status).body(body);
    }
}
