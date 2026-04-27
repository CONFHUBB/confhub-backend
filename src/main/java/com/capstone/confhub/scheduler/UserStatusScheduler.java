package com.capstone.confhub.scheduler;

import com.capstone.confhub.entity.User;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.utils.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserStatusScheduler {

    private final UserRepository userRepository;

    @Scheduled(fixedRate = 60000) // every minute
    @Transactional
    public void resetExpiredStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<User> expiredUsers = userRepository.findByStatusNotAndStatusUntilLessThanEqual(UserStatus.AVAILABLE, now);

        if (expiredUsers.isEmpty()) {
            return;
        }

        for (User user : expiredUsers) {
            user.setStatus(UserStatus.AVAILABLE);
            user.setStatusUntil(null);
        }

        userRepository.saveAll(expiredUsers);
        log.info("Reset {} expired user statuses to AVAILABLE", expiredUsers.size());
    }
}
