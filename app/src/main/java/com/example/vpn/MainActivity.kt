package com.example.vpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MyVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "DISCONNECT") {
            stopVpn()
        } else {
            startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return
        val builder = Builder()
        builder.addAddress("10.0.0.2", 24)
        builder.addRoute("0.0.0.0", 0)
        builder.addDnsServer("1.1.1.1")
        builder.setSession("KonyaVPN")
        vpnInterface = builder.establish()
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}

class MainActivity : ComponentActivity() {
    private var isConnected = mutableStateOf(false)

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            toggleVpnService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VpnScreen(
                isConnected = isConnected.value,
                onConnectClick = { handleVpnClick() }
            )
        }
    }

    private fun handleVpnClick() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            toggleVpnService()
        }
    }

    private fun toggleVpnService() {
        val intent = Intent(this, MyVpnService::class.java)
        if (isConnected.value) {
            intent.action = "DISCONNECT"
            startService(intent)
            isConnected.value = false
        } else {
            intent.action = "CONNECT"
            startService(intent)
            isConnected.value = true
        }
    }
}

@Composable
fun VpnScreen(isConnected: Boolean, onConnectClick: () -> Unit) {
    val bgColor1 by animateColorAsState(
        targetValue = if (isConnected) Color(0xFF0F2027) else Color(0xFF141E30),
        animationSpec = tween(1000), label = ""
    )
    val bgColor2 by animateColorAsState(
        targetValue = if (isConnected) Color(0xFF2C5364) else Color(0xFF243B55),
        animationSpec = tween(1000), label = ""
    )
    val buttonColor by animateColorAsState(
        targetValue = if (isConnected) Color(0xFF00E676) else Color(0xFFFF5252),
        animationSpec = tween(500), label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgColor1, bgColor2))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isConnected) "GÜVENLİ BAĞLANTI" else "BAĞLANTI YOK",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            Button(
                onClick = onConnectClick,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                modifier = Modifier.size(160.dp)
            ) {
                Text(
                    text = if (isConnected) "KAPAT" else "BAĞLAN",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = if (isConnected) "IP: Gizlendi" else "IP: Gerçek Konum",
                color = Color.LightGray,
                fontSize = 14.sp
            )
        }
    }
}
