package com.example.test.ui.notifications

import androidx.lifecycle.*
import com.example.test.data.DrivingAngelRepository
import com.example.test.data.database.entities.HeartRateEntity
import kotlinx.coroutines.launch


class NotificationsViewModel(private val repository: DrivingAngelRepository) : ViewModel() {

    // Using LiveData and caching what allHeartRates returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val allHeartRates: LiveData<List<HeartRateEntity>> = repository.allHeartRates.asLiveData()

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(heartRate: HeartRateEntity) = viewModelScope.launch {
        repository.insert(heartRate)
    }

    /**
     * Launching a new coroutine to delete all the data in a non-blocking way
     */
    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }
}

class DrivingAngelViewModelFactory(private val repository: DrivingAngelRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}