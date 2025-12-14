package ru.mentee.power.optimization;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Описание ассоциации для загрузки.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FetchAssociation {

    private String path;
    private JoinType joinType = JoinType.LEFT;

    public enum JoinType {
        INNER,
        LEFT,
        RIGHT
    }
}
