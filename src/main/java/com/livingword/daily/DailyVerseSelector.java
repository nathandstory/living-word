package com.livingword.daily;

import com.livingword.bible.BibleDataManager;
import com.livingword.bible.BibleReference;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DailyVerseSelector {
    public static final List<BibleReference> DEFAULT_POOL = List.of(
        new BibleReference("kjv", "psalms", 23, 1),
        new BibleReference("kjv", "psalms", 46, 10),
        new BibleReference("kjv", "john", 3, 16),
        new BibleReference("kjv", "romans", 8, 28),
        new BibleReference("kjv", "philippians", 4, 7),
        new BibleReference("webp", "psalms", 23, 1),
        new BibleReference("webp", "john", 3, 16),
        new BibleReference("webp", "romans", 8, 28)
    );

    private final List<BibleReference> pool;

    public DailyVerseSelector(List<BibleReference> pool) {
        if (pool == null || pool.isEmpty()) {
            throw new IllegalArgumentException("daily verse pool is required");
        }
        this.pool = List.copyOf(pool);
    }

    public Optional<DailyVerse> select(BibleDataManager bible, LocalDate date, long serverSeed) {
        Objects.requireNonNull(bible, "bible");
        Objects.requireNonNull(date, "date");
        int startIndex = indexFor(date, serverSeed, pool.size());
        for (int offset = 0; offset < pool.size(); offset++) {
            BibleReference reference = pool.get((startIndex + offset) % pool.size());
            Optional<String> text = bible.getVerse(reference);
            if (text.isPresent()) {
                return Optional.of(new DailyVerse(reference, text.get()));
            }
        }
        return Optional.empty();
    }

    private static int indexFor(LocalDate date, long serverSeed, int size) {
        long mixed = date.toEpochDay() * 1_103_515_245L + serverSeed;
        return Math.floorMod(Long.hashCode(mixed), size);
    }
}
