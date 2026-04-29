INSERT INTO medicamento_estados (id, nombre, descripcion) VALUES
(X'A0000000000010008000000000000001', 'vigente',     'Medicamento aprobado y disponible para uso clínico'),
(X'A0000000000010008000000000000002', 'en_revision', 'Medicamento bajo evaluación por nueva evidencia científica'),
(X'A0000000000010008000000000000003', 'obsoleto',    'Medicamento retirado o descontinuado');

INSERT INTO medicamentos (id, nombre, estado_id, descripcion) VALUES
(X'B0000000000010008000000000000001', 'Metformina 850mg',        X'A0000000000010008000000000000001', 'Antidiabético oral de primera línea para DM2'),
(X'B0000000000010008000000000000002', 'Amoxicilina 500mg',       X'A0000000000010008000000000000001', 'Antibiótico betalactámico de amplio espectro'),
(X'B0000000000010008000000000000003', 'Atorvastatina 40mg',      X'A0000000000010008000000000000001', 'Inhibidor de HMG-CoA reductasa para dislipidemia'),
(X'B0000000000010008000000000000004', 'Losartán 50mg',           X'A0000000000010008000000000000001', 'Antagonista del receptor de angiotensina II para HTA'),
(X'B0000000000010008000000000000005', 'Omeprazol 20mg',          X'A0000000000010008000000000000001', 'Inhibidor de bomba de protones para ERGE y úlceras'),
(X'B0000000000010008000000000000006', 'Ibuprofeno 400mg',        X'A0000000000010008000000000000001', 'AINE para dolor e inflamación leve a moderada'),
(X'B0000000000010008000000000000007', 'Paracetamol 500mg',       X'A0000000000010008000000000000001', 'Analgésico y antipirético de primera línea'),
(X'B0000000000010008000000000000008', 'Levotiroxina 50mcg',      X'A0000000000010008000000000000001', 'Hormona tiroidea sintética para hipotiroidismo'),
(X'B0000000000010008000000000000009', 'Empagliflozina 10mg',     X'A0000000000010008000000000000002', 'Inhibidor SGLT-2, bajo revisión por nuevas guías cardiovasculares'),
(X'B000000000001000800000000000000A', 'Rosuvastatina 20mg',      X'A0000000000010008000000000000002', 'Estatina bajo revisión por interacción con metformina en DM2'),
(X'B000000000001000800000000000000B', 'Ciprofloxacino 500mg',    X'A0000000000010008000000000000002', 'Fluoroquinolona bajo revisión por resistencia antimicrobiana'),
(X'B000000000001000800000000000000C', 'Cloranfenicol 500mg',     X'A0000000000010008000000000000003', 'Antibiótico obsoleto por toxicidad hematológica severa'),
(X'B000000000001000800000000000000D', 'Metamizol 500mg',         X'A0000000000010008000000000000003', 'Retirado por riesgo de agranulocitosis'),
(X'B000000000001000800000000000000E', 'Rofecoxib 25mg',          X'A0000000000010008000000000000003', 'Retirado del mercado por riesgo cardiovascular elevado'),
(X'B000000000001000800000000000000F', 'Terfenadina 60mg',        X'A0000000000010008000000000000003', 'Antihistamínico obsoleto por arritmias cardíacas graves');
