package com.niyyahmatch.niyyahmatch.dto;

import com.niyyahmatch.niyyahmatch.entity.Match;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SwipeResponse {

    private boolean matched;
    private MatchResponse matchDetails;

    public SwipeResponse(boolean matched) {
        this.matched = matched;
        this.matchDetails = null;
    }

    public SwipeResponse(Match match, Long currentUserId) {
        this.matched = true;
        this.matchDetails = new MatchResponse(match, currentUserId);
    }
}
