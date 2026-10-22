package com.duewise.reminder;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface ReminderRecipientRepository extends JpaRepository<ReminderRecipient, Long> {

    List<ReminderRecipient> findAllByOrderByDeliveryTypeAscEmailAsc();
}
