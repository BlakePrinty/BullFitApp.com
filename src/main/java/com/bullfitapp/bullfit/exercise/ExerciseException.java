package com.bullfitapp.bullfit.exercise;

public class ExerciseException extends RuntimeException {

    private final String field; // null means a general (non-field) error

    public ExerciseException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() { return field; }
}
