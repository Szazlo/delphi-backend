package com.davidwilson.delphi.repositories;

import com.davidwilson.delphi.entities.SubmissionReviews;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionReviewsRepository extends JpaRepository<SubmissionReviews, String> {
    List<SubmissionReviews> findBySubmissionId(String id);
    boolean existsBySubmissionId(String id);

    List<SubmissionReviews> findByReviewerId(String reviewerId);
}