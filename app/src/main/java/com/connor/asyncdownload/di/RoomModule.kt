package com.connor.asyncdownload.di

import android.content.Context
import androidx.room.Room
import com.connor.asyncdownload.model.room.DownDataBase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RoomModule {

    @Singleton
    @Provides
    fun provideDownDataBase(@ApplicationContext ctx: Context) =
        Room.databaseBuilder(ctx, DownDataBase::class.java, "down_database").build()

    @Singleton
    @Provides
    fun provideDownDao(dataBase: DownDataBase) = dataBase.downDao()
}