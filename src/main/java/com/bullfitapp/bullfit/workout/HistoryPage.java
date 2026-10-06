package com.bullfitapp.bullfit.workout;

import java.util.List;

public record HistoryPage(List<HistoryItem> items, int page, boolean hasPrevious,
                          boolean hasNext, long hiddenCount) {}
