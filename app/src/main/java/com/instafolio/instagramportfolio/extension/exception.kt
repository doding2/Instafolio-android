package com.instafolio.instagramportfolio.extension

// android.permission.MANAGE_EXTERNAL_STORAGE 퍼미션이
// 승인되지 않았는데 관련 작업을 하려고 할 때 필요
class NoManageStoragePermissionException(message: String): Exception(message)

// android.permission.READ_EXTERNAL_STORAGE 퍼미션이
// 승인되지 않았는데 관련 작업을 하려고 할 때 필요
class NoReadStoragePermissionException(message: String): Exception(message)