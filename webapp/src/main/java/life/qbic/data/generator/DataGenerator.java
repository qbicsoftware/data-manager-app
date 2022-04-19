package life.qbic.data.generator;

import com.vaadin.flow.spring.annotation.SpringComponent;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import life.qbic.data.Role;
import life.qbic.data.entity.TestUser;
import life.qbic.data.service.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringComponent
public class DataGenerator {

    @Bean
    public CommandLineRunner loadData(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        return args -> {
            Logger logger = LoggerFactory.getLogger(getClass());
            if (userRepository.count() != 0L) {
                logger.info("Using existing database");
                return;
            }
            int seed = 123;

            logger.info("Generating demo data");

            logger.info("... generating 2 TestUser entities...");
            TestUser testUser = new TestUser();
            testUser.setName("John Normal");
            testUser.setUsername("testUser");
            testUser.setHashedPassword(passwordEncoder.encode("testUser"));
            testUser.setProfilePictureUrl(
                    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=128&h=128&q=80");
            testUser.setRoles(Collections.singleton(Role.USER));
            userRepository.save(testUser);
            TestUser admin = new TestUser();
            admin.setName("Emma Powerful");
            admin.setUsername("admin");
            admin.setHashedPassword(passwordEncoder.encode("admin"));
            admin.setProfilePictureUrl(
                    "https://images.unsplash.com/photo-1607746882042-944635dfe10e?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=128&h=128&q=80");
            admin.setRoles(Stream.of(Role.USER, Role.ADMIN).collect(Collectors.toSet()));
            userRepository.save(admin);

            logger.info("Generated demo data");
        };
    }

}
