package com.example.todoapp;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;

public class MainViewModel extends ViewModel {

    private final MutableLiveData<List<TodoItem>> todoItemsLiveData = new MutableLiveData<>();

    public LiveData<List<TodoItem>> getTodoItemsLiveData() {
        return todoItemsLiveData;
    }

    public void setTodoItemsLiveData(List<TodoItem> todoItems) {
        todoItemsLiveData.postValue(todoItems);
    }
}
