package ru.mentee.power.entity.relationship;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Промежуточная сущность для ManyToMany с атрибутами.
 */
@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_enrollment_student_course",
                    columnNames = {"student_id", "course_id"})
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @EmbeddedId private EnrollmentId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentId")
    @JoinColumn(
            name = "student_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_enrollment_student"))
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("courseId")
    @JoinColumn(
            name = "course_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_enrollment_course"))
    private Course course;

    @Column(name = "enrollment_date", nullable = false)
    private LocalDate enrollmentDate;

    @Column(name = "grade", length = 2)
    private String grade;

    @Column(name = "attendance_percentage")
    private Integer attendancePercentage;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    public Enrollment(Student student, Course course) {
        this.student = student;
        this.course = course;
        this.id = new EnrollmentId(student.getId(), course.getId());
        this.enrollmentDate = LocalDate.now();
    }
}
