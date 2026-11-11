package com.sabrigur.service;

import com.sabrigur.dto.StaffRequest;
import com.sabrigur.entity.Librarian;
import com.sabrigur.exception.BusinessRuleException;
import com.sabrigur.exception.NotFoundException;
import com.sabrigur.repository.LibrarianRepository;
import com.sabrigur.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Staff account management by the administrator (FR-029). */
@Service
@RequiredArgsConstructor
public class StaffService {

    private final LibrarianRepository librarianRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<Librarian> findAll() {
        return librarianRepository.findAll();
    }

    @Transactional
    public Librarian create(StaffRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("EmailAlreadyUsed", "The e-mail address " + email + " is already registered");
        }
        Librarian librarian = librarianRepository.save(Librarian.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(email)
                .phoneNumber(request.phoneNumber())
                .employeeNo(request.employeeNo())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build());
        audit.log("STAFF_CREATE", "staff=" + librarian.getId());
        return librarian;
    }

    /** An administrator cannot deactivate his or her own account. */
    @Transactional
    public Librarian setActive(Long id, boolean active, String requesterEmail) {
        Librarian staff = librarianRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("UserNotFound", "Staff member " + id + " not found"));
        if (!active && staff.getEmail().equalsIgnoreCase(requesterEmail)) {
            throw new BusinessRuleException("CannotDeactivateSelf", "You cannot deactivate your own account");
        }
        staff.setActive(active);
        audit.log(active ? "STAFF_ACTIVATE" : "STAFF_DEACTIVATE", "staff=" + id);
        return librarianRepository.save(staff);
    }
}
