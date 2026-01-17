package com.guyuexuan.bjxd.model;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

public class TaskStatus {
    private boolean signCompleted;
    private boolean viewCompleted;
    private boolean questionCompleted;

    public TaskStatus() {
        this.signCompleted = false;
        this.viewCompleted = false;
        this.questionCompleted = false;
    }

    public boolean isSignCompleted() {
        return signCompleted;
    }

    public void setSignCompleted(boolean signCompleted) {
        this.signCompleted = signCompleted;
    }

    public boolean isViewCompleted() {
        return viewCompleted;
    }

    public void setViewCompleted(boolean viewCompleted) {
        this.viewCompleted = viewCompleted;
    }

    public boolean isQuestionCompleted() {
        return questionCompleted;
    }

    public void setQuestionCompleted(boolean questionCompleted) {
        this.questionCompleted = questionCompleted;
    }

    public boolean isAllCompleted() {
        return signCompleted && viewCompleted && questionCompleted;
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
