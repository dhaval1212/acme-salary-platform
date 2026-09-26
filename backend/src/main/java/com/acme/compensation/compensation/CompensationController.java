package com.acme.compensation.compensation;

import com.acme.compensation.common.ApiResponse;
import com.acme.compensation.compensation.dto.CompensationRequest;
import com.acme.compensation.compensation.dto.CompensationResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees/{employeeId}/compensation")
public class CompensationController {

    private final CompensationService service;

    public CompensationController(CompensationService service) {
        this.service = service;
    }

    /** Current salary only — the most recent record with effective_date <= today */
    @GetMapping
    public ResponseEntity<ApiResponse<CompensationResponse>> getCurrent(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getCurrent(employeeId)));
    }

    /** Full compensation history, newest first */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<CompensationResponse>>> getHistory(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getHistory(employeeId)));
    }

    /** Record a new salary change (append-only) */
    @PostMapping
    public ResponseEntity<ApiResponse<CompensationResponse>> record(
            @PathVariable UUID employeeId,
            @Valid @RequestBody CompensationRequest request
    ) {
        CompensationResponse created = service.record(employeeId, request);
        return ResponseEntity
                .created(URI.create("/api/v1/employees/" + employeeId + "/compensation/history"))
                .body(ApiResponse.ok(created));
    }
}
