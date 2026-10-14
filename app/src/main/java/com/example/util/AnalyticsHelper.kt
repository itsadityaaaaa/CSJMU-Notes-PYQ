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

    fun logDocumentView(resourceId: String, title: String, course: String, academicYear: Int, resourceType: String = "NOTE") {
        runCatching {
            firebaseAnalytics?.let { analytics ->
                val bundle = Bundle().apply {
                    putString(FirebaseAnalytics.Param.ITEM_ID, resourceId)
                    putString(FirebaseAnalytics.Param.ITEM_NAME, title.take(100))
                    putString(FirebaseAnalytics.Param.ITEM_CATEGORY, course)
                    putString(FirebaseAnalytics.Param.CONTENT_TYPE, resourceType)
                    putInt("academic_year", academicYear)
                }
                analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
                analytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle)
            }
        }
    }

    fun logDocumentDownload(resourceId: String, title: String, resourceType: String = "NOTE") {
        runCatching {
            firebaseAnalytics?.let { analytics ->
                val bundle = Bundle().apply {
                    putString(FirebaseAnalytics.Param.ITEM_ID, resourceId)
                    putString(FirebaseAnalytics.Param.ITEM_NAME, title.take(100))
                    putString(FirebaseAnalytics.Param.CONTENT_TYPE, resourceType)
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

    fun logSearch(query: String, resultCount: Int) {
        if (query.isBlank()) return
        runCatching {
            firebaseAnalytics?.let { analytics ->
                val bundle = Bundle().apply {
                    putString(FirebaseAnalytics.Param.SEARCH_TERM, query.take(100))
                    putInt("result_count", resultCount)
                }
                analytics.logEvent(FirebaseAnalytics.Event.SEARCH, bundle)
            }
        }
    }

    fun logFilterSelect(college: String, course: String, year: Int, semester: Int) {
        runCatching {
            firebaseAnalytics?.let { analytics ->
                val bundle = Bundle().apply {
                    putString("college", college)
                    putString("course", course)
                    putInt("academic_year", year)
                    putInt("semester", semester)
                }
                analytics.logEvent("catalog_filter_applied", bundle)
            }
        }
    }

    fun setUserAcademicPreference(course: String, year: Int) {
        runCatching {
            firebaseAnalytics?.apply {
                setUserProperty("preferred_course", course)
                setUserProperty("academic_year", year.toString())
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
