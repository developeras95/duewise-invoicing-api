package com.duewise.repository.reminder;

import java.util.List;

import com.duewise.entity.reminder.ReminderRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRecipientRepository extends JpaRepository<ReminderRecipient, Long> {

    List<ReminderRecipient> findAllByOrderByDeliveryTypeAscEmailAsc();
}
