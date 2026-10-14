package com.example.util

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsHelper {
    private const val TAG = "AnalyticsHelper"
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        if (firebaseAnalytics == null) {
            runCatching {
                firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext).apply {
                    setAnalyticsCollectionEnabled(true)
                }
                Log.i(TAG, "Firebase Analytics initialized successfully with active user tracking.")
            }.onFailure { e ->
                Log.w(TAG, "Failed to initialize Firebase Analytics", e)
            }
        }
    }

    fun logScreenView(screenName: String) {
        runCatching {
            firebaseAnalytics?.let { analytics ->
                val bundle = Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
                }
                analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            }
        }
    }

    fun logDocumentView(resourceId: String, title: String, course: String, academicYear: Int) {
        runCatching {
            firebaseAnalytics?.let { analytics ->
                val bundle = Bundle().apply {
                    putString(FirebaseAnalytics.Param.ITEM_ID, resourceId)
                    putString(FirebaseAnalytics.Param.ITEM_NAME, title.take(100))
                    putString(FirebaseAnalytics.Param.ITEM_CATEGORY, course)
                    putInt("academic_year", academicYear)
                }
                analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
            }
        }
    }

    fun logDocumentDownload(resourceId: String, title: String) {
        runCatching {
            firebaseAnalytics?.let { analytics ->
                val bundle = Bundle().apply {
                    putString(FirebaseAnalytics.Param.ITEM_ID, resourceId)
                    putString(FirebaseAnalytics.Param.ITEM_NAME, title.take(100))
                }
                analytics.logEvent("document_download", bundle)
            }
        }
    }

    fun logDocumentUpload(course: String, year: Int, subject: String, type: String) {
        runCatching {
            firebaseAnalytics?.let { analytics ->
                val bundle = Bundle().apply {
                    putString("course", course)
                    putInt("academic_year", year)
                    putString("subject", subject.take(100))
                    putString("resource_type", type)
                }
                analytics.logEvent("document_uploaded", bundle)
            }
        }
    }

    fun logAdminAction(action: String, submissionId: String) {
        runCatching {
            firebaseAnalytics?.let { analytics ->
                val bundle = Bundle().apply {
                    putString("action_type", action)
                    putString("submission_id", submissionId)
                }
                analytics.logEvent("admin_moderation_action", bundle)
            }
        }
    }
}
