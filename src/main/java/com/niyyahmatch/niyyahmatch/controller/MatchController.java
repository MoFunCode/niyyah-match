package com.niyyahmatch.niyyahmatch.controller;

import com.niyyahmatch.niyyahmatch.dto.MatchResponse;
import com.niyyahmatch.niyyahmatch.entity.Match;
import com.niyyahmatch.niyyahmatch.exception.ResourceNotFoundException;
import com.niyyahmatch.niyyahmatch.service.MatchService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController()
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService){
        this.matchService = matchService;
    }
    @GetMapping("/active")
    public MatchResponse getActiveMatch(){
        // 1. Get userId from SecurityContext (JWT filter put it there)
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 2. Call service to get active match - returns Optional<Match>
        Optional<Match> matchOptional = matchService.getActiveMatch(userId);

        // 3. Handle Optional - if present return DTO, if empty throw 404
        if (matchOptional.isPresent()) {
            Match match = matchOptional.get();
            // Convert Match entity to MatchResponse DTO
            // Pass userId so DTO knows which user is "the other person"
            return new MatchResponse(match, userId);
        } else {
            // No active match - user is free to swipe
            throw new ResourceNotFoundException("No active match found");
        }
    }

}
