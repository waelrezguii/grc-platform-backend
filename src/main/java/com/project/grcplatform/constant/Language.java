package com.project.grcplatform.constant;

import java.util.Locale;

public enum Language {
    EN("en"),
    FR("fr"),
    AR("ar");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    public Locale toLocale() {
        return Locale.forLanguageTag(code);
    }
}
