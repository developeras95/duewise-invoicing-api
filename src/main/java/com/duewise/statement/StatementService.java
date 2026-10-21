package com.duewise.statement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.duewise.client.ClientResponse;
import com.duewise.client.ClientService;
import com.duewise.statement.StatementRepository.OpenInvoiceRow;
import com.duewise.statement.StatementResponse.CurrencyBalance;
import com.duewise.statement.StatementResponse.OpenInvoice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatementService {

    private final StatementRepository repository;
    private final ClientService clientService;
    private final Clock clock;

    public StatementService(StatementRepository repository, ClientService clientService, Clock clock) {
        this.repository = repository;
        this.clientService = clientService;
        this.clock = clock;
    }

    public StatementResponse forClient(Long clientId) {
        ClientResponse client = clientService.get(clientId);   // 404 if the client does not exist
        LocalDate today = LocalDate.now(clock);
        List<OpenInvoiceRow> rows = repository.findOpenInvoices(clientId, today);

        List<OpenInvoice> openInvoices = new ArrayList<>(rows.size());
        Map<String, Totals> byCurrency = new LinkedHashMap<>();
        for (OpenInvoiceRow row : rows) {
            AgingBucket bucket = AgingBucket.of(row.daysOverdue());
            BigDecimal balance = scaled(row.balanceDue(), row.minorUnits());
            openInvoices.add(new OpenInvoice(row.id(), row.invoiceNumber(), row.currencyCode(), row.issueDate(),
                    row.dueDate(), row.daysOverdue(), bucket, balance));
            byCurrency.computeIfAbsent(row.currencyCode(), code -> new Totals(row.minorUnits())).add(bucket, balance);
        }

        List<CurrencyBalance> balances = byCurrency.entrySet().stream()
                .map(entry -> entry.getValue().toBalance(entry.getKey()))
                .toList();
        return new StatementResponse(client.id(), client.name(), today, balances, openInvoices);
    }

    private static BigDecimal scaled(BigDecimal amount, int minorUnits) {
        return amount.setScale(minorUnits, RoundingMode.HALF_UP);
    }

    /** Running totals for one currency. */
    private static final class Totals {
        private final int minorUnits;
        private final Map<AgingBucket, BigDecimal> buckets = new EnumMap<>(AgingBucket.class);
        private int count;

        Totals(int minorUnits) {
            this.minorUnits = minorUnits;
            for (AgingBucket bucket : AgingBucket.values()) {
                buckets.put(bucket, scaled(BigDecimal.ZERO, minorUnits));
            }
        }

        void add(AgingBucket bucket, BigDecimal amount) {
            buckets.merge(bucket, amount, BigDecimal::add);
            count++;
        }

        CurrencyBalance toBalance(String currencyCode) {
            BigDecimal total = buckets.values().stream().reduce(scaled(BigDecimal.ZERO, minorUnits), BigDecimal::add);
            return new CurrencyBalance(currencyCode, count, total,
                    buckets.get(AgingBucket.CURRENT),
                    buckets.get(AgingBucket.DAYS_1_30),
                    buckets.get(AgingBucket.DAYS_31_60),
                    buckets.get(AgingBucket.DAYS_61_90),
                    buckets.get(AgingBucket.OVER_90));
        }
    }
}
