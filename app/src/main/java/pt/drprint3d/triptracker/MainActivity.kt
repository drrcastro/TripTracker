package pt.drprint3d.triptracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import pt.drprint3d.triptracker.ui.MainScreen
import pt.drprint3d.triptracker.ui.MainViewModel
import pt.drprint3d.triptracker.ui.theme.TripTrackerTheme

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var hasAllPermissions by mutableStateOf(false)

    // Contact picker launcher
    private val pickContactLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { contactUri ->
                extractContactInfo(contactUri)
            }
        }
    }

    // GPX File Export Launcher
    private val exportGpxLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/gpx+xml")
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            viewModel.exportGpxToUri(fileUri)
        }
    }

    // Permissions launcher
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        checkPermissionsState()
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Permissões concedidas!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Algumas permissões foram recusadas. Algumas funcionalidades podem não funcionar.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkPermissionsState()

        // Collect toast events
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.toastEvent.collect { message ->
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        setContent {
            TripTrackerTheme {
                MainScreen(
                    viewModel = viewModel,
                    onPickContactClick = { openContactPicker() },
                    onExportGpxClick = {
                        val fileName = "triptracker_${System.currentTimeMillis()}.gpx"
                        exportGpxLauncher.launch(fileName)
                    },
                    onRequestPermissionsClick = { requestAppPermissions() },
                    hasPermissions = hasAllPermissions
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsState()
    }

    private fun checkPermissionsState() {
        val fineLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val sendSms = ContextCompat.checkSelfPermission(
            this, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        hasAllPermissions = fineLocation && sendSms
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionsLauncher.launch(permissions.toTypedArray())
    }

    private fun openContactPicker() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestAppPermissions()
            return
        }

        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        pickContactLauncher.launch(intent)
    }

    private fun extractContactInfo(contactUri: Uri) {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
                var number = if (numberIndex >= 0) cursor.getString(numberIndex) ?: "" else ""

                // Clean phone number format
                number = number.replace(" ", "").replace("-", "")

                viewModel.updateContact(name, number)
                Toast.makeText(this, "Contacto selecionado: $name ($number)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
