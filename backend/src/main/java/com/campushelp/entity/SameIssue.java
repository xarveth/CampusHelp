package com.campushelp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * "I have the same problem" — a student marks an existing complaint as
 * affecting them too, instead of filing a duplicate. One row per
 * (complaint, student); the count feeds the "frequently reported problem"
 * analytics and the duplicate-detection warning.
 */
@Entity
@Table(name = "complaint_same_issue",
       uniqueConstraints = @UniqueConstraint(name = "uk_same_issue",
               columnNames = {"complaint_id", "student_id"}))
public class SameIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "same_issue_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public SameIssue() {}

    public SameIssue(Complaint complaint, User student) {
        this.complaint = complaint;
        this.student = student;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }
    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
