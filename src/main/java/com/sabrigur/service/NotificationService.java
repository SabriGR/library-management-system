package com.sabrigur.service;

import com.sabrigur.entity.BorrowingRecord;
import com.sabrigur.repository.BorrowingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Sends e-mail notifications. The external e-mail service is simulated: messages are written to the log.
 * To use a real mail server, only {@link #sendEmail} has to be changed.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    private final BorrowingRecordRepository borrowingRepository;
    private final Clock clock;

    public void sendEmail(String to, String subject, String body) {
        LOG.info("EMAIL to={} subject=\"{}\" body=\"{}\"", to, subject, body);
    }

    /** Sends a reminder to every member who has an overdue loan (FR-026). Returns the number of e-mails. */
    @Transactional(readOnly = true)
    public int sendOverdueReminders() {
        List<BorrowingRecord> overdue = borrowingRepository.findOverdueBorrowings(LocalDate.now(clock));
        for (BorrowingRecord r : overdue) {
            sendEmail(r.getMember().getEmail(), "Overdue book: " + r.getBook().getTitle(),
                    "Dear " + r.getMember().getFullName() + ", the book '" + r.getBook().getTitle()
                            + "' was due on " + r.getDueDate() + ". Please return it as soon as possible.");
        }
        return overdue.size();
    }
}
