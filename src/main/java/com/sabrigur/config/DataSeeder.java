package com.sabrigur.config;

import com.sabrigur.entity.Book;
import com.sabrigur.entity.BorrowingRecord;
import com.sabrigur.entity.User;
import com.sabrigur.repository.BookRepository;
import com.sabrigur.repository.BorrowingRecordRepository;
import com.sabrigur.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner initData(BookRepository bookRepository,
                                      UserRepository userRepository,
                                      BorrowingRecordRepository borrowingRecordRepository) {
        return args -> {
            
            if (bookRepository.count() == 0 && userRepository.count() == 0) {
                
                
                Book book1 = Book.builder().title("Spring Boot in Action").author("Craig Walls").isbn("9781617292545").totalCopies(5).availableCopies(4).build();
                Book book2 = Book.builder().title("Clean Code").author("Robert C. Martin").isbn("9780132350884").totalCopies(3).availableCopies(3).build();
                Book book3 = Book.builder().title("Computer Organization and Design").author("David A. Patterson").isbn("9780124077263").totalCopies(2).availableCopies(1).build();
                Book book4 = Book.builder().title("Türkiye Coğrafyası").author("Ramazan Özey").isbn("9786053181234").totalCopies(4).availableCopies(4).build();
                Book book5 = Book.builder().title("English Grammar in Use").author("Raymond Murphy").isbn("9781108457651").totalCopies(6).availableCopies(6).build();

                bookRepository.saveAll(List.of(book1, book2, book3, book4, book5));

                
                User user1 = User.builder().firstName("Sabri").lastName("Gür").email("sabri@library.com").phoneNumber("5551112233").isActive(true).build();
                User user2 = User.builder().firstName("Bahri").lastName("Ayzabar").email("bahri@library.com").phoneNumber("5554445566").isActive(true).build();
                User user3 = User.builder().firstName("Oğuzhan").lastName("Işık").email("oğuzhan@library.com").phoneNumber("5557778899").isActive(true).build();

                userRepository.saveAll(List.of(user1, user2, user3));

                
                if (borrowingRecordRepository.count() == 0) {
                    
                  
                    BorrowingRecord record1 = BorrowingRecord.builder()
                            .user(user1)
                            .book(book1)
                            .borrowDate(LocalDate.now().minusDays(5))
                            .dueDate(LocalDate.now().plusDays(9)) 
                            .build();

                   
                    BorrowingRecord record2 = BorrowingRecord.builder()
                            .user(user2)
                            .book(book3)
                            .borrowDate(LocalDate.now().minusDays(20))
                            .dueDate(LocalDate.now().minusDays(6)) 
                            .build();

                   
                    BorrowingRecord record3 = BorrowingRecord.builder()
                            .user(user3)
                            .book(book5)
                            .borrowDate(LocalDate.now().minusDays(30))
                            .dueDate(LocalDate.now().minusDays(16))
                            .returnDate(LocalDate.now().minusDays(18)) 
                            .build();

                    borrowingRecordRepository.saveAll(List.of(record1, record2, record3));
                }
            }
        };
    }
}