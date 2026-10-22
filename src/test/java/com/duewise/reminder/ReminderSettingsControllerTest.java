package com.duewise.reminder;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.duewise.common.error.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReminderSettingsController.class)
class ReminderSettingsControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ReminderSettingsService service;

    @Test
    void badEmailsAndDaysAreReportedByPosition() throws Exception {
        mvc.perform(put("/api/v1/settings/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerDigestEnabled": true, "clientRemindersEnabled": true,
                                 "recipients": [{"email": "not-an-email", "type": "TO"}, {"email": "cc@acme.in"}],
                                 "reminderDays": [0, 7, 400], "version": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItems(
                        "recipients[0].email", "recipients[1].type", "reminderDays[0]", "reminderDays[2]")));
    }

    @Test
    void atLeastOneReminderDayAndAVersionAreRequired() throws Exception {
        mvc.perform(put("/api/v1/settings/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerDigestEnabled": true, "clientRemindersEnabled": true, "recipients": [], "reminderDays": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItems("reminderDays", "version")));
    }

    @Test
    void duplicateRecipientIsA400() throws Exception {
        when(service.update(any())).thenThrow(
                new BadRequestException("DUPLICATE_RECIPIENT", "owner@acme.in appears more than once in recipients."));

        mvc.perform(put("/api/v1/settings/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ownerDigestEnabled": true, "clientRemindersEnabled": true,
                                 "recipients": [{"email": "owner@acme.in", "type": "TO"}, {"email": "OWNER@acme.in", "type": "CC"}],
                                 "reminderDays": [1, 7], "version": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RECIPIENT"));
    }
}
