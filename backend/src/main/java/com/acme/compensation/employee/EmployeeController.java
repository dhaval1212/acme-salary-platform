package com.acme.compensation.employee;

import com.acme.compensation.common.ApiResponse;
import com.acme.compensation.common.PageMeta;
import com.acme.compensation.employee.dto.EmployeeRequest;
import com.acme.compensation.employee.dto.EmployeeResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<EmployeeResponse>>> list(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        int size = Math.min(pageSize, 100); // enforce max page size
        Page<EmployeeResponse> result = service.list(
                department, country, status, search,
                PageRequest.of(page, size, Sort.by("fullName"))
        );
        PageMeta meta = new PageMeta(page, size, result.getTotalElements(), result.getTotalPages());
        return ResponseEntity.ok(ApiResponse.ok(result.getContent(), meta));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse created = service.create(request);
        return ResponseEntity
                .created(URI.create("/api/v1/employees/" + created.id()))
                .body(ApiResponse.ok(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, request)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<EmployeeResponse>> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.deactivate(id)));
    }
}
