@file:OptIn(ExperimentalMaterial3Api::class)
package com.kidi.app

import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BG_DARK = Color(0xFF1A1A2E)
val BG_CARD = Color(0xFF27293D)
val ACCENT = Color(0xFFA882FF)
val ACCENT_DARK = Color(0xFF7B52E0)
val TEXT_WHITE = Color(0xFFFFFFFF)
val TEXT_GRAY = Color(0xFFB0B0C0)
val SUCCESS = Color(0xFF4ADE80)
val ERROR = Color(0xFFF87171)
val WARNING = Color(0xFFFBBF24)

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val msg = if (granted) "Notifications activées" else "Notifications désactivées"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        
        MonitorService.start(this)
        
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = ACCENT,
                    secondary = ACCENT_DARK,
                    background = BG_DARK,
                    surface = BG_CARD
                )
            ) {
                ChronoFamilleApp()
            }
        }
    }
}

@Composable
fun ChronoFamilleApp() {
    val ctx = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        containerColor = BG_DARK,
        topBar = {
            Column(modifier = Modifier.background(BG_CARD)) {
                TopAppBar(
                    title = { Text("ChronoFamille", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TEXT_WHITE) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BG_CARD),
                    navigationIcon = {
                        IconButton(onClick = {
                            PermissionHelper.requestOverlayPermission(ctx)
                            PermissionHelper.requestNotificationPolicyPermission(ctx)
                            PermissionHelper.requestUsageStatsPermission(ctx)
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Permissions", tint = ACCENT)
                        }
                    },
                    actions = {}
                )
                TabRow(selectedTabIndex = selectedTab, containerColor = BG_CARD) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        text = { Text("Parent", color = if (selectedTab == 0) ACCENT else TEXT_GRAY) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        text = { Text("Enfant", color = if (selectedTab == 1) ACCENT else TEXT_GRAY) })
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> ParentScreen()
                1 -> ChildScreen()
            }
        }
    }
}

@Composable
fun ParentScreen() {
    val ctx = LocalContext.current
    val quickTimes = listOf(5, 15, 30, 60, 120, 240, 480, 1440)
    var selectedTime by remember { mutableStateOf(1440) }
    var children by remember { mutableStateOf(LocalDataStore.getChildren(ctx)) }
    var requests by remember { mutableStateOf(LocalDataStore.getTimeRequests(ctx)) }
    var schoolSchedule by remember { mutableStateOf(LocalDataStore.getSchoolSchedule(ctx)) }
    
    val quickMessages = remember {
        listOf(
            QuickMessage("1", "Tu as le droit 15 min encore"),
            QuickMessage("2", "Pas d'écran d'ici le dodo"),
            QuickMessage("3", "Bonne soirée, à demain"),
            QuickMessage("4", "Je rentre bientôt, on se voit à l'école")
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BG_CARD)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Enfants connectés", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TEXT_WHITE)
                    Spacer(modifier = Modifier.height(12.dp))
                    children.forEach { ChildCard(child = it); Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BG_CARD)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Temps quotidien autorisé", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TEXT_WHITE)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Temps : ${selectedTime / 60} h ${selectedTime % 60} min", color = TEXT_GRAY, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(value = selectedTime.toFloat(),
                        onValueChange = { selectedTime = it.toInt().coerceIn(5, 1440) },
                        valueRange = 5f..1440f,
                        colors = SliderDefaults.colors(thumbColor = ACCENT, activeTrackColor = ACCENT, inactiveTrackColor = Color(0xFF444466)))
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            quickTimes.take(4).forEach { m ->
                                val label = if (m < 60) "$m min" else "${m / 60} h"
                                FilterChip(selected = selectedTime == m,
                                    onClick = { selectedTime = m }, label = { Text(label, fontSize = 13.sp) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            quickTimes.takeLast(4).forEach { m ->
                                val label = if (m < 60) "$m min" else "${m / 60} h"
                                FilterChip(selected = selectedTime == m,
                                    onClick = { selectedTime = m }, label = { Text(label, fontSize = 13.sp) })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = {
                            LocalDataStore.lockForMinutes(ctx, selectedTime)
                            Toast.makeText(ctx, "Verrouillé ${selectedTime / 60}h ${selectedTime % 60}min", Toast.LENGTH_SHORT).show()
                        }, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ACCENT),
                            shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Lock, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Verrouiller", fontWeight = FontWeight.Medium)
                        }
                        Button(onClick = {
                            LocalDataStore.unlock(ctx)
                            Toast.makeText(ctx, "Déverrouillé !", Toast.LENGTH_SHORT).show()
                        }, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444466)),
                            shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.LockOpen, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Déverrouiller", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BG_CARD)) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🏫 Mode École Automatique", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TEXT_WHITE)
                        Text("Lun-Ven 8h-12h / 14h-17h", fontSize = 13.sp, color = TEXT_GRAY)
                    }
                    Switch(checked = schoolSchedule.enabled,
                        onCheckedChange = {
                            schoolSchedule = schoolSchedule.copy(enabled = it)
                            LocalDataStore.setSchoolSchedule(ctx, schoolSchedule)
                            Toast.makeText(ctx, "Mode École: ${if(it) "ACTIF" else "DÉSACTIVÉ"}", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = ACCENT))
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BG_CARD)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Demandes de temps (${requests.filter { it.status == "pending" }.size})",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TEXT_WHITE)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (requests.isEmpty()) {
                        Text("Aucune demande pour le moment", color = TEXT_GRAY, fontSize = 14.sp)
                    } else {
                        requests.forEach { req ->
                            RequestCard(request = req,
                                onApprove = {
                                    LocalDataStore.updateRequestStatus(ctx, req.id, "approved")
                                    LocalDataStore.lockForMinutes(ctx, req.durationMin)
                                    requests = LocalDataStore.getTimeRequests(ctx)
                                    Toast.makeText(ctx, "Accordé ! ${req.durationMin}min", Toast.LENGTH_SHORT).show()
                                },
                                onDeny = {
                                    LocalDataStore.updateRequestStatus(ctx, req.id, "denied")
                                    requests = LocalDataStore.getTimeRequests(ctx)
                                    Toast.makeText(ctx, "Refusé", Toast.LENGTH_SHORT).show()
                                })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BG_CARD)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Messages pré-remplis", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TEXT_WHITE)
                    Spacer(modifier = Modifier.height(12.dp))
                    quickMessages.forEach { msg ->
                        Card(onClick = { Toast.makeText(ctx, "Message copié: ${msg.text}", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF333355))) {
                            Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(msg.text, color = TEXT_WHITE, fontSize = 14.sp)
                                Icon(Icons.Default.Send, null, tint = ACCENT, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChildCard(child: ChildDevice) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF333355))) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(ACCENT, ACCENT_DARK))),
                contentAlignment = Alignment.Center) {
                Text(child.avatar, color = TEXT_WHITE, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(child.name, fontWeight = FontWeight.SemiBold, color = TEXT_WHITE, fontSize = 15.sp)
                Text("En ligne · ${child.remainingTimeMin} min restant", color = TEXT_GRAY, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${child.remainingTimeMin / 60}h ${child.remainingTimeMin % 60}m",
                    color = ACCENT, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                val progress = if (child.totalTimeDayMin > 0) 
                    (child.remainingTimeMin.toFloat() / child.totalTimeDayMin).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(progress = progress,
                    modifier = Modifier.width(60.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = ACCENT, trackColor = Color(0xFF444466))
            }
        }
    }
}

