ALTER TABLE patients
    ADD CONSTRAINT fk_patient_medico
    FOREIGN KEY (medico_id) REFERENCES usuarios (id);
