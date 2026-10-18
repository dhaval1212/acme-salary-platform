package com.acme.compensation.analytics;

import com.acme.compensation.analytics.dto.DepartmentStats;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final AnalyticsRepository repository;

    public AnalyticsService(AnalyticsRepository repository) {
        this.repository = repository;
    }

    public List<DepartmentStats> getDepartmentStats(String department, String country) {
        return repository.findDepartmentStatsRaw(LocalDate.now(), department, country)
                .stream()
                .map(this::mapRow)
                .toList();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private DepartmentStats mapRow(Object[] row) {
        return new DepartmentStats(
                (String) row[0],
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).longValue(),
                toBigDecimal(row[4]),
                toBigDecimal(row[5]),
                toBigDecimal(row[6]),
                toBigDecimal(row[7])
        );
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(value.toString());
    }
}
