package com.duewise.entity.reminder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A number of days past the due date on which a client reminder is sent, e.g. 7. */
@Entity
@Table(name = "reminder_offset")
public class ReminderOffset {

    @Id
    @Column(name = "days_overdue")
    private Short daysOverdue;

    protected ReminderOffset() {
        // for JPA
    }

    public ReminderOffset(short daysOverdue) {
        this.daysOverdue = daysOverdue;
    }

    public short getDaysOverdue() {
        return daysOverdue;
    }
}
