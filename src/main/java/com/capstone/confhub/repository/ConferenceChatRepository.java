package com.capstone.confhub.repository;

import com.capstone.confhub.entity.ConferenceChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConferenceChatRepository extends JpaRepository<ConferenceChat, Long> {

    /** Group messages only (recipientId IS NULL) for a specific conference */
    List<ConferenceChat> findByConference_IdAndRecipientIdIsNullOrderByCreatedAtAsc(Integer conferenceId);

    /** DM between two users — globally (no conference filter) */
    @Query("SELECT c FROM ConferenceChat c WHERE c.recipientId IS NOT NULL " +
           "AND ((c.user.id = :userA AND c.recipientId = :userB) OR (c.user.id = :userB AND c.recipientId = :userA)) " +
           "ORDER BY c.createdAt ASC")
    List<ConferenceChat> findDmMessagesGlobal(
            @Param("userA") Integer userA,
            @Param("userB") Integer userB);

    /** All DMs involving a user — globally (no conference filter) */
    @Query("SELECT c FROM ConferenceChat c WHERE c.recipientId IS NOT NULL " +
           "AND (c.user.id = :userId OR c.recipientId = :userId) " +
           "ORDER BY c.createdAt DESC")
    List<ConferenceChat> findAllDmsForUserGlobal(
            @Param("userId") Integer userId);
}
