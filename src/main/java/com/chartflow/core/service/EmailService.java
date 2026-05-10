package com.chartflow.core.service;

public interface EmailService {
    boolean sendHtmlEmail(String toEmail, String subject, String htmlContent);
    boolean isAvailable();
}
