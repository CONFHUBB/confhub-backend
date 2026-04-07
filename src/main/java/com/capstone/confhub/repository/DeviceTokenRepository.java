package com.capstone.confhub.repository;

import com.capstone.confhub.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Integer> {

    List<DeviceToken> findByUser_Id(Integer userId);

    Optional<DeviceToken> findByUser_IdAndFcmToken(Integer userId, String fcmToken);

    void deleteByFcmToken(String fcmToken);
}