@Composable
fun RequestCard(request: TimeRequest, onApprove: () -> Unit, onDeny: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when(request.status) {
                "approved" -> Color(0xFF204030)
                "denied" -> Color(0xFF402020)
                else -> Color(0xFF333355)
            }
        )) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(request.childName, fontWeight = FontWeight.SemiBold, color = TEXT_WHITE)
                Text(request.time, color = TEXT_GRAY, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(request.message, color = TEXT_GRAY, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(10.dp))
            if (request.status == "pending") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onApprove, modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SUCCESS),
                        shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Accorder ${request.durationMin} min", fontSize = 12.sp)
                    }
                    Button(onClick = onDeny, modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ERROR),
                        shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Refuser", fontSize = 12.sp)
                    }
                }
            } else {
                Text(if(request.status == "approved") "✅ Demande acceptée" else "❌ Demande refusée",
                    color = if(request.status == "approved") SUCCESS else ERROR, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ChildScreen() {
    val ctx = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }
    
    val locked = remember(refreshKey) { LocalDataStore.isLocked(ctx) }
    val remain = remember(refreshKey) { LocalDataStore.getRemainingMinutes(ctx) }
    val schoolOn = remember(refreshKey) { LocalDataStore.getSchoolSchedule(ctx).enabled }
    val isSchoolTime = remember(refreshKey) { LocalDataStore.isSchoolTime(ctx) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            refreshKey++
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        if (locked) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3D2830))) {
                Column(modifier = Modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lock, null, Modifier.size(64.dp), tint = ERROR)
                    Spacer(Modifier.height(20.dp))
                    Text("ÉCRAN VERROUILLÉ", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ERROR)
                    Spacer(Modifier.height(12.dp))
                    Text("${remain / 60} h ${remain % 60} min restantes", fontSize = 18.sp, color = TEXT_WHITE)
                    if (schoolOn && isSchoolTime) {
                        Spacer(Modifier.height(8.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = ACCENT_DARK)) {
                            Text("🏫 Mode École actif", Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                color = TEXT_WHITE, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        val now = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date())
                        val req = TimeRequest(
                            childName = "Mon enfant",
                            message = "Je peux avoir un peu de temps s'il vous plaît ?",
                            durationMin = 15,
                            time = now
                        )
                        LocalDataStore.saveTimeRequest(ctx, req)
                        Toast.makeText(ctx, "Demande envoyée !", Toast.LENGTH_SHORT).show()
                    }, colors = ButtonDefaults.buttonColors(containerColor = ACCENT),
                        shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Demander du temps", fontWeight = FontWeight.Medium)
                    }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF203830))) {
                Column(modifier = Modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LockOpen, null, Modifier.size(64.dp), tint = SUCCESS)
                    Spacer(Modifier.height(20.dp))
                    Text("ÉCRAN DÉVERROUILLÉ", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SUCCESS)
                    Spacer(Modifier.height(12.dp))
                    Text("Profite bien de ton temps !", fontSize = 16.sp, color = TEXT_WHITE)
                    if (schoolOn && isSchoolTime) {
                        Spacer(Modifier.height(8.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = ACCENT_DARK)) {
                            Text("🏫 Mode École actif", Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                color = TEXT_WHITE, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
