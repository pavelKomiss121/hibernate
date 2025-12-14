package ru.mentee.power.entity.relationship;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

/**
 * Студент с ManyToMany связью к курсам.
 */
@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@NamedEntityGraph(
        name = "student-with-courses",
        attributeNodes = @NamedAttributeNode("enrollments"))
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_number", unique = true, nullable = false, length = 50)
    private String studentNumber;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    // Простой ManyToMany
    @ManyToMany(
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY)
    @JoinTable(
            name = "student_courses",
            joinColumns =
                    @JoinColumn(
                            name = "student_id",
                            foreignKey = @ForeignKey(name = "fk_sc_student")),
            inverseJoinColumns =
                    @JoinColumn(
                            name = "course_id",
                            foreignKey = @ForeignKey(name = "fk_sc_course")),
            uniqueConstraints =
                    @UniqueConstraint(
                            name = "uk_student_course",
                            columnNames = {"student_id", "course_id"}),
            indexes = {
                @Index(name = "idx_sc_student", columnList = "student_id"),
                @Index(name = "idx_sc_course", columnList = "course_id")
            })
    @jakarta.persistence.OrderBy("name ASC")
    @BatchSize(size = 20)
    @Builder.Default
    private Set<Course> simpleCourses = new HashSet<>();

    // ManyToMany с дополнительными атрибутами через промежуточную сущность
    @OneToMany(
            mappedBy = "student",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<Enrollment> enrollments = new ArrayList<>();

    // Вспомогательные методы для простого ManyToMany
    public void addCourse(Course course) {
        simpleCourses.add(course);
        course.getStudents().add(this);
    }

    public void removeCourse(Course course) {
        simpleCourses.remove(course);
        course.getStudents().remove(this);
    }

    // Вспомогательные методы для ManyToMany с атрибутами
    public void enrollInCourse(Course course, LocalDate enrollmentDate, String grade) {
        Enrollment enrollment = new Enrollment(this, course);
        enrollment.setEnrollmentDate(enrollmentDate);
        enrollment.setGrade(grade);
        enrollments.add(enrollment);
        course.getEnrollments().add(enrollment);
    }

    public void unenrollFromCourse(Course course) {
        Enrollment enrollment =
                enrollments.stream()
                        .filter(e -> e.getCourse().equals(course))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Not enrolled in course"));
        enrollments.remove(enrollment);
        course.getEnrollments().remove(enrollment);
        enrollment.setStudent(null);
        enrollment.setCourse(null);
    }
}
