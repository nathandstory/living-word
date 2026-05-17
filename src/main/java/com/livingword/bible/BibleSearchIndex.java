package com.livingword.bible;

import java.util.List;
import java.util.Objects;

public final class BibleSearchIndex {
    private final BibleDataManager dataManager;

    public BibleSearchIndex(BibleDataManager dataManager) {
        this.dataManager = Objects.requireNonNull(dataManager, "dataManager");
    }

    public List<BibleReference> search(String translationId, String query, int limit) {
        return dataManager.search(translationId, query, limit);
    }
}
