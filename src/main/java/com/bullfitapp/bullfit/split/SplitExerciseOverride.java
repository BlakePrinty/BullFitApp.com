package com.bullfitapp.bullfit.split;

import jakarta.persistence.*;

@Entity
@Table(name = "split_exercise_override")
public class SplitExerciseOverride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_split_id", nullable = false)
    private Long userSplitId;
    @Column(name = "split_day_exercise_id", nullable = false)
    private Long splitDayExerciseId;
    @Column(name = "replacement_exercise_id", nullable = false)
    private Long replacementExerciseId;
    // getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserSplitId() {
        return userSplitId;
    }

    public void setUserSplitId(Long userSplitId) {
        this.userSplitId = userSplitId;
    }

    public Long getSplitDayExerciseId() {
        return splitDayExerciseId;
    }

    public void setSplitDayExerciseId(Long splitDayExerciseId) {
        this.splitDayExerciseId = splitDayExerciseId;
    }

    public Long getReplacementExerciseId() {
        return replacementExerciseId;
    }

    public void setReplacementExerciseId(Long replacementExerciseId) {
        this.replacementExerciseId = replacementExerciseId;
    }
}
