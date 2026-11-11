package com.sabrigur.service;

import com.sabrigur.dto.BorrowingRecordResponse;
import com.sabrigur.dto.MemberDetailsResponse;
import com.sabrigur.dto.MemberUpdateRequest;
import com.sabrigur.dto.RegisterRequest;
import com.sabrigur.dto.UserResponse;
import com.sabrigur.entity.Member;
import com.sabrigur.exception.BusinessRuleException;
import com.sabrigur.exception.NotFoundException;
import com.sabrigur.repository.BorrowingRecordRepository;
import com.sabrigur.repository.MemberRepository;
import com.sabrigur.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Registration and member management (FR-001, FR-009 to FR-012). */
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final BorrowingRecordRepository borrowingRepository;
    private final FineService fineService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    /** Used for self-registration and by librarians who register a member at the desk. */
    @Transactional
    public Member register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("EmailAlreadyUsed", "The e-mail address " + email + " is already registered");
        }
        Member member = memberRepository.save(Member.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(email)
                .phoneNumber(request.phoneNumber())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build());
        audit.log("MEMBER_REGISTER", "member=" + member.getId());
        return member;
    }

    @Transactional(readOnly = true)
    public List<Member> search(String keyword) {
        return memberRepository.search(keyword == null ? "" : keyword.trim());
    }

    @Transactional(readOnly = true)
    public Member get(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("UserNotFound", "Member " + id + " not found"));
    }

    @Transactional
    public Member update(Long id, MemberUpdateRequest request) {
        Member member = get(id);
        member.setFirstName(request.firstName().trim());
        member.setLastName(request.lastName().trim());
        member.setPhoneNumber(request.phoneNumber());
        audit.log("MEMBER_UPDATE", "member=" + id);
        return memberRepository.save(member);
    }

    /** A deactivated member cannot borrow books or log in (FR-011). */
    @Transactional
    public Member setActive(Long id, boolean active) {
        Member member = get(id);
        member.setActive(active);
        audit.log(active ? "MEMBER_ACTIVATE" : "MEMBER_DEACTIVATE", "member=" + id);
        return memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public MemberDetailsResponse details(Long id) {
        Member member = get(id);
        List<BorrowingRecordResponse> loans = borrowingRepository.findByMemberIdAndReturnDateIsNull(id).stream()
                .map(BorrowingRecordResponse::from)
                .toList();
        return new MemberDetailsResponse(UserResponse.from(member), loans, fineService.totalUnpaid(id));
    }
}
