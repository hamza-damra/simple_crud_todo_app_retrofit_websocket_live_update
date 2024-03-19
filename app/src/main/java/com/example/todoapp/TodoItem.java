package com.example.todoapp;

public class TodoItem {
    private String _id;
    private String title;

    public TodoItem(String id) {
        _id = id;
    }

    public TodoItem(String id, String title) {
        _id = id;
        this.title = title;
    }

    public TodoItem(){}

    public String getTitle() {
        return title;
    }


    public void setTitle(String updatedTitle) {
        this.title = updatedTitle;
    }

    public String getId() {
        return _id;
    }
}
