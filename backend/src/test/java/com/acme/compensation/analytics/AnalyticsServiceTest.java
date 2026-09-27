package com.acme.compensation.analytics;

import com.acme.compensation.analytics.dto.DepartmentStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for AnalyticsService.
 * Focuses on the mapRow projection logic and filter pass-through.
 * SQL correctness is verified by repository integration tests.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    AnalyticsRepository repository;

    @InjectMocks
    AnalyticsService service;

    // ── getDepartmentStats ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getDepartmentStats()")
    class GetDepartmentStats {

        @Test
        @DisplayName("maps raw Object[] rows to DepartmentStats correctly")
        void mapsRowsCorrectly() {
            Object[] row = new Object[]{
                    "Engineering",
                    "United States",
                    "USD",
                    5L,
                    new BigDecimal("95000.00"),
                    new BigDecimal("92000.00"),
                    new BigDecimal("80000.00"),
                    new BigDecimal("110000.00")
            };
            ArrayList<Object[]> rows = new ArrayList<>();
            rows.add(row);
            given(repository.findDepartmentStatsRaw(any(LocalDate.class), isNull(), isNull()))
                    .willReturn(rows);

            List<DepartmentStats> result = service.getDepartmentStats(null, null);

            assertThat(result).hasSize(1);
            DepartmentStats stats = result.get(0);
            assertThat(stats.department()).isEqualTo("Engineering");
            assertThat(stats.country()).isEqualTo("United States");
            assertThat(stats.currency()).isEqualTo("USD");
            assertThat(stats.headcount()).isEqualTo(5L);
            assertThat(stats.avgSalary()).isEqualByComparingTo("95000.00");
            assertThat(stats.medianSalary()).isEqualByComparingTo("92000.00");
            assertThat(stats.minSalary()).isEqualByComparingTo("80000.00");
            assertThat(stats.maxSalary()).isEqualByComparingTo("110000.00");
        }

        @Test
        @DisplayName("returns empty list when repository returns no rows")
        void returnsEmptyList() {
            ArrayList<Object[]> empty = new ArrayList<>();
            given(repository.findDepartmentStatsRaw(any(), any(), any())).willReturn(empty);

            List<DepartmentStats> result = service.getDepartmentStats(null, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("passes department and country filters through to repository")
        void passesFiltersToRepository() {
            ArrayList<Object[]> empty = new ArrayList<>();
            given(repository.findDepartmentStatsRaw(any(), eq("Engineering"), eq("US")))
                    .willReturn(empty);

            service.getDepartmentStats("Engineering", "US");

            then(repository).should()
                    .findDepartmentStatsRaw(any(LocalDate.class), eq("Engineering"), eq("US"));
        }

        @Test
        @DisplayName("uses today as the asOf date")
        void usesTodayAsAsOfDate() {
            ArrayList<Object[]> empty = new ArrayList<>();
            given(repository.findDepartmentStatsRaw(any(), any(), any())).willReturn(empty);

            service.getDepartmentStats(null, null);

            then(repository).should()
                    .findDepartmentStatsRaw(eq(LocalDate.now()), any(), any());
        }

        @Test
        @DisplayName("handles null salary fields gracefully — defaults to zero")
        void handlesNullAmountFields() {
            Object[] rowWithNulls = new Object[]{"HR", "United States", "USD", 2L, null, null, null, null};
            ArrayList<Object[]> rows = new ArrayList<>();
            rows.add(rowWithNulls);
            given(repository.findDepartmentStatsRaw(any(), any(), any()))
                    .willReturn(rows);

            List<DepartmentStats> result = service.getDepartmentStats(null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).avgSalary()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.get(0).medianSalary()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("maps multiple department rows correctly")
        void mapsMultipleRows() {
            Object[] eng = {"Engineering", "United States", "USD", 10L,
                    new BigDecimal("100000.00"), new BigDecimal("98000.00"),
                    new BigDecimal("75000.00"), new BigDecimal("130000.00")};
            Object[] hr = {"HR", "United Kingdom", "GBP", 3L,
                    new BigDecimal("65000.00"), new BigDecimal("63000.00"),
                    new BigDecimal("55000.00"), new BigDecimal("70000.00")};
            List<Object[]> rows = new java.util.ArrayList<>();
            rows.add(eng);
            rows.add(hr);
            given(repository.findDepartmentStatsRaw(any(), any(), any()))
                    .willReturn(rows);

            List<DepartmentStats> result = service.getDepartmentStats(null, null);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(DepartmentStats::department)
                    .containsExactly("Engineering", "HR");
            assertThat(result).extracting(DepartmentStats::country)
                    .containsExactly("United States", "United Kingdom");
            assertThat(result).extracting(DepartmentStats::currency)
                    .containsExactly("USD", "GBP");
            assertThat(result).extracting(DepartmentStats::headcount)
                    .containsExactly(10L, 3L);
        }
    }
}
