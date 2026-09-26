package com.acme.compensation.employee;

import com.acme.compensation.common.ConflictException;
import com.acme.compensation.common.ResourceNotFoundException;
import com.acme.compensation.employee.dto.EmployeeRequest;
import com.acme.compensation.employee.dto.EmployeeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository repository;

    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(String department, String country,
                                       EmployeeStatus status, String search,
                                       Pageable pageable) {
        return repository.findAllFiltered(department, country, status, search, pageable)
                .map(EmployeeResponse::from);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(UUID id) {
        return repository.findById(id)
                .map(EmployeeResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    public EmployeeResponse create(EmployeeRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new ConflictException("An employee with email " + request.email() + " already exists");
        }
        Employee employee = new Employee();
        applyRequest(employee, request);
        return EmployeeResponse.from(repository.save(employee));
    }

    public EmployeeResponse update(UUID id, EmployeeRequest request) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));

        if (repository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ConflictException("An employee with email " + request.email() + " already exists");
        }
        applyRequest(employee, request);
        return EmployeeResponse.from(repository.save(employee));
    }

    public EmployeeResponse deactivate(UUID id) {
        Employee employee = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
        employee.setStatus(EmployeeStatus.INACTIVE);
        return EmployeeResponse.from(repository.save(employee));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void applyRequest(Employee employee, EmployeeRequest request) {
        employee.setFullName(request.fullName());
        employee.setEmail(request.email().toLowerCase());
        employee.setDepartment(request.department());
        employee.setJobTitle(request.jobTitle());
        employee.setCountry(request.country());
    }
}
