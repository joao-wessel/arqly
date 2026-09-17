package com.arqly.backend.notification;
import java.util.*;import org.springframework.stereotype.Component;
@Component public class NotificationChannelRegistry {private final List<NotificationChannel> channels;public NotificationChannelRegistry(List<NotificationChannel> channels){this.channels=channels;}public List<NotificationChannel> channels(){return channels;}}
