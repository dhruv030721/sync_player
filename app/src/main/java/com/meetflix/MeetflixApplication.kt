package com.meetflix

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Design Pattern: Application / Hilt Entry Point - Root application class initializing Hilt dependency injection graph.
 */
@HiltAndroidApp
class MeetflixApplication : Application()
