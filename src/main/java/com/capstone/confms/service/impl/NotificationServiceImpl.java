package com.capstone.confms.service.impl;

import com.capstone.confms.dto.NotificationDTO;
import com.capstone.confms.dto.response.NotificationResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.Notification;
import com.capstone.confms.entity.User;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.repository.NotificationRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ConferenceRepository conferenceRepository;

    @Override
    @Transactional
    public NotificationResponseDTO createNotification(NotificationDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + dto.getUserId()));
        Conference conference = conferenceRepository.findById(dto.getConferenceId())
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + dto.getConferenceId()));

        Notification notification = Notification.builder()
                .user(user)
                .conference(conference)
                .title(dto.getTitle())
                .message(dto.getMessage())
                .type(dto.getType())
                .link(dto.getLink())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        return mapToResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponseDTO> getNotificationsByUser(Integer userId, int page, int size) {
        Page<Notification> notifPage = notificationRepository
                .findByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));

        List<NotificationResponseDTO> content = notifPage.getContent().stream()
                .map(this::mapToResponseDTO)
                .toList();

        return PagedResponse.<NotificationResponseDTO>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements((int) notifPage.getTotalElements())
                .totalPages(notifPage.getTotalPages())
                .last(notifPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Integer userId) {
        return notificationRepository.countByUser_IdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public NotificationResponseDTO markAsRead(Integer id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id " + id));
        notification.setIsRead(true);
        return mapToResponseDTO(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead(Integer userId) {
        List<Notification> unread = notificationRepository.findByUser_IdAndIsReadFalse(userId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    @Override
    @Transactional
    public void deleteNotification(Integer id) {
        if (!notificationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Notification not found with id " + id);
        }
        notificationRepository.deleteById(id);
    }

    private NotificationResponseDTO mapToResponseDTO(Notification entity) {
        return NotificationResponseDTO.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .conferenceId(entity.getConference().getId())
                .conferenceName(entity.getConference().getName())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .type(entity.getType())
                .link(entity.getLink())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
