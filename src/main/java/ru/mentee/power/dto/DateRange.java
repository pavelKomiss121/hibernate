package ru.mentee.power.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Диапазон дат.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DateRange {
    private LocalDateTime from;
    private LocalDateTime to;
}
