package com.livingword.bible;

import java.util.Objects;

public final class BibleResourceLoader {
    private final BibleDataManager dataManager;

    public BibleResourceLoader(BibleDataManager dataManager) {
        this.dataManager = Objects.requireNonNull(dataManager, "dataManager");
    }

    public BibleDataManager dataManager() {
        return dataManager;
    }

    public void reload() {
        // Future implementation:
        // scan data/*/bible/<translation>/translation.json manifests and
        // data/*/bible/<translation>/books/<book>/<chapter>.json chapter files.
    }
}
