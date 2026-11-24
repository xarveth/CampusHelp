package com.campushelp.controller;

import com.campushelp.service.PublicService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Public endpoints for the homepage — no login needed, no private data. */
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final PublicService publicService;

    public PublicController(PublicService publicService) {
        this.publicService = publicService;
    }

    /** Aggregate numbers: total, resolved, open, resolution rate, avg resolution time. */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return publicService.stats();
    }

    /** Anonymised board of open complaints (subject + category only). */
    @GetMapping("/complaints")
    public List<Map<String, Object>> openBoard() {
        return publicService.openBoard();
    }
}
