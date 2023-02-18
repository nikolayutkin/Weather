package com.example.weather

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    val liveDataCurent = MutableLiveData<String>()
    val liveDataList = MutableLiveData<List<String>>()
}