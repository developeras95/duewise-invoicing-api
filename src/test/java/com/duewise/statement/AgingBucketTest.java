package com.duewise.statement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AgingBucketTest {

    @ParameterizedTest(name = "{0} days overdue -> {1}")
    @CsvSource({
            "0,   CURRENT",
            "1,   DAYS_1_30",
            "30,  DAYS_1_30",
            "31,  DAYS_31_60",
            "60,  DAYS_31_60",
            "61,  DAYS_61_90",
            "90,  DAYS_61_90",
            "91,  OVER_90",
            "400, OVER_90"
    })
    void bucketEdgesAreInclusiveAtTheTop(int daysOverdue, AgingBucket expected) {
        assertThat(AgingBucket.of(daysOverdue)).isEqualTo(expected);
    }
}
