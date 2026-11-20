package com.campushelp.config;

import com.campushelp.entity.Category;
import com.campushelp.entity.Complaint;
import com.campushelp.entity.ComplaintStatus;
import com.campushelp.entity.Role;
import com.campushelp.entity.StatusHistory;
import com.campushelp.entity.User;
import com.campushelp.repository.CategoryRepository;
import com.campushelp.repository.ComplaintRepository;
import com.campushelp.repository.StatusHistoryRepository;
import com.campushelp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * Seeds the database on first start (only when empty):
 * admin + student Himanshu Yadav, the 7 categories, and the worked example CH-1001.
 * Demo logins (password: campus123 for both):
 *   admin@campushelp.edu  (ADMIN)
 *   himanshu@campushelp.edu  (STUDENT, Himanshu Yadav)
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(UserRepository users,
                           CategoryRepository categories,
                           ComplaintRepository complaints,
                           StatusHistoryRepository history,
                           PasswordEncoder passwordEncoder) {
        return args -> {
            if (users.count() > 0) {
                return; // already seeded
            }
            User admin = users.save(new User("Campus Administrator", "admin@campushelp.edu",
                    passwordEncoder.encode("campus123"), Role.ADMIN));
            User himanshu = users.save(new User("Himanshu Yadav", "himanshu@campushelp.edu",
                    passwordEncoder.encode("campus123"), Role.STUDENT));

            List<String> names = List.of("Classroom", "Laboratory", "Internet",
                    "Electricity", "Cleanliness", "Water", "Other");
            for (String name : names) {
                categories.save(new Category(name));
            }
            Category lab = categories.findByName("Laboratory").orElseThrow();

            Complaint ch1001 = new Complaint();
            ch1001.setCode("CH-1001");
            ch1001.setStudent(himanshu);
            ch1001.setCategory(lab);
            ch1001.setSubject("Computers not working");
            ch1001.setDescription("Several computers in the computer laboratory are not starting. "
                    + "This is affecting practical sessions.");
            ch1001.setStatus(ComplaintStatus.PENDING);
            complaints.save(ch1001);
            history.save(new StatusHistory(ch1001, null, null, ComplaintStatus.PENDING));
        };
    }
}
