package com.duewise.dto.report;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RevenueRefresh")
public record RevenueRefreshResponse(
        @Schema(description = "The stored report now includes every payment recorded before this moment")
        Instant refreshedAt,
        @Schema(description = "How long the refresh took", example = "1330") long tookMs) {
}
