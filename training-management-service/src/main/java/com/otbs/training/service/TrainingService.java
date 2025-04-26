package com.otbs.training.service;

import com.otbs.training.dto.TrainingRequestDTO;
import com.otbs.training.dto.TrainingResponseDTO;

import java.util.List;

public interface TrainingService {
    void createTraining(TrainingRequestDTO trainingRequestDTO);
    void updateTraining(TrainingRequestDTO trainingRequestDTO, Long trainingId);
    void deleteTraining(Long trainingId);
    TrainingResponseDTO getTrainingById(Long trainingId);
    List<TrainingResponseDTO> getAllTrainings();
}
