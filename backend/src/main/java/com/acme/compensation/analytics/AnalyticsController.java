package com.acme.compensation.analytics;

import com.acme.compensation.analytics.dto.DepartmentStats;
import com.acme.compensation.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    /**
     * Salary statistics grouped by department.
     * Optional filters: department and country.
     */
    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<DepartmentStats>>> byDepartment(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String country
    ) {
        return ResponseEntity.ok(ApiResponse.ok(service.getDepartmentStats(department, country)));
    }
}
