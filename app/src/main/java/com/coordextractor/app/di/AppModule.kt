package com.coordextractor.app.di

import android.content.Context
import com.coordextractor.app.capture.CaptureManager
import com.coordextractor.app.ocr.OCRRepository
import com.coordextractor.app.parser.TextParser
import com.coordextractor.app.processing.ImageProcessor
import com.coordextractor.app.util.PreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt dependency injection module
 * Provides singleton instances of app components
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCaptureManager(
        @ApplicationContext context: Context
    ): CaptureManager = CaptureManager(context)

    @Provides
    @Singleton
    fun provideImageProcessor(): ImageProcessor = ImageProcessor()

    @Provides
    @Singleton
    fun provideOCRRepository(): OCRRepository = OCRRepository()

    @Provides
    @Singleton
    fun provideTextParser(): TextParser = TextParser()

    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext context: Context
    ): PreferencesManager = PreferencesManager(context)
}
