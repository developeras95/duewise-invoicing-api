package com.duewise.repository.reminder;

import java.util.List;

import com.duewise.entity.reminder.ReminderOffset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderOffsetRepository extends JpaRepository<ReminderOffset, Short> {

    List<ReminderOffset> findAllByOrderByDaysOverdueAsc();
}
