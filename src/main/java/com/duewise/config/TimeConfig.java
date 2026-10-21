package com.duewise.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One clock for the whole application, in the business's own time zone. "Today" decides default
 * issue dates and whether an invoice is overdue, so it must not depend on the server's zone.
 * Tests replace this bean with a fixed clock to control dates.
 */
@Configuration
public class TimeConfig {

    @Bean
    Clock clock(@Value("${duewise.business.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
