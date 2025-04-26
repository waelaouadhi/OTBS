package com.otbs.training.controller;

import com.otbs.leave.dto.MessageResponse;
import com.otbs.training.dto.TrainingRequestDTO;
import com.otbs.training.dto.TrainingResponseDTO;
import com.otbs.training.service.TrainingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trainings")
@RequiredArgsConstructor
@Slf4j
public class TrainingController {
    private final TrainingService trainingService;

    @PostMapping
    @PreAuthorize("hasAuthority('Manager')")
    public ResponseEntity<MessageResponse> createTraining(@Valid @RequestBody TrainingRequestDTO trainingRequestDTO) {
        trainingService.createTraining(trainingRequestDTO);
        return ResponseEntity.ok(new MessageResponse("Training created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Manager')")
    public ResponseEntity<MessageResponse> updateTraining(@Valid @RequestBody TrainingRequestDTO trainingRequestDTO,@PathVariable("id") Long id) {
        trainingService.updateTraining(trainingRequestDTO, id);
        return ResponseEntity.ok(new MessageResponse("Training updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Manager')")
    public ResponseEntity<MessageResponse> deleteTraining(@PathVariable("id") Long id) {
        trainingService.deleteTraining(id);
        return ResponseEntity.ok(new MessageResponse("Training deleted successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HR') or hasAuthority('Manager') or hasAuthority('Employee')")
    public ResponseEntity<List<TrainingResponseDTO>> getAllTrainings() {
        return ResponseEntity.ok(trainingService.getAllTrainings());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('HR') or hasAuthority('Manager')")
    public ResponseEntity<TrainingResponseDTO> getTrainingById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(trainingService.getTrainingById(id));
    }
}
