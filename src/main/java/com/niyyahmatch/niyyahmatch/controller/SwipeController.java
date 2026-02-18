package com.niyyahmatch.niyyahmatch.controller;

import com.niyyahmatch.niyyahmatch.dto.CandidateResponse;
import com.niyyahmatch.niyyahmatch.dto.SwipeQuotaResponse;
import com.niyyahmatch.niyyahmatch.service.CandidateService;
import com.niyyahmatch.niyyahmatch.service.MatchService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/swipes")
public class SwipeController {

    private final CandidateService candidateService;
    private final MatchService matchService;

    public SwipeController(CandidateService candidateService, MatchService matchService) {
        this.candidateService = candidateService;
        this.matchService = matchService;
    }

    @GetMapping("/candidates")
    public Page<CandidateResponse> getCandidates(@RequestParam(defaultValue = "0") int page) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return candidateService.getCandidates(userId, page).map(CandidateResponse::new);
    }

    @GetMapping("/remaining")
    public SwipeQuotaResponse getRemainingSwipes() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        int remaining = matchService.getRemainingSwipes(userId);
        return new SwipeQuotaResponse(remaining, 12);
    }
}
