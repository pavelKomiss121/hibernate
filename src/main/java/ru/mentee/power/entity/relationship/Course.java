package ru.mentee.power.entity.relationship;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;

/**
 * Курс с обратной ManyToMany связью.
 */
@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@NamedEntityGraph(
        name = "course-with-students",
        attributeNodes = {@NamedAttributeNode("students"), @NamedAttributeNode("enrollments")})
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "credits")
    private Integer credits;

    @Column(name = "duration_hours")
    private Integer durationHours;

    // Обратная сторона простого ManyToMany
    @ManyToMany(mappedBy = "simpleCourses", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Student> students = new HashSet<>();

    // ManyToMany с атрибутами
    @OneToMany(
            mappedBy = "course",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<Enrollment> enrollments = new ArrayList<>();

    // Статистика
    @Formula("(SELECT COUNT(*) FROM student_courses sc WHERE sc.course_id = id)")
    private Integer studentCount;

    @Formula(
            """
		(SELECT AVG(CASE e.grade
			WHEN 'A' THEN 4.0
			WHEN 'B' THEN 3.0
			WHEN 'C' THEN 2.0
			WHEN 'D' THEN 1.0
			ELSE 0.0 END)
		 FROM enrollments e WHERE e.course_id = id)
		""")
    private Double averageGrade;
}
