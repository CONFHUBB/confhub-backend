package com.capstone.confhub.repository;

import com.capstone.confhub.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TicketTypeRepository extends JpaRepository<TicketType, Integer> {
    List<TicketType> findByConferenceIdAndIsActiveTrue(Integer conferenceId);
    List<TicketType> findByConferenceId(Integer conferenceId);
}
