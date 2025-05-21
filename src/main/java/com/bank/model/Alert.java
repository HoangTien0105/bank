package com.bank.model;

import com.bank.enums.AlertStatus;
import com.bank.enums.AlertType;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "alert")
public class Alert extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;
    
    @Column(name = "description", nullable = false)
    private String description;
    
    @Column(name = "status", nullable = false)
    private AlertStatus status; // NEW, PROCESSING, RESOLVED, IGNORED
    
    @Column(name = "processed_date")
    private Date processedDate;
    
    @Column(name = "processed_by")
    private String processedBy;
    
    @Column(name = "resolution_notes")
    private String resolutionNotes;
}