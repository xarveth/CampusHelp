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

    List<Complaint> findAllByOrderByCreatedAtDesc();

    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);

    long countByStatus(ComplaintStatus status);

    @Query("SELECT c.category.name, COUNT(c) FROM Complaint c GROUP BY c.category.name ORDER BY COUNT(c) DESC")
    List<Object[]> countByCategory();
}
