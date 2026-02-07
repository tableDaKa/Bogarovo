package com.bogarovo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.telephony.SmsManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bogarovo.data.StockItem
import com.bogarovo.data.TaskItem
import com.bogarovo.ui.theme.BogarovoTheme
import com.bogarovo.ui.theme.RedLowStock
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BogarovoTheme {
                BogarovoApp()
            }
        }
    }
}

private enum class BogarovoDestination(val title: String) {
    STOCKS("Zásoby"),
    SMS("SMS"),
    FORECAST("Předpověď"),
    PLANNER("Plánovač")
}

@Composable
private fun BogarovoApp(viewModel: BogarovoViewModel = viewModel()) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val destinations = BogarovoDestination.values().toList()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Bogarovo",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                destinations.forEach { destination ->
                    NavigationDrawerItem(
                        label = { Text(destination.title) },
                        selected = false,
                        onClick = {
                            navController.navigate(destination.name) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                                launchSingleTop = true
                            }
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Bogarovo hospodářství") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->
            BogarovoNavHost(
                navController = navController,
                viewModel = viewModel,
                paddingValues = padding
            )
        }
    }
}

@Composable
private fun BogarovoNavHost(
    navController: NavHostController,
    viewModel: BogarovoViewModel,
    paddingValues: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = BogarovoDestination.STOCKS.name,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(BogarovoDestination.STOCKS.name) {
            StockScreen(viewModel)
        }
        composable(BogarovoDestination.SMS.name) {
            SmsScreen()
        }
        composable(BogarovoDestination.FORECAST.name) {
            ForecastScreen(viewModel)
        }
        composable(BogarovoDestination.PLANNER.name) {
            PlannerScreen(viewModel)
        }
    }
}

@Composable
private fun StockScreen(viewModel: BogarovoViewModel) {
    val items by viewModel.stockItems.collectAsStateWithLifecycle()
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (items.isEmpty()) {
            EmptyState(
                title = "Žádné zásoby",
                message = "Přidejte první položku do skladu."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    StockCard(item = item, onDelta = { delta ->
                        viewModel.updateStockAmount(item, item.currentAmount + delta)
                    })
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.bulkDeduct() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("🐄")
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Přidat zásobu")
        }
    }

    if (showDialog) {
        AddStockDialog(
            onDismiss = { showDialog = false },
            onSave = { name, amount, unit, limit, batch ->
                viewModel.addStockItem(name, amount, unit, limit, batch)
                showDialog = false
            }
        )
    }
}

@Composable
private fun StockCard(item: StockItem, onDelta: (Double) -> Unit) {
    val isLow = item.currentAmount < item.lowLimit
    val background = if (isLow) RedLowStock.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(background)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (isLow) {
                    Text(
                        text = "NÍZKÉ ZÁSOBY",
                        color = RedLowStock,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Stav: ${item.currentAmount} ${item.unit}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Limit: ${item.lowLimit} ${item.unit} • Dávka: ${item.batchSize} ${item.unit}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButtonWithLabel(
                    icon = Icons.Default.Remove,
                    label = "-${item.batchSize}",
                    onClick = { onDelta(-item.batchSize) }
                )
                IconButtonWithLabel(
                    icon = Icons.Default.Add,
                    label = "+${item.batchSize}",
                    onClick = { onDelta(item.batchSize) }
                )
            }
        }
    }
}

