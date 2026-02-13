package com.niyyahmatch.niyyahmatch.dto;

import com.niyyahmatch.niyyahmatch.entity.Match;
import com.niyyahmatch.niyyahmatch.entity.MatchStatus;
import com.niyyahmatch.niyyahmatch.entity.User;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResponse {
    private Long matchId;
    private UserResponse matchedUser;
    private MatchStatus status;
    private LocalDateTime matchedAt;

    // Custom constructor to convert Match entity → MatchResponse DTO
    // Takes the Match entity and the current user's ID to determine "the other person"
    public MatchResponse(Match match, Long currentUserId) {
        this.matchId = match.getId();
        this.status = match.getStatus();
        this.matchedAt = match.getMatchedAt();

        // Determine which user is "the other person" (not the current user)
        User otherUser = match.getUser1().getId().equals(currentUserId)
                ? match.getUser2()
                : match.getUser1();

        // Convert the other user to UserResponse (hides password)
        this.matchedUser = new UserResponse(otherUser);
    }
}
