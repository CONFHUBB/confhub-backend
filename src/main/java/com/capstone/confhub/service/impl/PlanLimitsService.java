package com.capstone.confhub.service.impl;

import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.repository.TicketRepository;
import com.capstone.confhub.utils.enums.SubscriptionPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Enforces feature limits based on the conference's subscription plan.
 *
 * Plan limits:
 *   STARTER:       20 papers, 10 reviewers, 1 track,  50 attendees, no AI, no export
 *   PROFESSIONAL: 100 papers, 50 reviewers, 5 tracks, 300 attendees, AI + export
 *   ENTERPRISE:   unlimited everything
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PlanLimitsService {

    private final ConferenceRepository conferenceRepository;
    private final PaperRepository paperRepository;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;
    private final TicketRepository ticketRepository;

    // ── Limit constants ──────────────────────────────────────────────────────

    private int maxPapers(SubscriptionPlan plan) {
        return switch (plan) {
            case STARTER -> 20;
            case PROFESSIONAL -> 100;
            case ENTERPRISE -> Integer.MAX_VALUE;
        };
    }

    private int maxReviewers(SubscriptionPlan plan) {
        return switch (plan) {
            case STARTER -> 10;
            case PROFESSIONAL -> 50;
            case ENTERPRISE -> Integer.MAX_VALUE;
        };
    }

    private int maxTracks(SubscriptionPlan plan) {
        return switch (plan) {
            case STARTER -> 1;
            case PROFESSIONAL -> 5;
            case ENTERPRISE -> Integer.MAX_VALUE;
        };
    }

    private int maxAttendees(SubscriptionPlan plan) {
        return switch (plan) {
            case STARTER -> 50;
            case PROFESSIONAL -> 300;
            case ENTERPRISE -> Integer.MAX_VALUE;
        };
    }

    private boolean canUseAI(SubscriptionPlan plan) {
        return plan == SubscriptionPlan.PROFESSIONAL || plan == SubscriptionPlan.ENTERPRISE;
    }

    private boolean canExport(SubscriptionPlan plan) {
        return plan == SubscriptionPlan.PROFESSIONAL || plan == SubscriptionPlan.ENTERPRISE;
    }

    // ── Public check methods ─────────────────────────────────────────────────

    private SubscriptionPlan getPlan(Integer conferenceId) {
        Conference conference = conferenceRepository.findById(conferenceId).orElse(null);
        if (conference == null || conference.getSubscriptionPlan() == null) {
            // No plan selected yet — treat as STARTER for safety
            return SubscriptionPlan.STARTER;
        }
        return conference.getSubscriptionPlan();
    }

    public void checkPaperLimit(Integer conferenceId) {
        SubscriptionPlan plan = getPlan(conferenceId);
        long currentCount = paperRepository.findByTrack_Conference_Id(conferenceId).size();
        if (currentCount >= maxPapers(plan)) {
            throw new BadRequestException(
                    String.format("Paper limit reached for %s plan (%d/%d). Please upgrade your plan.",
                            plan.name(), currentCount, maxPapers(plan)));
        }
    }

    public void checkTrackLimit(Integer conferenceId, int currentTrackCount) {
        SubscriptionPlan plan = getPlan(conferenceId);
        if (currentTrackCount >= maxTracks(plan)) {
            throw new BadRequestException(
                    String.format("Track limit reached for %s plan (%d/%d). Please upgrade your plan.",
                            plan.name(), currentTrackCount, maxTracks(plan)));
        }
    }

    public void checkAttendeeLimit(Integer conferenceId) {
        SubscriptionPlan plan = getPlan(conferenceId);
        long currentCount = ticketRepository.countByConference_Id(conferenceId);
        if (currentCount >= maxAttendees(plan)) {
            throw new BadRequestException(
                    String.format("Attendee limit reached for %s plan (%d/%d). Please upgrade your plan.",
                            plan.name(), currentCount, maxAttendees(plan)));
        }
    }

    public void checkCanUsePlagiarism(Integer conferenceId) {
        SubscriptionPlan plan = getPlan(conferenceId);
        if (!canUseAI(plan)) {
            throw new BadRequestException(
                    "AI Plagiarism check is not available on the " + plan.name() + " plan. Please upgrade to Professional or Enterprise.");
        }
    }

    public void checkCanUseAISuggestion(Integer conferenceId) {
        SubscriptionPlan plan = getPlan(conferenceId);
        if (!canUseAI(plan)) {
            throw new BadRequestException(
                    "AI Reviewer Suggestion is not available on the " + plan.name() + " plan. Please upgrade to Professional or Enterprise.");
        }
    }

    public void checkCanExport(Integer conferenceId) {
        SubscriptionPlan plan = getPlan(conferenceId);
        if (!canExport(plan)) {
            throw new BadRequestException(
                    "Export feature is not available on the " + plan.name() + " plan. Please upgrade to Professional or Enterprise.");
        }
    }

    /**
     * Returns the plan details for displaying on the frontend.
     */
    public SubscriptionPlan getConferencePlan(Integer conferenceId) {
        return getPlan(conferenceId);
    }
}
