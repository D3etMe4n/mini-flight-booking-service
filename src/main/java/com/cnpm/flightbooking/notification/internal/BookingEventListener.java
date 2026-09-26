package com.cnpm.flightbooking.notification.internal;

import com.cnpm.flightbooking.notification.event.BookingSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventListener {

    private final NotificationLogRepository notificationLogRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBookingSuccess(BookingSuccessEvent event) {
        log.info("Received BookingSuccessEvent for booking ID: {}", event.bookingId());

        String title = "Booking Confirmation - Flight " + event.flightNumber();
        String content = String.format(
                "Dear Customer, your booking #%d for flight %s (Seat %s) has been successfully confirmed. Total paid: %s VND.",
                event.bookingId(),
                event.flightNumber(),
                event.seatNumber(),
                event.totalAmount()
        );

        try {
            // 1. Mock email dispatch via SMTP/third-party mail service
            sendEmailMock(event);

            // 2. Persist audit record with SENT status
            saveNotificationLog(
                    event.bookingId(),
                    event.customerEmail(),
                    NotificationChannel.EMAIL,
                    title,
                    content,
                    NotificationStatus.SENT,
                    null
            );
            log.info("Email sent successfully to {}", event.customerEmail());

        } catch (Exception ex) {
            // 3. Defensive programming: Catch exceptions to prevent breaking the main transaction flow, persist as FAILED
            log.error("Failed to send booking notification email to {}: {}", event.customerEmail(), ex.getMessage());
            saveNotificationLog(
                    event.bookingId(),
                    event.customerEmail(),
                    NotificationChannel.EMAIL,
                    title,
                    content,
                    NotificationStatus.FAILED,
                    ex.getMessage()
            );
        }
    }

    private void sendEmailMock(BookingSuccessEvent event) {
        // Validate recipient address before attempting dispatch
        if (event.customerEmail() == null || event.customerEmail().isBlank()) {
            throw new IllegalArgumentException("Customer email address is empty");
        }
        // Simulated network I/O latency for external mail server
    }

    private void saveNotificationLog(Long bookingId,
                                     String recipient,
                                     NotificationChannel channel,
                                     String title,
                                     String content,
                                     NotificationStatus status,
                                     String errorMsg) {
        NotificationLog notificationLog = NotificationLog.builder()
                .bookingId(bookingId)
                .recipient(recipient)
                .channel(channel)
                .title(title)
                .content(content)
                .status(status)
                .errorMessage(errorMsg)
                .build();

        notificationLogRepository.save(notificationLog);
    }
}