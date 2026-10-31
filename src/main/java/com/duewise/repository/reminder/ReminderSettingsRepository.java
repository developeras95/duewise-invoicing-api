package com.duewise.repository.reminder;

import com.duewise.entity.reminder.ReminderSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderSettingsRepository extends JpaRepository<ReminderSettings, Short> {
}
