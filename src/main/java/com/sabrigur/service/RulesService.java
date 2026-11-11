package com.sabrigur.service;

import com.sabrigur.dto.RulesRequest;
import com.sabrigur.entity.LibraryRules;
import com.sabrigur.repository.LibraryRulesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RulesService {

    private final LibraryRulesRepository rulesRepository;
    private final AuditService audit;

    /** Returns the rules. The default rules are created on first use. */
    @Transactional
    public LibraryRules getRules() {
        return rulesRepository.findById(LibraryRules.SINGLETON_ID)
                .orElseGet(() -> rulesRepository.save(LibraryRules.builder().build()));
    }

    @Transactional
    public LibraryRules update(RulesRequest request) {
        LibraryRules rules = getRules();
        rules.setLoanPeriodDays(request.loanPeriodDays());
        rules.setMaxLoansPerMember(request.maxLoansPerMember());
        rules.setMaxRenewals(request.maxRenewals());
        rules.setFinePerDay(request.finePerDay());
        audit.log("RULES_UPDATE", "loanPeriodDays=" + rules.getLoanPeriodDays()
                + " maxLoans=" + rules.getMaxLoansPerMember() + " finePerDay=" + rules.getFinePerDay());
        return rulesRepository.save(rules);
    }
}
