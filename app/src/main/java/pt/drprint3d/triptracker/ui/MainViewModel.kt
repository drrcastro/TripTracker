package pt.drprint3d.triptracker.ui

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.drprint3d.triptracker.data.AppDatabase
import pt.drprint3d.triptracker.data.LocationFormatType
import pt.drprint3d.triptracker.data.LocationLog
import pt.drprint3d.triptracker.data.OperatingMode
import pt.drprint3d.triptracker.data.PreferencesRepository
import pt.drprint3d.triptracker.service.TrackingForegroundService
import pt.drprint3d.triptracker.utils.FormatHelper
import pt.drprint3d.triptracker.utils.GpxExporter
import pt.drprint3d.triptracker.utils.LocationHelper
import pt.drprint3d.triptracker.utils.SmsHelper
import java.io.OutputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application)
    private val database = AppDatabase.getDatabase(application)
    private val logDao = database.locationLogDao()
    private val locationHelper = LocationHelper(application)
    private val smsHelper = SmsHelper(application)

    val isTrackingActive: StateFlow<Boolean> = TrackingForegroundService.isServiceRunning

    private val _contactName = MutableStateFlow(prefsRepo.contactName)
    val contactName: StateFlow<String> = _contactName.asStateFlow()

    private val _contactPhone = MutableStateFlow(prefsRepo.contactPhone)
    val contactPhone: StateFlow<String> = _contactPhone.asStateFlow()

    private val _operatingMode = MutableStateFlow(prefsRepo.operatingMode)
    val operatingMode: StateFlow<OperatingMode> = _operatingMode.asStateFlow()

    private val _formatType = MutableStateFlow(prefsRepo.formatType)
    val formatType: StateFlow<LocationFormatType> = _formatType.asStateFlow()

    private val _customTemplate = MutableStateFlow(prefsRepo.customTemplate)
    val customTemplate: StateFlow<String> = _customTemplate.asStateFlow()

    private val _intervalMinutes = MutableStateFlow(prefsRepo.intervalMinutes)
    val intervalMinutes: StateFlow<Int> = _intervalMinutes.asStateFlow()

    private val _isLoadingSingleSms = MutableStateFlow(false)
    val isLoadingSingleSms: StateFlow<Boolean> = _isLoadingSingleSms.asStateFlow()

    val logsList = logDao.getAllLogsFlow()
    val logCount = logDao.getLogCountFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    fun updateContact(name: String, phone: String) {
        prefsRepo.contactName = name
        prefsRepo.contactPhone = phone
        _contactName.value = name
        _contactPhone.value = phone
    }

    fun updateContactPhoneOnly(phone: String) {
        prefsRepo.contactPhone = phone
        _contactPhone.value = phone
    }

    fun updateOperatingMode(mode: OperatingMode) {
        prefsRepo.operatingMode = mode
        _operatingMode.value = mode
    }

    fun updateFormatType(type: LocationFormatType) {
        prefsRepo.formatType = type
        _formatType.value = type
    }

    fun updateCustomTemplate(template: String) {
        prefsRepo.customTemplate = template
        _customTemplate.value = template
    }

    fun updateIntervalMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(1, 1440)
        prefsRepo.intervalMinutes = clamped
        _intervalMinutes.value = clamped
    }

    fun sendSingleSms(context: Context) {
        val phone = contactPhone.value.trim()
        if (phone.isBlank()) {
            emitToast("Por favor selecione um contacto ou introduza um número de telefone.")
            return
        }

        viewModelScope.launch {
            _isLoadingSingleSms.value = true
            val location = locationHelper.getCurrentLocation()

            if (location == null) {
                _isLoadingSingleSms.value = false
                emitToast("Não foi possível obter a localização GPS. Certifique-se de que o GPS está ativo.")
                return@launch
            }

            val formattedMsg = FormatHelper.formatLocationMessage(
                latitude = location.latitude,
                longitude = location.longitude,
                formatType = formatType.value,
                customTemplate = customTemplate.value
            )

            val sent = smsHelper.sendSms(phone, formattedMsg)

            val logEntry = LocationLog(
                timestamp = System.currentTimeMillis(),
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = if (location.hasAltitude()) location.altitude else null,
                accuracy = if (location.hasAccuracy()) location.accuracy else null,
                speed = if (location.hasSpeed()) location.speed else null,
                mode = OperatingMode.SINGLE_SMS.name,
                recipientPhone = phone,
                recipientName = contactName.value,
                messageText = formattedMsg,
                smsSent = sent
            )

            logDao.insert(logEntry)
            _isLoadingSingleSms.value = false

            if (sent) {
                emitToast("SMS enviada com sucesso!")
            } else {
                emitToast("Falha ao enviar SMS. Verifique permissões e sinal GSM.")
            }
        }
    }

    fun toggleTracking(context: Context) {
        if (isTrackingActive.value) {
            TrackingForegroundService.stopService(context)
            emitToast("Rastreio em segundo plano parado.")
        } else {
            if (operatingMode.value == OperatingMode.LOOP_SMS && contactPhone.value.isBlank()) {
                emitToast("Para o modo Loop SMS, por favor escolha um contacto de destino.")
                return
            }
            TrackingForegroundService.startService(context)
            emitToast("Rastreio iniciado em segundo plano!")
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            logDao.clearAll()
            emitToast("Histórico de localizações limpo.")
        }
    }

    fun exportGpxToStream(outputStream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allLogs = logDao.getAllLogsAsc()
                GpxExporter.exportToStream(allLogs, outputStream)
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Ficheiro GPX exportado com sucesso!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Erro ao exportar GPX: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun emitToast(msg: String) {
        viewModelScope.launch {
            _toastEvent.emit(msg)
        }
    }
}
