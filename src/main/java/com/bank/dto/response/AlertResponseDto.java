package com.bank.dto.response;

import com.bank.model.Alert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponseDto {
    private String id;
    private String transaction;
    private String alertType;
    private String description;
    private String status;
    private Date createDate;
    private Date processedDate;
    private String processedBy;
    private String resolutionNotes;

    public static AlertResponseDto build(Alert alert) {
        return AlertResponseDto.builder()
                .id(alert.getId())
                .transaction(alert.getTransaction().getId())
                .alertType(alert.getAlertType().toString())
                .description(alert.getDescription())
                .status(alert.getStatus().toString())
                .createDate(alert.getCreateDate())
                .processedDate(alert.getProcessedDate())
                .processedBy(alert.getProcessedBy())
                .resolutionNotes(alert.getResolutionNotes())
                .build();
    }
}