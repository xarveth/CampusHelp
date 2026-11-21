package com.campushelp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_ratings",
       uniqueConstraints = @UniqueConstraint(name = "uk_rating_complaint", columnNames = "complaint_id"))
public class ComplaintRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rating_id")
    private Long id;

    // One rating per complaint.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false)
    private int stars;

    @Column(length = 500)
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public ComplaintRating() {}

    public ComplaintRating(Complaint complaint, User student, int stars, String comment) {
        this.complaint = complaint;
        this.student = student;
        this.stars = stars;
        this.comment = comment;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }
    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }
    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
