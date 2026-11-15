package com.duewise.dto.report;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RevenueReport", description = """
        Money received per month and currency. Amounts in different currencies are never added together.
        Finished months come from the stored report (see storedAsOf); the current month, when it is in the
        period, is added up live from the payments (see liveMonth).""")
public record RevenueReportResponse(
        @Schema(type = "string", example = "2025-11") YearMonth from,
        @Schema(type = "string", example = "2026-10") YearMonth to,
        @Schema(description = "One entry per month and currency that received payments, oldest month first")
        List<RevenueMonth> months,
        @Schema(description = "Totals for the whole period, one per currency")
        List<RevenueTotal> totals,
        @Schema(description = "When the stored months were last brought up to date. Payments recorded later "
                + "for a finished month appear after the next refresh.")
        Instant storedAsOf,
        @Schema(type = "string", example = "2026-11",
                description = "The month added up live from the payments, so always up to date; null when the "
                        + "period ends before the current month")
        YearMonth liveMonth) {
}
