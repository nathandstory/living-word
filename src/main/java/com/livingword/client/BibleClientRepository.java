package com.livingword.client;

import com.livingword.bible.BibleDataManager;
import com.livingword.bible.BibleResourceLoader;

public final class BibleClientRepository {
    private static BibleDataManager cachedDataManager;

    private BibleClientRepository() {
    }

    public static BibleDataManager dataManager() {
        if (cachedDataManager == null) {
            cachedDataManager = loadBundledData();
        }
        return cachedDataManager;
    }

    private static BibleDataManager loadBundledData() {
        BibleDataManager manager = new BibleDataManager();
        new BibleResourceLoader(manager, BibleClientRepository.class.getClassLoader()).reload();
        return manager;
    }
}
