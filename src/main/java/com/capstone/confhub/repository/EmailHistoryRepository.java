package com.capstone.confhub.repository;

import com.capstone.confhub.entity.EmailHistory;
import com.capstone.confhub.utils.enums.EmailType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailHistoryRepository extends JpaRepository<EmailHistory, Integer> {

    Page<EmailHistory> findByConference_Id(Integer conferenceId, Pageable pageable);

    Page<EmailHistory> findByEmailType(EmailType emailType, Pageable pageable);

    Page<EmailHistory> findByConference_IdAndEmailType(Integer conferenceId, EmailType emailType, Pageable pageable);
}
