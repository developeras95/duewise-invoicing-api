package com.duewise.service.report;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.duewise.dto.report.RevenueMonth;
import com.duewise.dto.report.RevenueRefreshResponse;
import com.duewise.dto.report.RevenueReportResponse;
import com.duewise.dto.report.RevenueTotal;
import com.duewise.exception.BadRequestException;
import com.duewise.repository.report.RevenueReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Money received per month and currency.
 *
 * <p>Adding up every payment of a year joined to its invoice takes most of a second at millions of
 * payments, so finished months are kept in a stored copy (refreshed nightly and on request) and only the
 * current month is added up live. A payment recorded today therefore shows at once; a payment backdated
 * into a finished month shows after the next refresh, which the report states with storedAsOf.
 */
@Service
@Transactional(readOnly = true)
public class RevenueReportService {

    /** Ten years: enough for any report, and it keeps a single request bounded. */
    static final int MAX_MONTHS = 120;

    private static final Logger log = LoggerFactory.getLogger(RevenueReportService.class);

    private final RevenueReportRepository repository;
    private final Clock clock;

    public RevenueReportService(RevenueReportRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * The report from {@code from} to {@code to}, both included. Without {@code to}, it ends with the
     * current month; without {@code from}, it covers twelve months.
     */
    public RevenueReportResponse report(YearMonth from, YearMonth to) {
        YearMonth current = YearMonth.now(clock);
        YearMonth end = to != null ? to : current;
        YearMonth start = from != null ? from : end.minusMonths(11);
        if (start.isAfter(end)) {
            throw new BadRequestException("INVALID_MONTH_RANGE", "from must be on or before to.");
        }
        if (ChronoUnit.MONTHS.between(start, end) >= MAX_MONTHS) {
            throw new BadRequestException("MONTH_RANGE_TOO_LONG",
                    "A report can cover at most " + MAX_MONTHS + " months.");
        }

        List<RevenueMonth> months = new ArrayList<>();
        YearMonth lastStored = end.isBefore(current) ? end : current.minusMonths(1);
        if (!start.isAfter(lastStored)) {
            months.addAll(repository.storedMonths(start, lastStored));
        }
        YearMonth liveMonth = null;
        if (!start.isAfter(current) && !end.isBefore(current)) {
            liveMonth = current;
            months.addAll(repository.liveMonth(current));
        }
        return new RevenueReportResponse(start, end, months, totals(months), repository.refreshedAt(), liveMonth);
    }

    /** Brings the stored months up to date with every payment recorded so far. */
    @Transactional
    public RevenueRefreshResponse refresh() {
        long started = System.nanoTime();
        Instant refreshedAt = repository.refresh();
        long tookMs = (System.nanoTime() - started) / 1_000_000;
        log.info("Revenue report refreshed in {} ms", tookMs);
        return new RevenueRefreshResponse(refreshedAt, tookMs);
    }

    private static List<RevenueTotal> totals(List<RevenueMonth> months) {
        Map<String, RevenueTotal> byCurrency = new TreeMap<>();
        for (RevenueMonth month : months) {
            byCurrency.merge(month.currencyCode(),
                    new RevenueTotal(month.currencyCode(), month.payments(), month.received()),
                    (a, b) -> new RevenueTotal(a.currencyCode(), a.payments() + b.payments(),
                            a.received().add(b.received())));
        }
        return List.copyOf(byCurrency.values());
    }
}
