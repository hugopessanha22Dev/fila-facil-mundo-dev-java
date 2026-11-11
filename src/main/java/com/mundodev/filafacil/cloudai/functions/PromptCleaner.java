package com.mundodev.filafacil.cloudai.functions;

import com.mundodev.filafacil.cloudai.Redactor;

public final class PromptCleaner {

    private PromptCleaner() {
    }

    public static String clean(String prompt) {
        return Redactor.mask(prompt);
    }
}
