package com.bullfitapp.bullfit.weight;

import com.bullfitapp.bullfit.common.ZoneOptions;
import com.bullfitapp.bullfit.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class WeightService {

    private static final BigDecimal MIN = new BigDecimal("50");
    private static final BigDecimal MAX = new BigDecimal("1000");
    private static final LocalDate EARLIEST = LocalDate.of(2000, 1, 1);

    private final BodyWeightLogRepository repository;
    private final UserRepository userRepository;

    public WeightService(BodyWeightLogRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    /** Today's date in the user's own time zone. */
    public static LocalDate todayFor(String timeZone) {
        ZoneId zone = ZoneOptions.isValid(timeZone) ? ZoneId.of(timeZone) : ZoneId.of(ZoneOptions.DEFAULT);
        return LocalDate.now(zone);
    }

    @Transactional(readOnly = true)
    public List<BodyWeightLog> entriesOldestFirst(Long userId) {
        return repository.findByUserIdOrderByLoggedOnAsc(userId);
    }

    @Transactional
    public void log(Long userId, String timeZone, LocalDate date, BigDecimal weight) {
        LocalDate today = todayFor(timeZone);
        final LocalDate day = (date == null) ? today : date;
        if (day.isAfter(today) || day.isBefore(EARLIEST)) {
            throw new WeightException("Pick a date that isn't in the future.");
        }
        if (weight == null || weight.compareTo(MIN) < 0 || weight.compareTo(MAX) > 0) {
            throw new WeightException("Weight must be between 50 and 1000 lbs.");
        }
        BodyWeightLog entry = repository.findByUserIdAndLoggedOn(userId, day).orElseGet(() -> {
            BodyWeightLog created = new BodyWeightLog();
            created.setUserId(userId);
            created.setLoggedOn(day);
            return created;
        });
        entry.setWeight(weight.setScale(1, RoundingMode.HALF_UP));
        repository.save(entry);
        syncProfileWeight(userId);
    }

    @Transactional
    public void delete(Long userId, Long entryId) {
        BodyWeightLog entry = repository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        repository.delete(entry);
        repository.flush();
        syncProfileWeight(userId);
    }

    /** Keeps users.weight equal to the most recent entry, so there's one source of truth. */
    private void syncProfileWeight(Long userId) {
        BigDecimal latest = repository.findFirstByUserIdOrderByLoggedOnDesc(userId)
                .map(BodyWeightLog::getWeight).orElse(null);
        userRepository.findById(userId).ifPresent(user -> {
            user.setWeight(latest);
            userRepository.save(user);
        });
    }
}
