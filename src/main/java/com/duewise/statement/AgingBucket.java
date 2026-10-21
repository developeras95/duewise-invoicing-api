package com.duewise.statement;

/** How late an unpaid balance is, in the bands accountants use for receivables ageing. */
public enum AgingBucket {
    CURRENT,
    DAYS_1_30,
    DAYS_31_60,
    DAYS_61_90,
    OVER_90;

    /** 0 or less = not yet due (CURRENT); 1 = due yesterday. */
    public static AgingBucket of(int daysOverdue) {
        if (daysOverdue <= 0) {
            return CURRENT;
        }
        if (daysOverdue <= 30) {
            return DAYS_1_30;
        }
        if (daysOverdue <= 60) {
            return DAYS_31_60;
        }
        if (daysOverdue <= 90) {
            return DAYS_61_90;
        }
        return OVER_90;
    }
}
