package com.example.dto;

import java.util.List;

public class EmailRequest {
    private List<String> recipients;
    private String subject;
    private String message;
    private String reportType; // "proveedores" o "contratistas"
    private String customEmail;

    // Getters y Setters
    public List<String> getRecipients() { return recipients; }
    public void setRecipients(List<String> recipients) { this.recipients = recipients; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getCustomEmail() { return customEmail; }
    public void setCustomEmail(String customEmail) { this.customEmail = customEmail; }
}