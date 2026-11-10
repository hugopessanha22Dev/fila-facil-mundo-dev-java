package com.mundodev.filafacil.inference.tools;

public class ToolError extends RuntimeException {

    private final String code;

    public ToolError(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
