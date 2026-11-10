package com.mundodev.filafacil.auditoria.model;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int size, long total) {

    public int totalPages() {
        if (size <= 0) {
            return 0;
        }
        return (int) ((total + size - 1) / size);
    }

    public boolean hasNext() {
        return page < totalPages();
    }
}
