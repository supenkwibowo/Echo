package com.cecekfosfore.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager

class PermissionManager(private val activity: Activity) {

    enum class Permission {
        ALLOWED,
        PARTIALLY_ALLOWED,
        NOT_ALLOWED
    }

    private var onResult: ((Permission) -> Unit)? = null

    fun request(onResult: (Permission) -> Unit) {
        this.onResult = onResult
        val permission = Manifest.permission.RECORD_AUDIO
        if (activity.isPermissionAllowed(permission)) {
            onResult?.invoke(Permission.ALLOWED)
        } else {
            activity.requestPermission(permission)
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSION_RECORD_CONTEXT_ID -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                    onResult?.invoke(Permission.ALLOWED)
                } else {
                    onResult?.invoke(Permission.NOT_ALLOWED)
                }
            }
            else -> return
        }
    }

    private fun Activity.isPermissionAllowed(permission: String): Boolean =
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun Activity.requestPermission(permission: String) {
        if (shouldShowRequestPermissionRationale(permission)) {
            onResult?.invoke(Permission.NOT_ALLOWED)
        } else {
            requestPermissions(arrayOf(permission), REQUEST_PERMISSION_RECORD_CONTEXT_ID)
        }
    }

    private val REQUEST_PERMISSION_RECORD_CONTEXT_ID = 8000
}