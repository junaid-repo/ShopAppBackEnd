package com.management.shop.scheduler;

import com.management.shop.entity.MessageEntity;
import com.management.shop.repository.NotificationsRepo;
import com.management.shop.service.FCMService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class NotificationScheduler {

    @Autowired
    private NotificationsRepo notiRepo;

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private FCMService fcmService;

    // Stores the scheduled tasks so we can cancel/update them later
    private final Map<Integer, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    // 1. Load existing tasks on startup
    @PostConstruct
    public void loadTasksOnStartup() {
        log.info("Loading saved cron tasks from database on startup...");
        List<MessageEntity> messages = notiRepo.findAllMessagesWithCronEx();

        for (MessageEntity msg : messages) {
            scheduleNewTask(msg);
        }
    }

    // 2. Method to schedule a single task dynamically (Call this when saving a NEW message)
    public void scheduleNewTask(MessageEntity msg) {
        // Prevent duplicate scheduling
        if (scheduledTasks.containsKey(msg.getId())) {
            cancelTask(msg.getId());
        }

        ScheduledFuture<?> future = taskScheduler.schedule(() -> {
            try {
                log.info("Executing scheduled task with cron: {}", msg.getCronEx());
                fcmService.sendNotification(msg.getTitle(), msg.getDetails(), msg.getUserId());

                // Update DB only if FCM succeeds
                notiRepo.updateNotificationSentStatus(msg.getId(), Boolean.TRUE, LocalDateTime.now());
            } catch (Exception e) {
                log.error("Failed to send scheduled notification for message ID: {}", msg.getId(), e);
            }
        }, new CronTrigger(msg.getCronEx()));

        // Save the reference
        scheduledTasks.put(msg.getId(), future);
        log.info("Scheduled task ID {} with cron {}", msg.getId(), msg.getCronEx());
    }

    // 3. Method to cancel a task (Call this when deleting/disabling a message)
    public void cancelTask(Integer messageId) {
        ScheduledFuture<?> future = scheduledTasks.get(messageId);
        if (future != null) {
            future.cancel(false); // false means don't interrupt if it's currently running
            scheduledTasks.remove(messageId);
            log.info("Cancelled scheduled task ID {}", messageId);
        }
    }
}