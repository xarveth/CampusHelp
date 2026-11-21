package com.campushelp.repository;

import com.campushelp.entity.ComplaintRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<ComplaintRating, Long> {
    Optional<ComplaintRating> findByComplaintId(Long complaintId);
    boolean existsByComplaintId(Long complaintId);

    @Query("SELECT AVG(r.stars) FROM ComplaintRating r")
    Double averageStars();

    long countByStars(int stars);
}
