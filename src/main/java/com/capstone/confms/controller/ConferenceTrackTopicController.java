package com.capstone.confms.controller;

import com.capstone.confms.dto.ConferenceTrackTopicDTO;
import com.capstone.confms.dto.response.ConferenceTrackTopicResponseDTO;
import com.capstone.confms.service.ConferenceTrackTopicService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conference-track-topics")
@RequiredArgsConstructor
@Tag(name = "Conference Track Topic Management", description = "Operations related to Conference Track Topic setup")
public class ConferenceTrackTopicController {

    private final ConferenceTrackTopicService topicService;

    @PostMapping
    public ResponseEntity<ConferenceTrackTopicResponseDTO> createTopic(@RequestBody ConferenceTrackTopicDTO dto) {
        ConferenceTrackTopicResponseDTO createdTopic = topicService.createTopic(dto);
        return new ResponseEntity<>(createdTopic, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConferenceTrackTopicResponseDTO> updateTopic(
            @PathVariable Integer id,
            @RequestBody ConferenceTrackTopicDTO dto) {
        return ResponseEntity.ok(topicService.updateTopic(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConferenceTrackTopicResponseDTO> getTopicById(@PathVariable Integer id) {
        return ResponseEntity.ok(topicService.getTopicById(id));
    }

    @GetMapping
    public ResponseEntity<List<ConferenceTrackTopicResponseDTO>> getAllTopics() {
        return ResponseEntity.ok(topicService.getAllTopics());
    }

    @GetMapping("/track/{trackId}")
    public ResponseEntity<List<ConferenceTrackTopicResponseDTO>> getTopicsByTrackId(@PathVariable Integer trackId) {
        return ResponseEntity.ok(topicService.getTopicsByTrackId(trackId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable Integer id) {
        topicService.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }
}