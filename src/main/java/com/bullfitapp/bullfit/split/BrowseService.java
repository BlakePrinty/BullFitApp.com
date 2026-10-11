package com.bullfitapp.bullfit.split;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrowseService {

    private static final int PAGE_SIZE = 12;

    private final BullSplitRepository splitRepository;

    public BrowseService(BullSplitRepository splitRepository) {
        this.splitRepository = splitRepository;
    }

    @Transactional(readOnly = true)
    public Page<BrowseRow> browse(Integer days, String q, boolean popular, int page) {
        Integer validDays = (days != null && days >= 1 && days <= 8) ? days : null;
        String query = (q == null) ? "" : q.trim();
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE);
        return popular
                ? splitRepository.browsePopular(validDays, query, pageable)
                : splitRepository.browseNewest(validDays, query, pageable);
    }
}
