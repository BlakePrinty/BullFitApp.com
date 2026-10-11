package com.bullfitapp.bullfit.split;

import jakarta.persistence.*;

@Entity
@Table(name = "split_day_exercise")
public class SplitDayExercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "split_day_id", nullable = false) private Long splitDayId;
    @Column(name = "exercise_id", nullable = false) private Long exerciseId;
    @Column(name = "order_index", nullable = false) private int orderIndex;
    @Column(name = "target_sets") private Integer targetSets;
    @Column(name = "target_reps") private String targetReps;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSplitDayId() {
        return splitDayId;
    }

    public void setSplitDayId(Long splitDayId) {
        this.splitDayId = splitDayId;
    }

    public Long getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(Long exerciseId) {
        this.exerciseId = exerciseId;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Integer getTargetSets() {
        return targetSets;
    }

    public void setTargetSets(Integer targetSets) {
        this.targetSets = targetSets;
    }

    public String getTargetReps() {
        return targetReps;
    }

    public void setTargetReps(String targetReps) {
        this.targetReps = targetReps;
    }
}
