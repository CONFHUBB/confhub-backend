package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.TrackReviewSettingDTO;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.TrackReviewSetting;
import com.capstone.confhub.repository.ConferenceTrackRepository;
import com.capstone.confhub.repository.TrackReviewSettingRepository;
import com.capstone.confhub.service.TrackReviewSettingService;
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
        if (dto.getAllowReviewerQuota() != null) setting.setAllowReviewerQuota(dto.getAllowReviewerQuota());
        setting.setReviewerInviteExpirationDays(7); // Fixed
        if (dto.getAllowOthersReviewAccessAfterSubmit() != null) setting.setAllowOthersReviewAccessAfterSubmit(dto.getAllowOthersReviewAccessAfterSubmit());
        if (dto.getAllowReviewUpdateDuringDiscussion() != null) setting.setAllowReviewUpdateDuringDiscussion(dto.getAllowReviewUpdateDuringDiscussion());
        if (dto.getShowReviewerIdentityToOtherReviewer() != null) setting.setShowReviewerIdentityToOtherReviewer(dto.getShowReviewerIdentityToOtherReviewer());
        // Hardcoded settings — not configurable from UI
        setting.setShowAggregateColumns(true);
        setting.setAllowReviewerSeeStatusBeforeNotification(true);
        setting.setEnableAllPapersForDiscussion(true);
        if (dto.getAllowDiscussNonAssignedPapers() != null) setting.setAllowDiscussNonAssignedPapers(dto.getAllowDiscussNonAssignedPapers());
        setting.setAllowAuthorDiscuss(false);
        setting.setDoNotShowWithdrawnPapers(false);
        if (dto.getEnableDomainConflict() != null) setting.setEnableDomainConflict(dto.getEnableDomainConflict());
        if (dto.getEnableAuthorSelfConflict() != null) setting.setEnableAuthorSelfConflict(dto.getEnableAuthorSelfConflict());
        if (dto.getAllowAuthorConfigureConflict() != null) setting.setAllowAuthorConfigureConflict(dto.getAllowAuthorConfigureConflict());

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
            targetSetting.setAllowReviewerQuota(false);
            targetSetting.setReviewerInviteExpirationDays(7);
            targetSetting.setAllowOthersReviewAccessAfterSubmit(false);
            targetSetting.setAllowReviewUpdateDuringDiscussion(false);
            targetSetting.setShowReviewerIdentityToOtherReviewer(false);
            targetSetting.setShowAggregateColumns(true);
            targetSetting.setAllowReviewerSeeStatusBeforeNotification(true);
            targetSetting.setEnableAllPapersForDiscussion(true);
            targetSetting.setAllowDiscussNonAssignedPapers(false);
            targetSetting.setAllowAuthorDiscuss(false);
            targetSetting.setDoNotShowWithdrawnPapers(false);
            targetSetting.setEnableDomainConflict(true);
            targetSetting.setEnableAuthorSelfConflict(true);
            targetSetting.setAllowAuthorConfigureConflict(false);
        } else {
            // Copy fields from source
            targetSetting.setIsDoubleBlind(sourceSetting.getIsDoubleBlind());
            targetSetting.setReviewerInstructions(sourceSetting.getReviewerInstructions());
            targetSetting.setAllowReviewerQuota(sourceSetting.getAllowReviewerQuota());
            targetSetting.setReviewerInviteExpirationDays(sourceSetting.getReviewerInviteExpirationDays());
            targetSetting.setAllowOthersReviewAccessAfterSubmit(sourceSetting.getAllowOthersReviewAccessAfterSubmit());
            targetSetting.setAllowReviewUpdateDuringDiscussion(sourceSetting.getAllowReviewUpdateDuringDiscussion());
            targetSetting.setShowReviewerIdentityToOtherReviewer(sourceSetting.getShowReviewerIdentityToOtherReviewer());
            targetSetting.setShowAggregateColumns(true);  // Always enabled
            targetSetting.setAllowReviewerSeeStatusBeforeNotification(true);  // Always enabled
            targetSetting.setEnableAllPapersForDiscussion(true);  // Always enabled
            targetSetting.setAllowDiscussNonAssignedPapers(sourceSetting.getAllowDiscussNonAssignedPapers());
            targetSetting.setAllowAuthorDiscuss(false);  // Never allowed
            targetSetting.setDoNotShowWithdrawnPapers(false);  // Always show
            targetSetting.setEnableDomainConflict(sourceSetting.getEnableDomainConflict());
            targetSetting.setEnableAuthorSelfConflict(sourceSetting.getEnableAuthorSelfConflict());
            targetSetting.setAllowAuthorConfigureConflict(sourceSetting.getAllowAuthorConfigureConflict());
        }

        settingRepository.save(targetSetting);
        targetTrack.setTrackReviewSetting(targetSetting);
        trackRepository.save(targetTrack);
    }

    private TrackReviewSettingDTO mapEntityToDto(TrackReviewSetting entity) {
        TrackReviewSettingDTO dto = new TrackReviewSettingDTO();
        dto.setIsDoubleBlind(entity.getIsDoubleBlind());
        dto.setReviewerInstructions(entity.getReviewerInstructions());
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
        dto.setDoNotShowWithdrawnPapers(entity.getDoNotShowWithdrawnPapers());
        dto.setEnableDomainConflict(entity.getEnableDomainConflict());
        dto.setEnableAuthorSelfConflict(entity.getEnableAuthorSelfConflict());
        dto.setAllowAuthorConfigureConflict(entity.getAllowAuthorConfigureConflict());
        return dto;
    }
}
