package com.duewise.reminder;

import org.springframework.data.jpa.repository.JpaRepository;

interface ReminderSettingsRepository extends JpaRepository<ReminderSettings, Short> {
}
