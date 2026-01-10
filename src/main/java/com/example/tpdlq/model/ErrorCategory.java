package com.example.tpdlq.model;

public enum ErrorCategory {
    VALIDATION_ERROR("ValidationError"),
    MALFORMED_ERROR("MalformedError"),
    UNKNOWN_ERROR("UnknownError");

    private final String displayName;

    ErrorCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
