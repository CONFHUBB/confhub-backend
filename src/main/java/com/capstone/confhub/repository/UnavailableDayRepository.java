package com.capstone.confhub.repository;

import com.capstone.confhub.entity.UnavailableDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UnavailableDayRepository extends JpaRepository<UnavailableDay, Integer> {

    List<UnavailableDay> findByUser_IdOrderByStartDateDesc(Integer userId);

    /**
     * Check if a user is unavailable on a specific date.
     * A user is unavailable if any of their ranges contain the given date:
     *   startDate <= date AND endDate >= date
     */
    boolean existsByUser_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Integer userId, LocalDate date, LocalDate date2);
}
