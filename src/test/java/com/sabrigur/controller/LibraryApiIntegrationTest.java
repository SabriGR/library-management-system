package com.sabrigur.controller;

import com.jayway.jsonpath.JsonPath;
import com.sabrigur.config.DataSeeder;
import com.sabrigur.starter.LibraryManagmentSystemApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests of the REST API with the real security rules and the demo data of the DataSeeder.
 * Every test creates its own books and members, so the tests do not depend on each other.
 */
@SpringBootTest(classes = LibraryManagmentSystemApplication.class)
@AutoConfigureMockMvc
class LibraryApiIntegrationTest {

    private static final AtomicInteger COUNTER = new AtomicInteger(1000);

    @Autowired
    private MockMvc mvc;

    private RequestPostProcessor librarian() {
        return httpBasic(DataSeeder.LIBRARIAN_EMAIL, DataSeeder.LIBRARIAN_PASSWORD);
    }

    private RequestPostProcessor admin() {
        return httpBasic(DataSeeder.ADMIN_EMAIL, DataSeeder.ADMIN_PASSWORD);
    }

    private long id(ResultActions result) throws Exception {
        Number n = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
        return n.longValue();
    }

    /** Registers a new member (public endpoint) and returns the id. */
    private long registerMember(String email) throws Exception {
        return id(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Test\",\"lastName\":\"User\",\"email\":\"" + email
                                + "\",\"phoneNumber\":\"555\",\"password\":\"password1\"}"))
                .andExpect(status().isCreated()));
    }

    private long addBook(int copies) throws Exception {
        int n = COUNTER.incrementAndGet();
        return id(mvc.perform(post("/api/books").with(librarian()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test Book " + n + "\",\"author\":\"Tester\",\"isbn\":\"TST-" + n
                                + "\",\"category\":\"Testing\",\"totalCopies\":" + copies + "}"))
                .andExpect(status().isCreated()));
    }

    private ResultActions borrow(long memberId, long bookId) throws Exception {
        return mvc.perform(post("/api/library/borrow").with(librarian())
                .param("userId", String.valueOf(memberId)).param("bookId", String.valueOf(bookId)));
    }

    // ------------------------------------------------------------------ security

    @Test
    @DisplayName("NFR-004: a request without login is refused (401)")
    void unauthenticated() throws Exception {
        mvc.perform(get("/api/books")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/library/borrow").param("userId", "1").param("bookId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("API documentation (OpenAPI) is public and lists the endpoints")
    void apiDocumentation() throws Exception {
        String docs = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(docs).contains("/api/books", "/api/library/borrow", "basicAuth");
    }

    @Test
    @DisplayName("FR-002: wrong password is refused, correct login returns the profile and role")
    void login() throws Exception {
        mvc.perform(get("/api/auth/me").with(httpBasic(DataSeeder.LIBRARIAN_EMAIL, "wrong-password")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").with(librarian()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("LIBRARIAN"))
                .andExpect(jsonPath("$.email").value(DataSeeder.LIBRARIAN_EMAIL));
    }

    @Test
    @DisplayName("FR-003: a member cannot use librarian functions, a librarian cannot use administrator functions")
    void roleRules() throws Exception {
        String email = "role" + COUNTER.incrementAndGet() + "@test.com";
        registerMember(email);
        RequestPostProcessor member = httpBasic(email, "password1");

        mvc.perform(post("/api/library/borrow").with(member).param("userId", "1").param("bookId", "1"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/members").with(member)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/rules").with(librarian())).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/rules").with(admin())).andExpect(status().isOk())
                .andExpect(jsonPath("$.loanPeriodDays").value(14));
    }

    // ------------------------------------------------------------------ registration

    @Test
    @DisplayName("FR-001: registration is public, the new member can log in, duplicates and bad input are refused")
    void registration() throws Exception {
        String email = "new" + COUNTER.incrementAndGet() + "@test.com";
        registerMember(email);

        mvc.perform(get("/api/auth/me").with(httpBasic(email, "password1")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("MEMBER"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"" + email + "\",\"password\":\"password1\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EmailAlreadyUsed"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ValidationError"));
    }

    // ------------------------------------------------------------------ catalog

    @Test
    @DisplayName("FR-004 to FR-008: add, search, view, update and remove a book")
    void catalog() throws Exception {
        long bookId = addBook(2);

        mvc.perform(get("/api/books").with(librarian()).param("keyword", "tester"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem((int) bookId)));
        mvc.perform(get("/api/books/" + bookId).with(librarian()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.availableCopies").value(2));

        mvc.perform(put("/api/books/" + bookId).with(librarian()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Renamed\",\"author\":\"Tester\",\"isbn\":\"RENAMED-" + bookId
                                + "\",\"category\":\"Testing\",\"totalCopies\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.availableCopies").value(3));

        mvc.perform(delete("/api/books/" + bookId).with(librarian())).andExpect(status().isNoContent());
        mvc.perform(get("/api/books/" + bookId).with(librarian())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("FR-004: duplicate ISBN and invalid copies are refused")
    void catalogValidation() throws Exception {
        long bookId = addBook(1);
        String isbn = JsonPath.read(mvc.perform(get("/api/books/" + bookId).with(librarian()))
                .andReturn().getResponse().getContentAsString(), "$.isbn");

        mvc.perform(post("/api/books").with(librarian()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\",\"author\":\"Y\",\"isbn\":\"" + isbn + "\",\"totalCopies\":1}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DuplicateIsbn"));
        mvc.perform(post("/api/books").with(librarian()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\",\"author\":\"Y\",\"isbn\":\"NEW-ISBN\",\"totalCopies\":0}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ circulation

    @Test
    @DisplayName("FR-013 to FR-018: borrow, no copy left, return, return twice")
    void borrowAndReturn() throws Exception {
        long memberId = registerMember("loan" + COUNTER.incrementAndGet() + "@test.com");
        long otherId = registerMember("loan" + COUNTER.incrementAndGet() + "@test.com");
        long bookId = addBook(1);

        long recordId = id(borrow(memberId, bookId).andExpect(status().isOk())
                .andExpect(jsonPath("$.returnDate").doesNotExist())
                .andExpect(jsonPath("$.overdue").value(false)));

        mvc.perform(get("/api/books/" + bookId).with(librarian())).andExpect(jsonPath("$.availableCopies").value(0));
        borrow(otherId, bookId).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BookNotAvailable"));

        mvc.perform(post("/api/library/return/" + recordId).with(librarian()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.returnDate").exists());
        mvc.perform(get("/api/books/" + bookId).with(librarian())).andExpect(jsonPath("$.availableCopies").value(1));
        mvc.perform(post("/api/library/return/" + recordId).with(librarian()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BookAlreadyReturned"));
    }

    @Test
    @DisplayName("FR-014: a member cannot have more than 5 active loans")
    void loanLimit() throws Exception {
        long memberId = registerMember("limit" + COUNTER.incrementAndGet() + "@test.com");
        for (int i = 0; i < 5; i++) {
            borrow(memberId, addBook(1)).andExpect(status().isOk());
        }
        borrow(memberId, addBook(1)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LoanLimitReached"));
    }

    @Test
    @DisplayName("FR-011/FR-014: an inactive member cannot borrow; unknown ids give 404")
    void inactiveAndUnknown() throws Exception {
        long memberId = registerMember("inactive" + COUNTER.incrementAndGet() + "@test.com");
        long bookId = addBook(1);
        mvc.perform(patch("/api/members/" + memberId + "/deactivate").with(librarian())).andExpect(status().isOk());

        borrow(memberId, bookId).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("UserInactive"));
        borrow(999999, bookId).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("UserNotFound"));
        mvc.perform(post("/api/library/return/999999").with(librarian())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("FR-019 to FR-021: renew once, then the renewal is refused; members see only their own loans")
    void renewAndOwnLoans() throws Exception {
        String email = "renew" + COUNTER.incrementAndGet() + "@test.com";
        long memberId = registerMember(email);
        long otherId = registerMember("other" + COUNTER.incrementAndGet() + "@test.com");
        RequestPostProcessor member = httpBasic(email, "password1");
        long recordId = id(borrow(memberId, addBook(2)).andExpect(status().isOk()));

        mvc.perform(post("/api/library/renew/" + recordId).with(member))
                .andExpect(status().isOk()).andExpect(jsonPath("$.renewalCount").value(1));
        mvc.perform(post("/api/library/renew/" + recordId).with(member))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RenewalNotAllowed"));

        mvc.perform(get("/api/library/user/" + memberId + "/active").with(member))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/library/user/" + memberId + "/history").with(member)).andExpect(status().isOk());
        mvc.perform(get("/api/library/user/" + otherId + "/active").with(member)).andExpect(status().isForbidden());
        mvc.perform(get("/api/library/user/" + otherId + "/active").with(librarian())).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ reservation

    @Test
    @DisplayName("FR-023 to FR-025: reserve, notification on return, cancel")
    void reservation() throws Exception {
        String email = "res" + COUNTER.incrementAndGet() + "@test.com";
        long borrowerId = registerMember("borrower" + COUNTER.incrementAndGet() + "@test.com");
        registerMember(email);
        RequestPostProcessor member = httpBasic(email, "password1");
        long bookId = addBook(1);

        mvc.perform(post("/api/reservations").with(member).param("bookId", String.valueOf(bookId)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BookAvailable"));

        long recordId = id(borrow(borrowerId, bookId).andExpect(status().isOk()));

        long reservationId = id(mvc.perform(post("/api/reservations").with(member).param("bookId", String.valueOf(bookId)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("WAITING")));
        mvc.perform(post("/api/reservations").with(member).param("bookId", String.valueOf(bookId)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("AlreadyReserved"));

        // the book is reserved, so the borrower cannot renew
        // (the borrower is not logged in as a member here, so only the return is tested)
        mvc.perform(post("/api/library/return/" + recordId).with(librarian())).andExpect(status().isOk());
        mvc.perform(get("/api/reservations/my").with(member))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("READY"));

        mvc.perform(delete("/api/reservations/" + reservationId).with(member))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ------------------------------------------------------------------ overdue and fines (demo data)

    @Test
    @DisplayName("FR-022, FR-026 to FR-028: overdue list, reminder, late return creates a fine, fine payment")
    void overdueAndFine() throws Exception {
        mvc.perform(get("/api/library/overdue").with(librarian()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].bookTitle", hasItem("Computer Organization and Design")));
        mvc.perform(post("/api/library/overdue/remind").with(librarian()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.emailsSent").isNumber());

        // the demo overdue loan: due 6 days ago, fine = 6 x 0.50 = 3.00
        String overdue = mvc.perform(get("/api/library/overdue").with(librarian())).andReturn().getResponse().getContentAsString();
        Number recordId = JsonPath.<java.util.List<Integer>>read(overdue, "$[?(@.bookTitle=='Computer Organization and Design')].id").get(0);

        mvc.perform(post("/api/library/return/" + recordId).with(librarian()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fineAmount").value(3.0));

        String unpaid = mvc.perform(get("/api/fines/unpaid").with(librarian()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Number fineId = JsonPath.<java.util.List<Integer>>read(unpaid, "$[?(@.recordId==" + recordId + ")].id").get(0);

        mvc.perform(post("/api/fines/" + fineId + "/pay").with(librarian()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paid").value(true));
        mvc.perform(post("/api/fines/" + fineId + "/pay").with(librarian()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("FineAlreadyPaid"));
    }

    // ------------------------------------------------------------------ reports and administration

    @Test
    @DisplayName("FR-031: reports are available for librarians only")
    void reports() throws Exception {
        mvc.perform(get("/api/reports/most-borrowed").with(librarian()).param("limit", "3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].timesBorrowed").isNumber());
        mvc.perform(get("/api/reports/member-activity").with(librarian()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].totalLoans").isNumber());
        mvc.perform(get("/api/reports/overdue").with(librarian())).andExpect(status().isOk());

        String email = "rep" + COUNTER.incrementAndGet() + "@test.com";
        registerMember(email);
        mvc.perform(get("/api/reports/most-borrowed").with(httpBasic(email, "password1"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FR-029: administrator creates and deactivates staff; a deactivated account cannot log in")
    void staffManagement() throws Exception {
        String email = "staff" + COUNTER.incrementAndGet() + "@library.com";
        long staffId = id(mvc.perform(post("/api/admin/staff").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"New\",\"lastName\":\"Librarian\",\"email\":\"" + email
                                + "\",\"password\":\"password1\",\"employeeNo\":\"L-9\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("LIBRARIAN")));

        mvc.perform(get("/api/auth/me").with(httpBasic(email, "password1"))).andExpect(status().isOk());
        mvc.perform(patch("/api/admin/staff/" + staffId + "/deactivate").with(admin())).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").with(httpBasic(email, "password1"))).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/admin/staff").with(admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].email", hasItem(DataSeeder.ADMIN_EMAIL)));
        mvc.perform(patch("/api/admin/staff/1/deactivate").with(admin()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CannotDeactivateSelf"));
    }

    @Test
    @DisplayName("FR-030: library rules can be changed by the administrator, invalid values are refused")
    void rules() throws Exception {
        mvc.perform(put("/api/admin/rules").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loanPeriodDays\":14,\"maxLoansPerMember\":5,\"maxRenewals\":1,\"finePerDay\":0.50}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.maxLoansPerMember").value(5));
        mvc.perform(put("/api/admin/rules").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loanPeriodDays\":0,\"maxLoansPerMember\":5,\"maxRenewals\":1,\"finePerDay\":0.50}"))
                .andExpect(status().isBadRequest());
    }
}
