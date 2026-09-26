package com.acme.compensation.compensation;

import com.acme.compensation.common.ConflictException;
import com.acme.compensation.common.ResourceNotFoundException;
import com.acme.compensation.compensation.dto.CompensationRequest;
import com.acme.compensation.compensation.dto.CompensationResponse;
import com.acme.compensation.employee.Employee;
import com.acme.compensation.employee.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CompensationService {

    private final CompensationRepository compensationRepository;
    private final EmployeeRepository employeeRepository;

    public CompensationService(CompensationRepository compensationRepository,
                               EmployeeRepository employeeRepository) {
        this.compensationRepository = compensationRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public CompensationResponse getCurrent(UUID employeeId) {
        assertEmployeeExists(employeeId);
        return compensationRepository
                .findCurrentByEmployeeId(employeeId, LocalDate.now())
                .map(CompensationResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Compensation record", employeeId));
    }

    @Transactional(readOnly = true)
    public List<CompensationResponse> getHistory(UUID employeeId) {
        assertEmployeeExists(employeeId);
        return compensationRepository.findHistoryByEmployeeId(employeeId)
                .stream()
                .map(CompensationResponse::from)
                .toList();
    }

    public CompensationResponse record(UUID employeeId, CompensationRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        if (compensationRepository.existsByEmployeeIdAndEffectiveDate(employeeId, request.effectiveDate())) {
            throw new ConflictException(
                    "A compensation record for this employee already exists on " + request.effectiveDate()
            );
        }

        CompensationHistory entry = new CompensationHistory();
        entry.setEmployee(employee);
        entry.setAmount(request.amount());
        entry.setCurrency(request.currency().toUpperCase());
        entry.setEffectiveDate(request.effectiveDate());
        entry.setChangedBy(request.changedBy());

        // NOTE: amount is intentionally not logged here (salary is sensitive)
        return CompensationResponse.from(compensationRepository.save(entry));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void assertEmployeeExists(UUID employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee", employeeId);
        }
    }
}
