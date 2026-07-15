package com.riad.bizaccount

import android.app.Application
import com.riad.bizaccount.data.repository.CategoryRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BizAccountApplication : Application() {

    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch(Dispatchers.IO) {
            categoryRepository.seedDefaultsIfEmpty()
        }
    }
}
