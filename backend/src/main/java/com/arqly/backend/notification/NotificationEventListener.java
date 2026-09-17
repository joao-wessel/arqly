package com.arqly.backend.notification;
import org.springframework.context.event.EventListener;import org.springframework.stereotype.Component;
@Component public class NotificationEventListener {private final NotificationDispatcher dispatcher;public NotificationEventListener(NotificationDispatcher dispatcher){this.dispatcher=dispatcher;}@EventListener public void handle(NotificationRequestedEvent event){dispatcher.dispatch(event.request());}}
