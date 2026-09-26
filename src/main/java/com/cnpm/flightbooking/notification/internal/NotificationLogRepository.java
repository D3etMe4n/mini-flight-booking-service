package com.cnpm.flightbooking.notification.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
}
