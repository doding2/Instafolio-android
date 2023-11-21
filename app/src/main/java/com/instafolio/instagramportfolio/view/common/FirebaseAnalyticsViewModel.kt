package com.instafolio.instagramportfolio.view.common

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.instafolio.instagramportfolio.model.ResultSlide

class FirebaseAnalyticsViewModel(context: Context) : ViewModel() {
    private val analyticsLiveData = MutableLiveData<FirebaseAnalytics>()
    private val analytics get() = analyticsLiveData.value

    init {
        FirebaseApp.initializeApp(context)
        analyticsLiveData.value = Firebase.analytics
    }

    // 갤러리에서 불러오기
    fun logEventLoadFromGallery(numOfImages: Long, format: String) {
        analytics?.logEvent("load_images_from_gallery") {
            param("number_of_files", numOfImages)
            param("format", format)
        }
    }

    // 드라이브에서 불러오기
    fun logEventLoadFromDrive(numOfFiles: Long, format: String) {
        analytics?.logEvent("load_images_from_gallery") {
            param("number_of_files", numOfFiles)
            param("format", format)
        }
    }

    // 프로젝트 선택
    fun logEventSelectResultSlide(resultSlide: ResultSlide) {
        analytics?.logEvent("select_project") {
            param("id", resultSlide.id)
            param("number_of_images", resultSlide.size.toLong())
            param("downloaded_format", resultSlide.format)
        }
    }

    // 프로젝트 선택 해제
    fun logEventUnselectResultSlide(resultSlide: ResultSlide) {
        analytics?.logEvent("unselect_project") {
            param("id", resultSlide.id)
            param("number_of_images", resultSlide.size.toLong())
            param("downloaded_format", resultSlide.format)
        }
    }

    // 프로젝트 선택 취소
    fun logEventCancelResultSlides(resultSlides: List<ResultSlide>) {
        analytics?.logEvent("cancel_selecting_projects") {
            param("number_of_canceled_projects", resultSlides.size.toLong())
        }

        resultSlides.forEach {
            logEventUnselectResultSlide(it)
        }
    }

    // 프로젝트 편집
    fun logEventEditResultSlides(resultSlides: List<ResultSlide>) {
        analytics?.logEvent("click_edit_button") {
            param("number_of_edited_projects", resultSlides.size.toLong())
        }

        resultSlides.forEach {
            analytics?.logEvent("edit_projects") {
                param("id", it.id)
                param("number_of_images", it.size.toLong())
                param("downloaded_format", it.format)
            }
        }
    }

    // 프로젝트 삭제
    fun logEventDeleteResultSlides(resultSlides: List<ResultSlide>) {
        analytics?.logEvent("click_delete_button") {
            param("number_of_deleted_projects", resultSlides.size.toLong())
        }

        resultSlides.forEach {
            analytics?.logEvent("delete_project") {
                param("id", it.id)
                param("number_of_images", it.size.toLong())
                param("downloaded_format", it.format)
            }
        }
    }

    // 편집 화면에서 뒤로가기
    fun logEventBackFromEditScreen() {
        analytics?.logEvent("click_back_button_from_edit_screen", null)
    }

    // 다음으로 가기 버튼
    fun logEventNextFromEditScreen() {
        analytics?.logEvent("click_next_button_from_edit_screen", null)
    }

    // 바인딩 버튼 클릭
    fun logEventBinding(isBound: Boolean) {
        analytics?.logEvent("click_binding_button") {
            param("isBound", isBound.toString())
        }
    }

    // 바인딩 버튼 클릭
    fun logEventInstaSize(isInstaSized: Boolean) {
        analytics?.logEvent("click_binding_button") {
            param("isInstaSized", isInstaSized.toString())
        }
    }

    // 툴팁 버튼
    fun logEventTooltip() {
        analytics?.logEvent("click_tooltip_button", null)
    }

    // 툴팁 화면에서 뒤로가기
    fun logEventBackFromTooltip() {
        analytics?.logEvent("click_back_button_from_tooltip", null)
    }

    // 프리뷰 화면에서 뒤로가기
    fun logEventBackFromPreviewScreen() {
        analytics?.logEvent("click_back_button_from_preview_screen", null)
    }

    // 다운로드 버튼
    fun logEventDownloadButton() {
        analytics?.logEvent("click_download_button", null)
    }

    fun logEventDownloadStart(numOfDownloading: Long, format: String) {
        analytics?.logEvent("download_start") {
            param("number_of_downloading_files", numOfDownloading)
            param("format", format)
        }
    }

    fun logEventCancelDownloading() {
        analytics?.logEvent("cancel_downloading", null)
    }

    fun logEventShowAd() {
        analytics?.logEvent("show_ad", null)
    }

    fun logDismissAd() {
        analytics?.logEvent("dismiss_ad", null)
    }

    fun logEventDownloadCompleted() {
        analytics?.logEvent("download_completed", null)
    }

    fun logEventBackToHome() {
        analytics?.logEvent("back_to_home_screen", null)
    }

    fun logEventError(message: String) {
        analytics?.logEvent("error_occur") {
            param("message", message)
        }
    }

}