package com.livingword.bible;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BibleResourceLoader {
    private final BibleDataManager dataManager;
    private final ClassLoader classLoader;
    private final BibleJsonParser parser;

    public BibleResourceLoader(BibleDataManager dataManager) {
        this(dataManager, Thread.currentThread().getContextClassLoader());
    }

    public BibleResourceLoader(BibleDataManager dataManager, ClassLoader classLoader) {
        this.dataManager = Objects.requireNonNull(dataManager, "dataManager");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
        this.parser = new BibleJsonParser();
    }

    public BibleDataManager dataManager() {
        return dataManager;
    }

    public void reload() {
        reload(List.of("kjv"));
    }

    public void reload(List<String> translationIds) {
        for (String translationId : translationIds) {
            loadTranslation(translationId);
        }
    }

    private void loadTranslation(String translationId) {
        String basePath = "data/livingword/bible/" + translationId + "/";
        Optional<TranslationManifest> manifest = read(basePath + "translation.json", parser::parseManifest);
        if (manifest.isEmpty()) {
            return;
        }
        dataManager.registerTranslation(manifest.get());

        read(basePath + "index.json", parser::parseIndex).ifPresent(index -> {
            for (String bookId : index.bookIds()) {
                for (int chapter : index.chapters(bookId)) {
                    read(basePath + "books/" + bookId + "/" + String.format("%03d", chapter) + ".json", parser::parseChapter)
                        .ifPresent(dataManager::registerChapter);
                }
            }
        });
    }

    private <T> Optional<T> read(String resourcePath, ResourceParser<T> resourceParser) {
        InputStream stream = classLoader.getResourceAsStream(resourcePath);
        if (stream == null) {
            return Optional.empty();
        }
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return Optional.of(resourceParser.parse(reader));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Failed to read Bible resource " + resourcePath, exception);
        }
    }

    @FunctionalInterface
    private interface ResourceParser<T> {
        T parse(Reader reader);
    }
}
