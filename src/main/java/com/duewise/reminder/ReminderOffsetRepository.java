package com.duewise.reminder;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface ReminderOffsetRepository extends JpaRepository<ReminderOffset, Short> {

    List<ReminderOffset> findAllByOrderByDaysOverdueAsc();
}
