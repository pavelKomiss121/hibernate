package ru.mentee.power.entity.mp_178;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Встраиваемый объект для размеров товара.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Dimensions {

    @Column(name = "length_cm")
    private Double length;

    @Column(name = "width_cm")
    private Double width;

    @Column(name = "height_cm")
    private Double height;

    @Column(name = "volume_cubic_cm", insertable = false, updatable = false)
    @Transient // Formula не работает в @Embeddable, используем transient
    private Double volume;

    public Double calculateVolume() {
        if (length != null && width != null && height != null) {
            return length * width * height;
        }
        return 0.0;
    }
}
