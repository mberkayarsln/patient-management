package com.pm.patientservice.service;

import com.pm.patientservice.dto.CreatePatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.dto.UpdatePatientRequestDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;

    public List<PatientResponseDTO> getPatients() {
        List<Patient> patients = patientRepository.findAll();
        List<PatientResponseDTO> patientResponseDTOS = new ArrayList<>();

        for (Patient patient : patients) {
            patientResponseDTOS.add(preparePatientResponseDTO(patient));
        }

        return patientResponseDTOS;
    }

    public PatientResponseDTO createPatient(CreatePatientRequestDTO createPatientRequestDTO) {

        if (patientRepository.existsByEmail(createPatientRequestDTO.getEmail())) {
            throw new EmailAlreadyExistsException("A patient with this email already exists: " + createPatientRequestDTO.getEmail());
        }

        Patient patient = preparePatientEntity(createPatientRequestDTO);

        patientRepository.save(patient);

        billingServiceGrpcClient.createBillingAccount(patient.getId().toString(), patient.getName(), patient.getEmail());

        return preparePatientResponseDTO(patient);
    }

    public PatientResponseDTO updatePatient(UUID id, UpdatePatientRequestDTO updatePatientRequestDTO) {
        Patient patient = patientRepository.findById(id).orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + id));

        if (patientRepository.existsByEmailAndIdNot(updatePatientRequestDTO.getEmail(), id)) {
            throw new EmailAlreadyExistsException("A patient with this email already exists: " + updatePatientRequestDTO.getEmail());
        }

        patient.setName(updatePatientRequestDTO.getName());
        patient.setEmail(updatePatientRequestDTO.getEmail());
        patient.setAddress(updatePatientRequestDTO.getAddress());
        patient.setDateOfBirth(LocalDate.parse(updatePatientRequestDTO.getDateOfBirth()));

        Patient updatedPatient = patientRepository.save(patient);

        return preparePatientResponseDTO(updatedPatient);
    }

    public void deletePatient(UUID id) {
        patientRepository.deleteById(id);
    }


    private PatientResponseDTO preparePatientResponseDTO(Patient patient) {
        PatientResponseDTO patientResponseDTO = new PatientResponseDTO();
        patientResponseDTO.setId(patient.getId().toString());
        patientResponseDTO.setName(patient.getName());
        patientResponseDTO.setEmail(patient.getEmail());
        patientResponseDTO.setAddress(patient.getAddress());
        patientResponseDTO.setDateOfBirth(patient.getDateOfBirth().toString());

        return patientResponseDTO;
    }

    private Patient preparePatientEntity(CreatePatientRequestDTO createPatientRequestDTO) {
        Patient patient = new Patient();

        patient.setName(createPatientRequestDTO.getName());
        patient.setEmail(createPatientRequestDTO.getEmail());
        patient.setAddress(createPatientRequestDTO.getAddress());
        patient.setDateOfBirth(LocalDate.parse(createPatientRequestDTO.getDateOfBirth()));
        patient.setRegisteredDate(LocalDate.parse(createPatientRequestDTO.getRegisteredDate()));

        return patient;
    }
}
