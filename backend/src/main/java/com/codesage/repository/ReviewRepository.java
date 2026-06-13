package com.codesage.repository;

import com.codesage.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Review entity with custom queries for analytics and filtering.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Find all reviews for a specific repository
     */
    Page<Review> findByRepositoryOwnerAndRepositoryNameOrderByCreatedAtDesc(
            String repositoryOwner,
            String repositoryName,
            Pageable pageable);

    /**
     * Find review by repository and PR number
     */
    Optional<Review> findByRepositoryOwnerAndRepositoryNameAndPrNumber(
            String repositoryOwner,
            String repositoryName,
            Integer prNumber);

    boolean existsByRepositoryOwnerAndRepositoryNameAndPrNumber(
            String repositoryOwner,
            String repositoryName,
            Integer prNumber);

    /**
     * Find all reviews with a specific status
     */
    List<Review> findByStatusOrderByCreatedAtDesc(Review.ReviewStatus status);

    /**
     * Find reviews created after a specific timestamp
     */
    List<Review> findByCreatedAtAfterOrderByCreatedAtDesc(Instant after);

    /**
     * Get total count of reviews
     */
    @Query("SELECT COUNT(r) FROM Review r")
    long getTotalReviewCount();

    /**
     * Get average quality score across all reviews
     */
    @Query("SELECT AVG(r.qualityScore) FROM Review r WHERE r.status = 'COMPLETED'")
    Double getAverageQualityScore();

    /**
     * Get total count of issues found
     */
    @Query("SELECT COUNT(i) FROM Review r JOIN r.issues i WHERE r.status = 'COMPLETED'")
    long getTotalIssuesCount();

    /**
     * Get count of active (pending) reviews
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.status = 'PENDING'")
    long getActivePRCount();

    /**
     * Get reviews by repository with pagination
     */
    @Query("SELECT r FROM Review r WHERE r.repositoryOwner = :owner AND r.repositoryName = :name ORDER BY r.createdAt DESC")
    Page<Review> findByRepository(@Param("owner") String owner, @Param("name") String name, Pageable pageable);

    /**
     * Get recent reviews (last N days)
     */
    @Query("SELECT r FROM Review r WHERE r.createdAt >= :since ORDER BY r.createdAt DESC")
    List<Review> findRecentReviews(@Param("since") Instant since);

    /**
     * Find reviews with quality score below threshold
     */
    @Query("SELECT r FROM Review r WHERE r.qualityScore < :threshold AND r.status = 'COMPLETED' ORDER BY r.qualityScore ASC")
    List<Review> findLowQualityReviews(@Param("threshold") Double threshold);
}
