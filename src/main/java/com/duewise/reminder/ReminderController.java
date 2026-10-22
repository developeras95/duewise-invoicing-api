package com.duewise.reminder;

import com.duewise.common.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reminders")
@Tag(name = "Reminders")
public class ReminderController {

    private final ReminderService service;

    public ReminderController(ReminderService service) {
        this.service = service;
    }

    @PostMapping("/run")
    @Operation(summary = "Run the reminder job now",
            description = "Does exactly what the daily schedule does. Safe to repeat: reminders already sent are "
                    + "never sent again, and the owner summary goes out at most once a day.")
    public ReminderRunSummary run() {
        return service.run();
    }

    @GetMapping("/log")
    @Operation(summary = "Reminder history", description = "Every attempt, newest first, including failures and their errors.")
    public PageResponse<ReminderLogResponse> log(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.history(PageRequest.of(page, size));
    }
}
