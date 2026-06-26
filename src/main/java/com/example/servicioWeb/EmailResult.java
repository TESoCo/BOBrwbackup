// EmailResult.java - Clase compartida
package com.example.servicioWeb;

import java.util.ArrayList;
import java.util.List;

public class EmailResult {
    private int successCount = 0;
    private int failedCount = 0;
    private List<String> failedEmails = new ArrayList<>();

    public void incrementSuccess() { successCount++; }
    public void incrementFailed() { failedCount++; }
    public void addFailedEmail(String email) { failedEmails.add(email); }

    public int getSuccessCount() { return successCount; }
    public int getFailedCount() { return failedCount; }
    public List<String> getFailedEmails() { return failedEmails; }
}