package com.capstone.confms.repository;

import com.capstone.confms.entity.Bidding;
import com.capstone.confms.utils.enums.BidValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BiddingRepository extends JpaRepository<Bidding, Integer> {

    List<Bidding> findByReviewer_Id(Integer reviewerId);

    List<Bidding> findByPaper_Id(Integer paperId);

    Optional<Bidding> findByReviewer_IdAndPaper_Id(Integer reviewerId, Integer paperId);

    List<Bidding> findByReviewer_IdAndPaper_Track_Conference_Id(Integer reviewerId, Integer conferenceId);

    long countByReviewer_IdAndBidValueAndPaper_Track_Conference_Id(
            Integer reviewerId, BidValue bidValue, Integer conferenceId);

    boolean existsByReviewer_IdAndPaper_Id(Integer reviewerId, Integer paperId);
}