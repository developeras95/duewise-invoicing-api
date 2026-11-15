package com.duewise.service.report;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Refreshes the stored revenue report on the configured schedule (duewise.reports.refresh-cron, 02:00
 * every day by default) in the business time zone. Switched off with duewise.reports.refresh-enabled=false.
 */
@Component
@ConditionalOnProperty(prefix = "duewise.reports", name = "refresh-enabled", havingValue = "true", matchIfMissing = true)
public class RevenueRefreshJob {

    private final RevenueReportService service;

    public RevenueRefreshJob(RevenueReportService service) {
        this.service = service;
    }

    @Scheduled(cron = "${duewise.reports.refresh-cron}", zone = "${duewise.business.time-zone}")
    void refreshNightly() {
        service.refresh();
    }
}
