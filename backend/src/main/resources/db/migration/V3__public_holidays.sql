CREATE TABLE public_holidays (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    holiday_date DATE NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    calendar_year INT NOT NULL,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO public_holidays (holiday_date, name, calendar_year, description) VALUES
('2026-01-01', 'New Year''s Day', 2026, 'New Year''s Day'),
('2026-02-08', 'Tet Holiday', 2026, 'Lunar New Year holiday'),
('2026-09-02', 'National Day', 2026, 'National Day holiday');
