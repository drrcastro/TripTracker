package pt.drprint3d.triptracker.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.drprint3d.triptracker.R
import pt.drprint3d.triptracker.data.LocationFormatType
import pt.drprint3d.triptracker.data.LocationLog
import pt.drprint3d.triptracker.data.OperatingMode
import pt.drprint3d.triptracker.utils.FormatHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OperatingMode.getDisplayTitle(): String {
    return when (this) {
        OperatingMode.SINGLE_SMS -> stringResource(R.string.mode_single_sms)
        OperatingMode.LOOP_SMS -> stringResource(R.string.mode_loop_sms)
        OperatingMode.JUST_LOG -> stringResource(R.string.mode_just_log)
    }
}

@Composable
fun OperatingMode.getDisplayDesc(): String {
    return when (this) {
        OperatingMode.SINGLE_SMS -> stringResource(R.string.mode_single_sms_desc)
        OperatingMode.LOOP_SMS -> stringResource(R.string.mode_loop_sms_desc)
        OperatingMode.JUST_LOG -> stringResource(R.string.mode_just_log_desc)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onPickContactClick: () -> Unit,
    onExportGpxClick: () -> Unit,
    onRequestPermissionsClick: () -> Unit,
    hasPermissions: Boolean
) {
    val baseContext = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val currentLangTag by viewModel.appLanguage.collectAsState()

    val localizedContext = remember(currentLangTag) {
        val locale = Locale.forLanguageTag(currentLangTag)
        Locale.setDefault(locale)
        val config = Configuration(baseConfig)
        config.setLocale(locale)
        baseContext.createConfigurationContext(config)
    }

    val localizedConfiguration = remember(currentLangTag) {
        val locale = Locale.forLanguageTag(currentLangTag)
        Configuration(baseConfig).apply {
            setLocale(locale)
        }
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedConfiguration
    ) {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current

        val contactName by viewModel.contactName.collectAsState()
        val contactPhone by viewModel.contactPhone.collectAsState()
        val operatingMode by viewModel.operatingMode.collectAsState()
        val formatType by viewModel.formatType.collectAsState()
        val customTemplate by viewModel.customTemplate.collectAsState()
        val intervalMinutes by viewModel.intervalMinutes.collectAsState()
        val isTrackingActive by viewModel.isTrackingActive.collectAsState()
        val isLoadingSingleSms by viewModel.isLoadingSingleSms.collectAsState()
        val logCount by viewModel.logCount.collectAsState(initial = 0)
        val logsList by viewModel.logsList.collectAsState(initial = emptyList())

        var showMenu by remember { mutableStateOf(false) }
        var showSettingsDialog by remember { mutableStateOf(false) }
        var showAboutDialog by remember { mutableStateOf(false) }
        var showLogsDialog by remember { mutableStateOf(false) }
        var showClearLogsConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                navigationIcon = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showSettingsDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.about)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showAboutDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Permission Warning Banner if missing permissions
            if (!hasPermissions) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.permissions_required_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.permissions_required_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestPermissionsClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(stringResource(R.string.grant_permissions))
                        }
                    }
                }
            }

            // Status Badge if tracking active
            AnimatedVisibility(visible = isTrackingActive) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.tracking_active_title),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF1B5E20)
                            )
                            Text(
                                text = "${stringResource(R.string.mode_label)}: ${operatingMode.getDisplayTitle()} (${intervalMinutes}m)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }

            // 1. Operating Mode Selection
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.mode_of_operation),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OperatingMode.entries.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.updateOperatingMode(mode) }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = (operatingMode == mode),
                                onClick = { viewModel.updateOperatingMode(mode) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = mode.getDisplayTitle(),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = mode.getDisplayDesc(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 2. Main Action Button
            if (operatingMode == OperatingMode.SINGLE_SMS) {
                Button(
                    onClick = { viewModel.sendSingleSms(context) },
                    enabled = !isLoadingSingleSms && hasPermissions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoadingSingleSms) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.obtaining_gps))
                    } else {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.send_sms_now),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.toggleTracking(context) },
                    enabled = hasPermissions,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTrackingActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isTrackingActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTrackingActive) stringResource(R.string.stop_tracking) else stringResource(R.string.start_tracking),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Quick Info summary card showing selected recipient & interval
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.current_config),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showSettingsDialog = true }) {
                            Text(stringResource(R.string.change_in_settings))
                        }
                    }
                    if (operatingMode != OperatingMode.JUST_LOG) {
                        Text(
                            text = "${stringResource(R.string.contact)}: ${if (contactPhone.isNotBlank()) "${contactName.ifBlank { contactPhone }} ($contactPhone)" else stringResource(R.string.none_selected)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "${stringResource(R.string.format)}: ${formatType.title}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (operatingMode != OperatingMode.SINGLE_SMS) {
                        Text(
                            text = "${stringResource(R.string.interval)}: $intervalMinutes ${stringResource(R.string.minutes)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // 3. Location Logs & GPX Export Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.history_gpx_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$logCount ${stringResource(R.string.records)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onExportGpxClick,
                            enabled = logCount > 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.export_gpx))
                        }

                        OutlinedButton(
                            onClick = { showLogsDialog = true },
                            enabled = logCount > 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListNumbered,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.view_logs))
                        }
                    }

                    if (logCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { showClearLogsConfirm = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.clear_history))
                        }
                    }
                }
            }
        }
    }

    // --- Definições (Settings Dialog) ---
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings))
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // 1. Language Selection
                    Column {
                        Text(
                            text = stringResource(R.string.app_language),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val currentLangTag by viewModel.appLanguage.collectAsState()
                        val languages = listOf(
                            "pt" to stringResource(R.string.lang_pt),
                            "en" to stringResource(R.string.lang_en),
                            "es" to stringResource(R.string.lang_es),
                            "de" to stringResource(R.string.lang_de),
                            "fr" to stringResource(R.string.lang_fr)
                        )

                        var langDropdownExpanded by remember { mutableStateOf(false) }
                        val currentLangName = languages.find { it.first == currentLangTag }?.second ?: languages.first().second

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { langDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentLangName,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("▼")
                            }
                            DropdownMenu(
                                expanded = langDropdownExpanded,
                                onDismissRequest = { langDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                languages.forEach { (tag, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            viewModel.updateAppLanguage(tag)
                                            langDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // 2. Contacto de Destino
                    if (operatingMode != OperatingMode.JUST_LOG) {
                        Column {
                            Text(
                                text = stringResource(R.string.target_contact),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    if (contactName.isNotBlank()) {
                                        Text(
                                            text = contactName,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Text(
                                        text = contactPhone.ifBlank { stringResource(R.string.none_selected) },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (contactPhone.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(onClick = {
                                    showSettingsDialog = false
                                    onPickContactClick()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContactPage,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.choose))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = contactPhone,
                                onValueChange = { viewModel.updateContactPhoneOnly(it) },
                                label = { Text(stringResource(R.string.phone_number)) },
                                placeholder = { Text("+351912345678") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        HorizontalDivider()
                    }

                    // 3. Formato da Mensagem & Pre-visualizacao
                    Column {
                        Text(
                            text = stringResource(R.string.message_format),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        var dropdownExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { dropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = formatType.title,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("▼")
                            }
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                LocationFormatType.entries.forEach { fmt ->
                                    DropdownMenuItem(
                                        text = { Text(fmt.title) },
                                        onClick = {
                                            viewModel.updateFormatType(fmt)
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (formatType == LocationFormatType.CUSTOM) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customTemplate,
                                onValueChange = { viewModel.updateCustomTemplate(it) },
                                label = { Text(stringResource(R.string.custom_template)) },
                                placeholder = { Text("Ex: https://maps.google.com/?q={lat},{lng}") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.preview),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val sampleText = FormatHelper.formatLocationMessage(
                                latitude = 38.71667,
                                longitude = -9.13333,
                                formatType = formatType,
                                customTemplate = customTemplate
                            )
                            Text(
                                text = sampleText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 4. Intervalo de Envio/Registo (dropdown)
                    if (operatingMode != OperatingMode.SINGLE_SMS) {
                        HorizontalDivider()

                        Column {
                            Text(
                                text = stringResource(R.string.sending_interval),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val minutesStr = stringResource(R.string.minutes)
                            val intervalOptions = listOf(
                                1 to "1 $minutesStr",
                                2 to "2 $minutesStr",
                                5 to "5 $minutesStr",
                                10 to "10 $minutesStr",
                                15 to "15 $minutesStr",
                                30 to "30 $minutesStr",
                                60 to "60 $minutesStr (1 h)",
                                120 to "120 $minutesStr (2 h)"
                            )

                            var intervalDropdownExpanded by remember { mutableStateOf(false) }
                            val currentLabel = intervalOptions.find { it.first == intervalMinutes }?.second ?: "$intervalMinutes $minutesStr"

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { intervalDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = currentLabel,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("▼")
                                }
                                DropdownMenu(
                                    expanded = intervalDropdownExpanded,
                                    onDismissRequest = { intervalDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    intervalOptions.forEach { (mins, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                viewModel.updateIntervalMinutes(mins)
                                                intervalDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSettingsDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }

    // --- Sobre / About Dialog ---
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.about))
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = stringResource(R.string.version),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${stringResource(R.string.made_with_love)} ",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            try {
                                uriHandler.openUri("https://linktr.ee/drprint3d")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "linktr.ee/drprint3d",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // --- Logs Dialog ---
    if (showLogsDialog) {
        AlertDialog(
            onDismissRequest = { showLogsDialog = false },
            title = { Text("${stringResource(R.string.history_gpx_title)} ($logCount)") },
            text = {
                if (logsList.isEmpty()) {
                    Text(stringResource(R.string.no_records))
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logsList) { log ->
                            LogItemRow(log = log)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLogsDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // --- Clear Logs Confirmation Dialog ---
    if (showClearLogsConfirm) {
        AlertDialog(
            onDismissRequest = { showClearLogsConfirm = false },
            title = { Text(stringResource(R.string.clear_history_confirm_title)) },
            text = { Text(stringResource(R.string.clear_history_confirm_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearLogs()
                        showClearLogsConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogsConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
}

@Composable
fun LogItemRow(log: LocationLog) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(log.timestamp))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = log.mode,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Lat: ${log.latitude}, Lng: ${log.longitude}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            if (!log.recipientPhone.isNullOrEmpty()) {
                Text(
                    text = "Para: ${log.recipientName ?: log.recipientPhone} | SMS: ${if (log.smsSent) "Enviada" else "Falhou"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (log.smsSent) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
