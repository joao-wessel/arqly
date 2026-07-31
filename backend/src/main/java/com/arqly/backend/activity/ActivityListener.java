package com.arqly.backend.activity;

import com.arqly.backend.service.ActivityService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ActivityListener {
    private final ActivityService activityService;

    public ActivityListener(ActivityService activityService) {
        this.activityService = activityService;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onActivityRequested(ActivityRequestedEvent event) {
        activityService.create(event);
    }

    @EventListener
    public void onActivityRequestedWithoutTransaction(ActivityRequestedEvent event) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            activityService.create(event);
        }
    }
}
