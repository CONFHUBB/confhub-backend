package com.capstone.confms.service.impl;

import com.capstone.confms.dto.TrackReviewSettingDTO;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.entity.TrackReviewSetting;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.repository.TrackReviewSettingRepository;
import com.capstone.confms.service.TrackReviewSettingService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackReviewSettingServiceImpl implements TrackReviewSettingService {

    private final TrackReviewSettingRepository settingRepository;
    private final ConferenceTrackRepository trackRepository;

    @Override
    @Transactional(readOnly = true)
    public TrackReviewSettingDTO getReviewSettingsByTrackId(Integer trackId) {
        ConferenceTrack track = trackRepository.findById(trackId)
                .orElseThrow(() -> new EntityNotFoundException("Track not found with ID: " + trackId));

        TrackReviewSetting setting = track.getTrackReviewSetting();
        if (setting == null) {
            return new TrackReviewSettingDTO(); // Return default settings if none exist yet
        }

        return mapEntityToDto(setting);
    }

    @Override
    @Transactional
    public TrackReviewSettingDTO updateReviewSettings(Integer trackId, TrackReviewSettingDTO dto) {
        ConferenceTrack track = trackRepository.findById(trackId)
                .orElseThrow(() -> new EntityNotFoundException("Track not found with ID: " + trackId));

        TrackReviewSetting setting = track.getTrackReviewSetting();
        if (setting == null) {
            setting = new TrackReviewSetting();
            setting.setTrack(track);
        }

        // Apply DTO fields
        if (dto.getIsDoubleBlind() != null) setting.setIsDoubleBlind(dto.getIsDoubleBlind());
        if (dto.getReviewerInstructions() != null) setting.setReviewerInstructions(dto.getReviewerInstructions());
        if (dto.getRequireSubjectAreas() != null) setting.setRequireSubjectAreas(dto.getRequireSubjectAreas());
        if (dto.getAllowReviewerQuota() != null) setting.setAllowReviewerQuota(dto.getAllowReviewerQuota());
        if (dto.getReviewerInviteExpirationDays() != null) setting.setReviewerInviteExpirationDays(dto.getReviewerInviteExpirationDays());
        if (dto.getAllowOthersReviewAccessAfterSubmit() != null) setting.setAllowOthersReviewAccessAfterSubmit(dto.getAllowOthersReviewAccessAfterSubmit());
        if (dto.getAllowReviewUpdateDuringDiscussion() != null) setting.setAllowReviewUpdateDuringDiscussion(dto.getAllowReviewUpdateDuringDiscussion());
        if (dto.getShowReviewerIdentityToOtherReviewer() != null) setting.setShowReviewerIdentityToOtherReviewer(dto.getShowReviewerIdentityToOtherReviewer());
        if (dto.getShowAggregateColumns() != null) setting.setShowAggregateColumns(dto.getShowAggregateColumns());
        if (dto.getAllowReviewerSeeStatusBeforeNotification() != null) setting.setAllowReviewerSeeStatusBeforeNotification(dto.getAllowReviewerSeeStatusBeforeNotification());
        if (dto.getEnableAllPapersForDiscussion() != null) setting.setEnableAllPapersForDiscussion(dto.getEnableAllPapersForDiscussion());
        if (dto.getAllowDiscussNonAssignedPapers() != null) setting.setAllowDiscussNonAssignedPapers(dto.getAllowDiscussNonAssignedPapers());
        if (dto.getAllowAuthorDiscuss() != null) setting.setAllowAuthorDiscuss(dto.getAllowAuthorDiscuss());
        if (dto.getNotifyReviewerOnReviewUpdateDuringDiscussion() != null) setting.setNotifyReviewerOnReviewUpdateDuringDiscussion(dto.getNotifyReviewerOnReviewUpdateDuringDiscussion());
        if (dto.getNotifyOnManualAssignment() != null) setting.setNotifyOnManualAssignment(dto.getNotifyOnManualAssignment());
        if (dto.getDoNotShowWithdrawnPapers() != null) setting.setDoNotShowWithdrawnPapers(dto.getDoNotShowWithdrawnPapers());
        if (dto.getAddReviewerOnInviteAccept() != null) setting.setAddReviewerOnInviteAccept(dto.getAddReviewerOnInviteAccept());

        TrackReviewSetting savedSetting = settingRepository.save(setting);
        track.setTrackReviewSetting(savedSetting);
        trackRepository.save(track);

        return mapEntityToDto(savedSetting);
    }

    @Override
    @Transactional
    public void copyReviewSettings(Integer sourceTrackId, Integer targetTrackId) {
        if (sourceTrackId.equals(targetTrackId)) {
            throw new IllegalArgumentException("Source and target track IDs cannot be the same");
        }

        ConferenceTrack sourceTrack = trackRepository.findById(sourceTrackId)
                .orElseThrow(() -> new EntityNotFoundException("Source Track not found with ID: " + sourceTrackId));

        ConferenceTrack targetTrack = trackRepository.findById(targetTrackId)
                .orElseThrow(() -> new EntityNotFoundException("Target Track not found with ID: " + targetTrackId));

        if (!sourceTrack.getConference().getId().equals(targetTrack.getConference().getId())) {
            throw new IllegalArgumentException("Cannot copy settings between different conferences");
        }

        TrackReviewSetting sourceSetting = sourceTrack.getTrackReviewSetting();

        TrackReviewSetting targetSetting = targetTrack.getTrackReviewSetting();
        if (targetSetting == null) {
            targetSetting = new TrackReviewSetting();
            targetSetting.setTrack(targetTrack);
        }

        if (sourceSetting == null) {
            // Source has no settings — reset target to defaults
            targetSetting.setIsDoubleBlind(false);
            targetSetting.setReviewerInstructions(null);
            targetSetting.setRequireSubjectAreas(false);
            targetSetting.setAllowReviewerQuota(false);
            targetSetting.setReviewerInviteExpirationDays(null);
            targetSetting.setAllowOthersReviewAccessAfterSubmit(false);
            targetSetting.setAllowReviewUpdateDuringDiscussion(false);
            targetSetting.setShowReviewerIdentityToOtherReviewer(false);
            targetSetting.setShowAggregateColumns(false);
            targetSetting.setAllowReviewerSeeStatusBeforeNotification(false);
            targetSetting.setEnableAllPapersForDiscussion(false);
            targetSetting.setAllowDiscussNonAssignedPapers(false);
            targetSetting.setAllowAuthorDiscuss(false);
            targetSetting.setNotifyReviewerOnReviewUpdateDuringDiscussion(false);
            targetSetting.setNotifyOnManualAssignment(false);
            targetSetting.setDoNotShowWithdrawnPapers(false);
            targetSetting.setAddReviewerOnInviteAccept(true);
        } else {
            // Copy fields from source
            targetSetting.setIsDoubleBlind(sourceSetting.getIsDoubleBlind());
            targetSetting.setReviewerInstructions(sourceSetting.getReviewerInstructions());
            targetSetting.setRequireSubjectAreas(sourceSetting.getRequireSubjectAreas());
            targetSetting.setAllowReviewerQuota(sourceSetting.getAllowReviewerQuota());
            targetSetting.setReviewerInviteExpirationDays(sourceSetting.getReviewerInviteExpirationDays());
            targetSetting.setAllowOthersReviewAccessAfterSubmit(sourceSetting.getAllowOthersReviewAccessAfterSubmit());
            targetSetting.setAllowReviewUpdateDuringDiscussion(sourceSetting.getAllowReviewUpdateDuringDiscussion());
            targetSetting.setShowReviewerIdentityToOtherReviewer(sourceSetting.getShowReviewerIdentityToOtherReviewer());
            targetSetting.setShowAggregateColumns(sourceSetting.getShowAggregateColumns());
            targetSetting.setAllowReviewerSeeStatusBeforeNotification(sourceSetting.getAllowReviewerSeeStatusBeforeNotification());
            targetSetting.setEnableAllPapersForDiscussion(sourceSetting.getEnableAllPapersForDiscussion());
            targetSetting.setAllowDiscussNonAssignedPapers(sourceSetting.getAllowDiscussNonAssignedPapers());
            targetSetting.setAllowAuthorDiscuss(sourceSetting.getAllowAuthorDiscuss());
            targetSetting.setNotifyReviewerOnReviewUpdateDuringDiscussion(sourceSetting.getNotifyReviewerOnReviewUpdateDuringDiscussion());
            targetSetting.setNotifyOnManualAssignment(sourceSetting.getNotifyOnManualAssignment());
            targetSetting.setDoNotShowWithdrawnPapers(sourceSetting.getDoNotShowWithdrawnPapers());
            targetSetting.setAddReviewerOnInviteAccept(sourceSetting.getAddReviewerOnInviteAccept());
        }

        settingRepository.save(targetSetting);
        targetTrack.setTrackReviewSetting(targetSetting);
        trackRepository.save(targetTrack);
    }

    private TrackReviewSettingDTO mapEntityToDto(TrackReviewSetting entity) {
        TrackReviewSettingDTO dto = new TrackReviewSettingDTO();
        dto.setIsDoubleBlind(entity.getIsDoubleBlind());
        dto.setReviewerInstructions(entity.getReviewerInstructions());
        dto.setRequireSubjectAreas(entity.getRequireSubjectAreas());
        dto.setAllowReviewerQuota(entity.getAllowReviewerQuota());
        dto.setReviewerInviteExpirationDays(entity.getReviewerInviteExpirationDays());
        dto.setAllowOthersReviewAccessAfterSubmit(entity.getAllowOthersReviewAccessAfterSubmit());
        dto.setAllowReviewUpdateDuringDiscussion(entity.getAllowReviewUpdateDuringDiscussion());
        dto.setShowReviewerIdentityToOtherReviewer(entity.getShowReviewerIdentityToOtherReviewer());
        dto.setShowAggregateColumns(entity.getShowAggregateColumns());
        dto.setAllowReviewerSeeStatusBeforeNotification(entity.getAllowReviewerSeeStatusBeforeNotification());
        dto.setEnableAllPapersForDiscussion(entity.getEnableAllPapersForDiscussion());
        dto.setAllowDiscussNonAssignedPapers(entity.getAllowDiscussNonAssignedPapers());
        dto.setAllowAuthorDiscuss(entity.getAllowAuthorDiscuss());
        dto.setNotifyReviewerOnReviewUpdateDuringDiscussion(entity.getNotifyReviewerOnReviewUpdateDuringDiscussion());
        dto.setNotifyOnManualAssignment(entity.getNotifyOnManualAssignment());
        dto.setDoNotShowWithdrawnPapers(entity.getDoNotShowWithdrawnPapers());
        dto.setAddReviewerOnInviteAccept(entity.getAddReviewerOnInviteAccept());
        return dto;
    }
}
