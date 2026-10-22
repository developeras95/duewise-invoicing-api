package com.duewise.reminder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings/reminders")
@Tag(name = "Reminders", description = "Overdue reminders to clients and the owner's daily summary")
public class ReminderSettingsController {

    private final ReminderSettingsService service;

    public ReminderSettingsController(ReminderSettingsService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Get reminder settings")
    public ReminderSettingsResponse get() {
        return service.get();
    }

    @PutMapping
    @Operation(summary = "Replace reminder settings",
            description = "Switches, owner-summary recipients (TO / CC) and reminder days, all at once. "
                    + "Requires the current 'version'. Takes effect on the next run, no restart needed.")
    public ReminderSettingsResponse update(@Valid @RequestBody ReminderSettingsRequest request) {
        return service.update(request);
    }
}
