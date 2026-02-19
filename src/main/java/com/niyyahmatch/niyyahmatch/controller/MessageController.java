package com.niyyahmatch.niyyahmatch.controller;

import com.niyyahmatch.niyyahmatch.dto.MessageResponse;
import com.niyyahmatch.niyyahmatch.dto.SendMessageRequest;
import com.niyyahmatch.niyyahmatch.entity.Message;
import com.niyyahmatch.niyyahmatch.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches/{matchId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    // @ResponseStatus(CREATED) returns 201 instead of 200 - semantically correct for resource creation.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(@PathVariable Long matchId,
                                       @Valid @RequestBody SendMessageRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Message message = messageService.sendMessage(userId, matchId, request.getContent());
        return new MessageResponse(message);
    }

    @GetMapping
    public Page<MessageResponse> getMessages(@PathVariable Long matchId,
                                             @RequestParam(defaultValue = "0") int page) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // .map(MessageResponse::new) converts each Message entity in the Page to a MessageResponse DTO.
        // This is a method reference - shorthand for: message -> new MessageResponse(message)
        return messageService.getMessages(userId, matchId, page).map(MessageResponse::new);
    }
}
