package com.example.dto;

import java.util.List;

public class EmailRequest {
    private List<String> recipients;
    private String subject;
    private String message;
    private String reportType;
    private String customEmail;
    private Long idObraSelect;
    private Long idObraTexto;
    private String idUsuario;
    private Long idAPU;
    private String fecha;

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

    public Long getIdObraSelect() { return idObraSelect; }
    public void setIdObraSelect(Long idObraSelect) { this.idObraSelect = idObraSelect; }

    public Long getIdObraTexto() { return idObraTexto; }
    public void setIdObraTexto(Long idObraTexto) { this.idObraTexto = idObraTexto; }

    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    public Long getIdAPU() { return idAPU; }
    public void setIdAPU(Long idAPU) { this.idAPU = idAPU; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
}