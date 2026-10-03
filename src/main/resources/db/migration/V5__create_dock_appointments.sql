CREATE TABLE dock_appointments (
    id               BIGSERIAL PRIMARY KEY,
    carrier          VARCHAR(200) NOT NULL,
    truck_plate      VARCHAR(10)  NOT NULL,
    dock             INT          NOT NULL,
    date             DATE         NOT NULL,
    start_time       TIME         NOT NULL,
    end_time         TIME         NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    attachment_path  VARCHAR(500),
    created_at       TIMESTAMP    NOT NULL
);

-- agendamentos migrados da planilha de docas (set/2026)
INSERT INTO dock_appointments (carrier, truck_plate, dock, date, start_time, end_time, status, created_at) VALUES
    ('Transportes Rápido Sul', 'ABC1D23', 1, '2026-10-05', '08:00', '09:00', 'SCHEDULED', '2026-09-28 10:12:00'),
    ('Rodo Norte Cargas', 'QWE4R56', 1, '2026-10-05', '08:30', '09:30', 'SCHEDULED', '2026-09-29 15:40:00'),
    ('LogBR Express', 'RTY7U89', 2, '2026-10-05', '09:00', '10:00', 'SCHEDULED', '2026-09-30 08:05:00'),
    ('Transportes Rápido Sul', 'ABC1D23', 3, '2026-10-06', '13:00', '14:00', 'SCHEDULED', '2026-09-30 11:20:00'),
    ('Cargas Paulista', 'ZXC2V34', 2, '2026-10-06', '07:00', '08:00', 'SCHEDULED', '2026-10-01 09:00:00'),
    ('LogBR Express', 'rty7u89', 2, '2026-10-06', '07:30', '08:30', 'SCHEDULED', '2026-10-01 17:45:00');
