package com.duewise.controller.report;

import java.time.YearMonth;

import com.duewise.dto.report.RevenueRefreshResponse;
import com.duewise.dto.report.RevenueReportResponse;
import com.duewise.service.report.RevenueReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Money received over time")
public class ReportController {

    private final RevenueReportService service;

    public ReportController(RevenueReportService service) {
        this.service = service;
    }

    @GetMapping("/revenue")
    @Operation(summary = "Revenue per month",
            description = "Money received per month and currency, with totals per currency. Finished months come "
                    + "from the stored report, refreshed nightly (see storedAsOf); the current month is always live.")
    public RevenueReportResponse revenue(
            @Parameter(description = "First month, e.g. 2025-11. Default: eleven months before 'to'.", example = "2025-11")
            @RequestParam(required = false) YearMonth from,
            @Parameter(description = "Last month, included, e.g. 2026-10. Default: the current month.", example = "2026-10")
            @RequestParam(required = false) YearMonth to) {
        return service.report(from, to);
    }

    @PostMapping("/revenue/refresh")
    @Operation(summary = "Refresh the stored revenue report now",
            description = "Recomputes every finished month from the payments, as the nightly job does. Use after "
                    + "recording payments for a past month. Takes about a second per few million payments.")
    public RevenueRefreshResponse refresh() {
        return service.refresh();
    }
}
