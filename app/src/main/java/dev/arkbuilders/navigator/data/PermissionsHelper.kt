package dev.arkbuilders.navigator.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dev.arkbuilders.navigator.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionsHelper @Inject constructor(private val appContext: Context) {
    private val permissionResultFlow = MutableSharedFlow<Boolean>()
    private var writePermLauncher: ActivityResultLauncher<String>? = null
    private var writePermUsingSettingsLauncher: ActivityResultLauncher<String>? =
        null
    private var writePermLauncherR: ActivityResultLauncher<String>? = null

    fun registerActivity(activity: AppCompatActivity) {
        writePermLauncher = buildWritePermLauncher(activity)
        writePermUsingSettingsLauncher =
            buildWritePermsUsingSettingsLauncher(activity)
        writePermLauncherR = buildWritePermLauncherR(activity)
    }

    fun isWritePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Environment.isExternalStorageManager()
        else {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    fun askForWritePermissions(
        fragment: Fragment? = null
    ) {
        val packageUri = "package:" + BuildConfig.APPLICATION_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            writePermLauncherR!!.launch(packageUri)
        } else {
            val rationale =
                fragment?.shouldShowRequestPermissionRationale(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) ?: true

            if (rationale)
                writePermLauncher!!
                    .launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            else
                writePermUsingSettingsLauncher!!.launch(packageUri)
        }
    }

    suspend fun askForWritePermissionsAndAwait(
        scope: CoroutineScope,
        fragment: Fragment? = null
    ): Boolean {
        val granted = customAskAndAwait(scope) {
            askForWritePermissions(fragment)
        }
        return granted
    }

    suspend fun customAskAndAwait(
        scope: CoroutineScope,
        ask: suspend () -> Unit
    ): Boolean {
        var isGranted = false
        val askJob = scope.launch {
            permissionResultFlow.collect {
                isGranted = it
                cancel()
            }
        }

        ask()

        askJob.join()
        return isGranted
    }

    private fun buildWritePermLauncher(
        activity: AppCompatActivity
    ) = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        activity.lifecycleScope.launch {
            permissionResultFlow.emit(isGranted)
        }
    }

    private fun buildWritePermsUsingSettingsLauncher(
        activity: AppCompatActivity
    ) =
        activity.registerForActivityResult(
            FilesAccessUsingSettings(appContext)
        ) { isGranted ->
            activity.lifecycleScope.launch {
                permissionResultFlow.emit(isGranted)
            }
        }

    private fun buildWritePermLauncherR(
        activity: AppCompatActivity
    ) =
        activity.registerForActivityResult(
            ManageFilesAccessPermissionContract()
        ) { isGranted ->
            activity.lifecycleScope.launch {
                permissionResultFlow.emit(isGranted)
            }
        }
}

private class ManageFilesAccessPermissionContract :
    ActivityResultContract<String, Boolean>() {

    @RequiresApi(Build.VERSION_CODES.R)
    override fun createIntent(context: Context, input: String) = Intent(
        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
        Uri.parse(input)
    )

    @RequiresApi(Build.VERSION_CODES.R)
    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return Environment.isExternalStorageManager()
    }
}

private class FilesAccessUsingSettings(private val context: Context) :
    ActivityResultContract<String, Boolean>() {
    override fun createIntent(context: Context, input: String) = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse(input)
    )

    override fun parseResult(resultCode: Int, intent: Intent?) =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) ==
            PackageManager.PERMISSION_GRANTED
}
