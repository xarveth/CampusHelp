package com.campushelp.repository;

import com.campushelp.entity.SameIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface SameIssueRepository extends JpaRepository<SameIssue, Long> {

    boolean existsByComplaintIdAndStudentId(Long complaintId, Long studentId);

    long countByComplaintId(Long complaintId);

    /** Complaint ids ordered by how many students reported the same problem. */
    @Query("SELECT s.complaint.id, COUNT(s) FROM SameIssue s GROUP BY s.complaint.id ORDER BY COUNT(s) DESC")
    List<Object[]> mostReportedComplaintIds();
}
