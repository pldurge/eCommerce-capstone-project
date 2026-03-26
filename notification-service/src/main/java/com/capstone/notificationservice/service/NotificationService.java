package com.capstone.notificationservice.service;

import com.capstone.notificationservice.client.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final UserServiceClient userServiceClient;

    @Value("${app.notification.from-email}")
    private String fromEmail;

    @Value("${app.notification.from-name}")
    private String fromName;

    // ─── Kafka Listeners ─────────────────────────────────────────────────────
    @KafkaListener(topics = "user_registered", groupId = "notification-service-group")
    public void handleUserRegistered(Map<String, Object> map) {
        try{
            //Email is directly available in the registration event
            String email = (String) map.get("email");
            String name = (String) map.get("name");
            sendWelcomeEmail(email, name);
        }catch(Exception e){
            log.error("Error sending welcome email", e);
        }
    }

    @KafkaListener(topics = "user_logged", groupId = "notification-service-group")
    public void handleUserLogged(Map<String, Object> map) {
        try{
            String email = (String) map.get("email");
            String name = (String) map.get("name");
            sendLoggedInEmail(email, name);
        }catch(Exception e){
            log.error("Error sending login email", e);
        }
    }

    @KafkaListener(topics = "password_reset", groupId = "notification-service-group")
    public void handlePasswordResetRequested(Map<String, Object> map) {
        try{
            String email = (String) map.get("email");
            String name = (String) map.get("name");
            String resetToken = (String) map.get("resetToken");
            sendPasswordResetEmail(email, name, resetToken);
        } catch (Exception e) {
            log.error("Error sending password reset email", e);
        }
    }

    @KafkaListener(topics = "order-status-updated", groupId = "notification-service-group")
    public void handleOrderStatusUpdated(Map<String, Object> map) {
        try{
            String userId = (String) map.get("userId");
            String orderNumber = (String) map.get("orderNumber");
            String newStatus = (String) map.get("newStatus");

            String email = resolveEmail(userId);
            if(email != null){
                sendOrderStatusEmail(email, orderNumber, newStatus);
            }
        }catch(Exception e){
            log.error("Error sending order status email", e);
        }
    }

    @KafkaListener(topics = "payment-notification", groupId = "notification-service-group")
    public void handlePaymentNotification(Map<String, Object> map) {
        try{
            String userId = (String) map.get("userId");
            String orderId = (String) map.get("orderId");
            String type = (String) map.get("type");
            String amount =  (String) map.get("amount");

            String email = resolveEmail(userId);
            if(email != null){
                sendPaymentEmail(email, type, orderId, amount);
            }
        } catch (Exception e) {
            log.error("Error sending payment notification", e);
        }
    }

    // ─── Email Senders ────────────────────────────────────────────────────────

    private void sendWelcomeEmail(String toEmail, String name) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        String formattedDateTime = now.format(formatter);
        String subject = "Welcome to E-Commerce Platform";
        String body = String.format("""
                Dear %s,
                Welcome to our platform! Your account has been created successfully.
                You can now browse products, add them to your cart, and place orders.
                Date & Time: %s
                
                Happy Shopping!
                The Ecommerce Team
                """, name, formattedDateTime);
        sendEmail(toEmail, subject, body);
        log.info("Welcome email sent to: {}", toEmail);
    }

    private void sendLoggedInEmail(String toEmail, String name) {
        String subject = "Account Logged In";
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        String formattedDateTime = now.format(formatter);
        String body = String.format("""
            Dear %s,

            Your account was successfully logged in.

            Date & Time: %s

            If this was not you, please secure your account immediately.

            Regards,
            Your Team
            """, name, formattedDateTime);
        sendEmail(toEmail, subject, body);
        log.info("Login email sent to: {}", toEmail);
    }

    private void sendPasswordResetEmail(String toEmail, String name, String resetToken) {
        String subject   = "Password Reset Request";
        String resetLink = "https://ecommerce.com/reset-password?token=" + resetToken;
        String body = String.format("""
                Dear %s,

                We received a request to reset your password.

                Click the link below to reset your password (valid for 1 hour):
                %s

                If you didn't request this, please ignore this email.

                The Ecommerce Team
                """, name, resetLink);
        sendEmail(toEmail, subject, body);
        log.info("Password reset email sent to: {}", toEmail);
    }

    private void sendOrderStatusEmail(String toEmail, String orderNumber, String status) {
        String subject = "Order Update: " + orderNumber;
        String body = String.format("""
                Your order %s has been updated.

                New Status: %s

                You can track your order in our app.

                The Ecommerce Team
                """, orderNumber, status.replace("_", " "));
        sendEmail(toEmail, subject, body);
        log.info("Order status email sent for order: {}", orderNumber);
    }

    private void sendPaymentEmail(String toEmail, String type, String orderId, String amount) {
        boolean isSuccess = "PAYMENT_SUCCESS".equals(type);
        String subject = isSuccess
                ? "Payment Confirmed - Order #" + orderId
                : "Payment Refunded - Order #" + orderId;
        String body = isSuccess
                ? String.format("Your payment of $%s for order #%s has been confirmed!", amount, orderId)
                : String.format("Your refund of $%s for order #%s has been processed!", amount, orderId);
        sendEmail(toEmail, subject, body);
        log.info("Payment notification sent to: {} for type: {}", toEmail, type);
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromName + " <" + fromEmail + ">");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    //Helper that resolves userId → email and logs if it fails
    private String resolveEmail(String userId) {
        String email = userServiceClient.getEmailByUserId(userId);
        if (email == null) {
            log.warn("Could not resolve email for userId: {} — notification skipped", userId);
        }
        return email;
    }
}
