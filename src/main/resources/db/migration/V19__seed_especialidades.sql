-- Fixed UUIDs for the 16 seeded specialties: X'...' binary literals work on
-- both MySQL and H2. The 2000-namespace prefix keeps them disjoint from the
-- role seed (1000-namespace in V3).
INSERT INTO especialidades (id, nombre, slug, descripcion) VALUES
    (X'00000000000020008000000000000001', 'Cardiología',       'cardiologia',         'Diseases of the heart and vascular system'),
    (X'00000000000020008000000000000002', 'Endocrinología',    'endocrinologia',      'Hormonal and metabolic disorders'),
    (X'00000000000020008000000000000003', 'Neurología',        'neurologia',          'Disorders of the nervous system'),
    (X'00000000000020008000000000000004', 'Oncología',         'oncologia',           'Cancer diagnosis and treatment'),
    (X'00000000000020008000000000000005', 'Pediatría',         'pediatria',           'Medical care of infants, children, and adolescents'),
    (X'00000000000020008000000000000006', 'Gastroenterología', 'gastroenterologia',   'Digestive system disorders'),
    (X'00000000000020008000000000000007', 'Neumología',        'neumologia',          'Respiratory system diseases'),
    (X'00000000000020008000000000000008', 'Dermatología',      'dermatologia',        'Skin, hair, and nail conditions'),
    (X'00000000000020008000000000000009', 'Ginecología',       'ginecologia',         'Female reproductive system'),
    (X'0000000000002000800000000000000A', 'Urología',          'urologia',            'Urinary tract and male reproductive system'),
    (X'0000000000002000800000000000000B', 'Psiquiatría',       'psiquiatria',         'Mental, emotional, and behavioral disorders'),
    (X'0000000000002000800000000000000C', 'Reumatología',      'reumatologia',        'Joint, muscle, and autoimmune diseases'),
    (X'0000000000002000800000000000000D', 'Oftalmología',      'oftalmologia',        'Eye and vision disorders'),
    (X'0000000000002000800000000000000E', 'Hematología',       'hematologia',         'Blood and blood-forming organ disorders'),
    (X'0000000000002000800000000000000F', 'Medicina Interna',  'medicina-interna',    'Adult internal medicine'),
    (X'00000000000020008000000000000010', 'Infectología',      'infectologia',        'Infectious diseases');