@Composable
private fun IconButtonWithLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.height(0.dp))
        Text(text = label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun AddStockDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, String, Double, Double) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("") }
    var limit by rememberSaveable { mutableStateOf("") }
    var batch by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nová položka") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InputField(value = name, onValueChange = { name = it }, label = "Název")
                InputField(value = amount, onValueChange = { amount = it }, label = "Aktuální stav")
                InputField(value = unit, onValueChange = { unit = it }, label = "Jednotka")
                InputField(value = limit, onValueChange = { limit = it }, label = "Limit pro nízké zásoby")
                InputField(value = batch, onValueChange = { batch = it }, label = "Velikost dávky")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    val limitValue = limit.toDoubleOrNull() ?: 0.0
                    val batchValue = batch.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && unit.isNotBlank()) {
                        onSave(name, amountValue, unit, limitValue, batchValue)
                    }
                }
            ) {
                Text("Uložit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}

@Composable
private fun InputField(value: String, onValueChange: (String) -> Unit, label: String) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EmptyState(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }
}

data class SmsRecipient(
    val name: String,
    val phone: String,
    val selected: Boolean = true
)

@Composable
private fun SmsScreen() {
    val context = LocalContext.current
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val recipients = remember { mutableStateListOf<SmsRecipient>() }
    var message by rememberSaveable {
        mutableStateOf(
            "Dobry den, pokud jste doposud nevyzvedli svou objednavku, tuto lze vyzvednout " +
                "do soboty 9:00. S podekovanim Bogarovo hospodarstvi. PS: Jestli jste si tuto " +
                "vyzvedli prosíme nezapomínejte odebrat také svou dodejku....."
        )
    }
    var statusMessage by rememberSaveable { mutableStateOf("") }

    val cameraPermissionGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    val smsPermissionGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED

    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        statusMessage = if (granted) "Fotoaparát připraven." else "Bez oprávnění nelze fotit."
    }
    val requestSmsPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        statusMessage = if (granted) "SMS oprávnění uděleno." else "Bez oprávnění nelze posílat SMS."
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap == null) {
            statusMessage = "Fotka nebyla pořízena."
            return@rememberLauncherForActivityResult
        }
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val extracted = extractRecipients(result.text)
                recipients.clear()
                recipients.addAll(extracted)
                statusMessage = if (extracted.isEmpty()) {
                    "Na fotce nebyla nalezena žádná jména s telefonem."
                } else {
                    "Načteno ${extracted.size} kontaktů."
                }
            }
            .addOnFailureListener {
                recipients.clear()
                statusMessage = "OCR se nezdařilo. Zkuste prosím lepší fotku."
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Rozesílání SMS z dodacích listů", style = MaterialTheme.typography.titleMedium)

        Button(onClick = {
            if (!cameraPermissionGranted) {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            } else {
                takePictureLauncher.launch(null)
            }
        }) {
            Text("Vyfotit dodací list")
        }

        if (statusMessage.isNotEmpty()) {
            Text(text = statusMessage, color = MaterialTheme.colorScheme.secondary)
        }

        Text(text = "Nalezení lidé:", style = MaterialTheme.typography.titleSmall)
        if (recipients.isEmpty()) {
            Text("Zatím žádné výsledky.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recipients) { recipient ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = recipient.selected,
                            onCheckedChange = { checked ->
                                val index = recipients.indexOf(recipient)
                                if (index >= 0) {
                                    recipients[index] = recipient.copy(selected = checked)
                                }
                            }
                        )
                        Column {
                            Text(text = recipient.name, fontWeight = FontWeight.Bold)
                            Text(text = recipient.phone)
                        }
                    }
                }
            }
        }

        Text(text = "SMS zpráva:", style = MaterialTheme.typography.titleSmall)
        androidx.compose.material3.OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = {
            if (!smsPermissionGranted) {
                requestSmsPermission.launch(Manifest.permission.SEND_SMS)
                return@Button
            }
            val smsManager = SmsManager.getDefault()
            val selectedRecipients = recipients.filter { it.selected }
            if (selectedRecipients.isEmpty()) {
                statusMessage = "Nejsou vybraní příjemci."
                return@Button
            }
            selectedRecipients.forEach { recipient ->
                smsManager.sendTextMessage(recipient.phone, null, message, null, null)
            }
            statusMessage = "SMS byly odeslány: ${selectedRecipients.size}"
        }) {
            Text("Odeslat hromadně")
        }
    }
}

private fun extractRecipients(text: String): List<SmsRecipient> {
    val results = mutableListOf<SmsRecipient>()
    val phoneRegex = Regex("""(\\+?\\d[\\d\\s-]{7,})""")
    text.lines().forEach { line ->
        val match = phoneRegex.find(line)
        if (match != null) {
            val phone = match.value.replace(" ", "").replace("-", "")
            val name = line.replace(match.value, "").trim()
            if (name.isNotBlank()) {
                results.add(SmsRecipient(name = name, phone = phone))
            }
        }
    }
    return results
}

@Composable
private fun ForecastScreen(viewModel: BogarovoViewModel) {
    val items by viewModel.stockItems.collectAsStateWithLifecycle()
    if (items.isEmpty()) {
        EmptyState(title = "Žádná data", message = "Nejdřív přidejte zásoby.")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            val days = if (item.batchSize <= 0) {
                "N/A"
            } else {
                (item.currentAmount / item.batchSize).toInt().toString()
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = item.name, fontWeight = FontWeight.Bold)
                    Text(text = "Dní do konce: $days")
                }
            }
        }
    }
}

@Composable
private fun PlannerScreen(viewModel: BogarovoViewModel) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val stocks by viewModel.stockItems.collectAsStateWithLifecycle()
    var showDialog by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (tasks.isEmpty()) {
            EmptyState(title = "Žádné úkoly", message = "Přidejte úkol do kalendáře.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks) { task ->
                    TaskCard(task = task, onToggle = { viewModel.toggleTaskCompletion(task) })
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Přidat úkol")
        }
    }

    if (showDialog) {
        AddTaskDialog(
            stockItems = stocks,
            onDismiss = { showDialog = false },
            onSave = { title, date, stockId ->
                viewModel.addTask(title, date, stockId)
                showDialog = false
            }
        )
    }
}

@Composable
private fun TaskCard(task: TaskItem, onToggle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(checked = task.completed, onCheckedChange = { onToggle() })
                Column {
                    Text(text = task.title, fontWeight = FontWeight.Bold)
                    Text(text = "Datum: ${task.dueDate}")
                }
            }
            if (task.stockItemId != null) {
                Text(text = "Po splnění odečte dávku ze zásob.")
            }
        }
    }
}

@Composable
private fun AddTaskDialog(
    stockItems: List<StockItem>,
    onDismiss: () -> Unit,
    onSave: (String, String, Long?) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf("") }
    var selectedStockId by rememberSaveable { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nový úkol") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InputField(value = title, onValueChange = { title = it }, label = "Název úkolu")
                InputField(value = date, onValueChange = { date = it }, label = "Datum (např. 20.5.2024)")
                Text(text = "Propojit se zásobami (volitelné):")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    stockItems.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedStockId = item.id }
                                .padding(4.dp)
                        ) {
                            Checkbox(
                                checked = selectedStockId == item.id,
                                onCheckedChange = { checked ->
                                    selectedStockId = if (checked) item.id else null
                                }
                            )
                            Text(text = item.name)
                        }
                    }
                    if (stockItems.isEmpty()) {
                        Text(text = "Nejdříve přidejte zásoby.")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank() && date.isNotBlank()) {
                    onSave(title, date, selectedStockId)
                }
            }) {
                Text("Uložit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}
