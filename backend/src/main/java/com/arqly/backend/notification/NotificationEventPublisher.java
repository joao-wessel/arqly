package com.arqly.backend.notification;
import org.springframework.context.ApplicationEventPublisher;import org.springframework.stereotype.Component;
@Component public class NotificationEventPublisher {private final ApplicationEventPublisher publisher;public NotificationEventPublisher(ApplicationEventPublisher publisher){this.publisher=publisher;}public void publish(NotificationRequest request){publisher.publishEvent(new NotificationRequestedEvent(request));}}
