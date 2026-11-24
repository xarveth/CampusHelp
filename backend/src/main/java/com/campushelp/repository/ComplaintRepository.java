package com.campushelp.repository;

import com.campushelp.entity.Complaint;
import com.campushelp.entity.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Optional<Complaint> findByCode(String code);

    List<Complaint> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<Complaint> findByAssignedToIdOrderByCreatedAtDesc(Long staffId);

    List<Complaint> findAllByOrderByCreatedAtDesc();

    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);

    long countByStatus(ComplaintStatus status);

    /** Open complaints in a category, newest first (duplicate detection scans these). */
    @Query("SELECT c FROM Complaint c WHERE c.category.id = :categoryId AND c.status <> :resolved ORDER BY c.createdAt DESC")
    List<Complaint> findOpenByCategoryId(Long categoryId, ComplaintStatus resolved);

    /** Resolved complaints, for the average-resolution-time calculation. */
    @Query("SELECT c FROM Complaint c WHERE c.status = 'RESOLVED' AND c.resolvedAt IS NOT NULL")
    List<Complaint> findResolvedWithResolvedAt();

    @Query("SELECT c.category.name, COUNT(c) FROM Complaint c GROUP BY c.category.name ORDER BY COUNT(c) DESC")
    List<Object[]> countByCategory();

    /** How many times each subject was submitted (frequently reported problems). */
    @Query("SELECT c.subject, COUNT(c) FROM Complaint c GROUP BY c.subject ORDER BY COUNT(c) DESC")
    List<Object[]> countBySubject();
}
