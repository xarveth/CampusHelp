package com.campushelp.service;

import com.campushelp.dto.DuplicateCheckResponse;
import com.campushelp.entity.Complaint;
import com.campushelp.entity.ComplaintStatus;
import com.campushelp.repository.ComplaintRepository;
import com.campushelp.repository.SameIssueRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Duplicate detection. Compares a new complaint's subject + description with
 * the open complaints in the same category using word-set (Jaccard)
 * similarity, after lower-casing and dropping common stop words.
 * A match scores 0.30 or higher; the best 3 are returned. This never blocks
 * submission — the student sees the warning and decides.
 */
@Service
public class DuplicateDetectionService {

    private static final double THRESHOLD = 0.30;
    private static final int MAX_MATCHES = 3;

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "of", "to", "in", "on", "for", "is",
            "are", "was", "were", "it", "its", "this", "that", "with", "not",
            "please", "my", "our", "we", "i", "me", "has", "have", "been",
            "from", "by", "at", "as", "do", "does", "did", "so", "but", "if");

    private final ComplaintRepository complaints;
    private final SameIssueRepository sameIssues;

    public DuplicateDetectionService(ComplaintRepository complaints,
                                     SameIssueRepository sameIssues) {
        this.complaints = complaints;
        this.sameIssues = sameIssues;
    }

    public DuplicateCheckResponse check(Long categoryId, String subject, String description) {
        Set<String> words = tokenize(subject + " " + (description == null ? "" : description));
        List<DuplicateCheckResponse.DuplicateMatch> scored = new ArrayList<>();
        for (Complaint c : complaints.findOpenByCategoryId(categoryId, ComplaintStatus.RESOLVED)) {
            Set<String> other = tokenize(c.getSubject() + " " + c.getDescription());
            double sim = jaccard(words, other);
            if (sim >= THRESHOLD) {
                scored.add(new DuplicateCheckResponse.DuplicateMatch(
                        c.getId(), c.getCode(), c.getSubject(), c.getStatus().name(),
                        c.getCategory().getName(),
                        sameIssues.countByComplaintId(c.getId()),
                        Math.round(sim * 100.0) / 100.0));
            }
        }
        scored.sort((a, b) -> Double.compare(b.similarity(), a.similarity()));
        return new DuplicateCheckResponse(scored.stream().limit(MAX_MATCHES).toList());
    }

    /** Public for tests: word-set similarity between two texts. */
    public static double similarity(String a, String b) {
        return jaccard(tokenize(a), tokenize(b));
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    private static Set<String> tokenize(String text) {
        Set<String> words = new HashSet<>();
        if (text == null) return words;
        for (String w : text.toLowerCase().split("[^a-z0-9]+")) {
            if (w.length() > 2 && !STOP_WORDS.contains(w)) {
                words.add(w);
            }
        }
        return words;
    }
}
