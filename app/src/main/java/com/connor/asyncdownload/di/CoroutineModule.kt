package com.connor.asyncdownload.di

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(ActivityComponent::class)
object CoroutineModule {

    @Provides
    @ActivityScoped
    fun provideCoroutineScope(activity: Activity): CoroutineScope {
        return (activity as ComponentActivity).lifecycleScope
    }
}