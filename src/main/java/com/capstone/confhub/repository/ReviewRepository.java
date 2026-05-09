package com.capstone.confhub.repository;

import com.capstone.confhub.dto.response.TopReviewerResponseDTO;
import com.capstone.confhub.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByPaper_Id(Integer paperId);

    List<Review> findByReviewer_Id(Integer reviewerId);

    List<Review> findByPaper_Track_Conference_Id(Integer conferenceId);

    boolean existsByPaper_IdAndReviewer_Id(Integer paperId, Integer reviewerId);

    long countByPaper_Id(Integer paperId);

    long countByReviewer_IdAndPaper_Track_Conference_Id(Integer reviewerId, Integer conferenceId);

    List<Review> findByReviewer_IdAndPaper_Track_Conference_Id(Integer reviewerId, Integer conferenceId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.paper.track.conference.id = :conferenceId")
    long countByConferenceId(Integer conferenceId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.paper.track.conference.id = :conferenceId AND r.status = 'COMPLETED'")
    long countCompletedByConferenceId(Integer conferenceId);

    @Query("""
            SELECT new com.capstone.confhub.dto.response.TopReviewerResponseDTO(
                r.reviewer.id,
                CONCAT(r.reviewer.firstName, ' ', r.reviewer.lastName),
                r.reviewer.email,
                COUNT(r),
                p.jobTitle,
                p.institution,
                p.department,
                p.biography,
                p.googleScholarLink,
                p.institutionCountry,
                p.websiteUrl,
                p.orcid,
                p.avatarUrl
            )
            FROM Review r
            LEFT JOIN UserProfile p ON p.user = r.reviewer
            WHERE r.status = com.capstone.confhub.utils.enums.ReviewStatus.COMPLETED
            GROUP BY r.reviewer.id, r.reviewer.firstName, r.reviewer.lastName,
                     r.reviewer.email,
                     p.jobTitle, p.institution, p.department, p.biography, p.googleScholarLink,
                     p.institutionCountry, p.websiteUrl, p.orcid, p.avatarUrl
            ORDER BY COUNT(r) DESC
            """)
    List<TopReviewerResponseDTO> findTopReviewers(Pageable pageable);
}
