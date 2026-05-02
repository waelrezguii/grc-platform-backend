package com.project.grcplatform.scheduler;

import com.project.grcplatform.constant.AssessmentStatus;
import com.project.grcplatform.constant.NotificationEntityType;
import com.project.grcplatform.constant.NotificationType;
import com.project.grcplatform.model.Control;
import com.project.grcplatform.model.RiskAssessment;
import com.project.grcplatform.model.TreatmentAction;
import com.project.grcplatform.repository.ControlRepository;
import com.project.grcplatform.repository.RiskAssessmentRepository;
import com.project.grcplatform.repository.TreatmentActionRepository;
import com.project.grcplatform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationService notificationService;
    private final ControlRepository controlRepository;
    private final TreatmentActionRepository actionRepository;
    private final RiskAssessmentRepository assessmentRepository;

    /**
     * Every day at 08:00 — check for overdue control reviews.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkOverdueControlReviews() {
        log.info("[Scheduler] Checking overdue control reviews...");
        try {
            List<Control> overdueControls = controlRepository.findOverdueForReview(LocalDate.now());
            for (Control control : overdueControls) {
                if (control.getCreatedBy() == null) continue;
                notificationService.notify(
                        control.getCreatedBy().getId(),
                        NotificationType.CONTROL_REVIEW_OVERDUE,
                        NotificationEntityType.CONTROL,
                        control.getId(),
                        "Control Review Overdue",
                        "Control \"" + control.getTitle() + "\" [" + control.getIsoReference() + "] was due for review on "
                                + control.getNextReviewDate() + ". Please schedule a review immediately."
                );
            }
            log.info("[Scheduler] Sent {} overdue control review notifications.", overdueControls.size());
        } catch (Exception e) {
            log.error("[Scheduler] Error checking overdue control reviews: {}", e.getMessage());
        }
    }

    /**
     * Every day at 08:05 — check for overdue treatment actions.
     */
    @Scheduled(cron = "0 5 8 * * *")
    public void checkOverdueTreatmentActions() {
        log.info("[Scheduler] Checking overdue treatment actions...");
        try {
            List<TreatmentAction> overdueActions = actionRepository.findOverdueActions(LocalDate.now());
            for (TreatmentAction action : overdueActions) {
                if (action.getAssignee() == null) continue;
                notificationService.notify(
                        action.getAssignee().getId(),
                        NotificationType.ACTION_OVERDUE,
                        NotificationEntityType.ACTION,
                        action.getId(),
                        "Treatment Action Overdue",
                        "Action \"" + action.getTitle() + "\" was due on " + action.getDueDate()
                                + " and is still " + action.getStatus().name() + ". Please update its status."
                );
            }
            log.info("[Scheduler] Sent {} overdue action notifications.", overdueActions.size());
        } catch (Exception e) {
            log.error("[Scheduler] Error checking overdue treatment actions: {}", e.getMessage());
        }
    }

    /**
     * Every day at 08:10 — check for assessments due within 7 days.
     */
    @Scheduled(cron = "0 10 8 * * *")
    public void checkAssessmentsDueSoon() {
        log.info("[Scheduler] Checking assessments due soon...");
        try {
            LocalDate threshold = LocalDate.now().plusDays(7);
            List<RiskAssessment> dueSoon = assessmentRepository.findDueSoon(threshold,
                    List.of(AssessmentStatus.PLANNED, AssessmentStatus.IN_PROGRESS, AssessmentStatus.UNDER_REVIEW));
            for (RiskAssessment assessment : dueSoon) {
                if (assessment.getRiskOwner() == null) continue;
                notificationService.notify(
                        assessment.getRiskOwner().getId(),
                        NotificationType.ASSESSMENT_DUE_SOON,
                        NotificationEntityType.ASSESSMENT,
                        assessment.getId(),
                        "Assessment Due Soon",
                        "Risk Assessment \"" + assessment.getTitle() + "\" is due on "
                                + assessment.getEndDate() + " and is currently " + assessment.getStatus().name() + "."
                );
            }
            log.info("[Scheduler] Sent {} assessment due-soon notifications.", dueSoon.size());
        } catch (Exception e) {
            log.error("[Scheduler] Error checking assessments due soon: {}", e.getMessage());
        }
    }
}