@file:OptIn(ExperimentalMaterial3Api::class)

package com.funbox.dipcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("DIP 指撥開關", "HEX 16進制旋鈕")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> DipCalculatorScreen()
            1 -> HexKnobScreen()
        }
    }
}

/* =====================================================================
   功能一：R型中繼器 DIP 開關撥碼換算（8-Pin / 10-Pin 二進位）
   ===================================================================== */
@Composable
fun DipCalculatorScreen() {
    var inputAddress by remember { mutableStateOf("1") }
    var pinCount by remember { mutableIntStateOf(8) }
    var isPinReversed by remember { mutableStateOf(false) }
    var isOnAtBottom by remember { mutableStateOf(false) }

    val currentAddress = inputAddress.toIntOrNull() ?: 0
    val maxAddress = if (pinCount == 8) 255 else 1023

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("R型消防中繼器撥碼計算器", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("支援：宏力/清谷/良豪/永揚/能美/日華/松下 等廠牌", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = pinCount == 8,
                onClick = {
                    pinCount = 8
                    if (currentAddress > 255) inputAddress = "255"
                },
                label = { Text("8-Pin (1~255)") }
            )
            FilterChip(
                selected = pinCount == 10,
                onClick = { pinCount = 10 },
                label = { Text("10-Pin (1~1023)") }
            )
        }

        OutlinedTextField(
            value = inputAddress,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }
                val num = filtered.toIntOrNull() ?: 0
                inputAddress = if (num <= maxAddress) filtered else maxAddress.toString()
            },
            label = { Text("請輸入位址號碼 (Address)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("撥碼開關狀態（點擊開關可切換）", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    FilterChip(
                        selected = isPinReversed,
                        onClick = { isPinReversed = !isPinReversed },
                        label = { Text(if (isPinReversed) "↔️ 順序: N➔1" else "↔️ 順序: 1➔N") }
                    )
                    FilterChip(
                        selected = isOnAtBottom,
                        onClick = { isOnAtBottom = !isOnAtBottom },
                        label = { Text(if (isOnAtBottom) "↕️ ON: 朝下" else "↕️ ON: 朝上") }
                    )
                }

                val pinRange = if (isPinReversed) (pinCount - 1 downTo 0) else (0 until pinCount)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in pinRange) {
                        val bitWeight = 1 shl i
                        val isOn = (currentAddress and bitWeight) != 0
                        PinItem(
                            pinNumber = i + 1,
                            isOn = isOn,
                            weight = bitWeight,
                            isOnAtBottom = isOnAtBottom,
                            onToggle = {
                                val newAddress = if (isOn) currentAddress - bitWeight else currentAddress + bitWeight
                                if (newAddress <= maxAddress) inputAddress = newAddress.toString()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("計算拆解明細：", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                val activePins = (0 until pinCount).filter { (currentAddress and (1 shl it)) != 0 }
                if (activePins.isEmpty()) {
                    Text("所有開關皆為 OFF (位址: 0)", fontSize = 13.sp, color = Color.Gray)
                } else {
                    val breakdown = activePins.joinToString(" + ") { "Pin${it + 1}(權重${1 shl it})" }
                    Text("ON 開關：$breakdown = $currentAddress", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun PinItem(pinNumber: Int, isOn: Boolean, weight: Int, isOnAtBottom: Boolean, onToggle: () -> Unit) {
    val onColor = Color(0xFF2E7D32)
    val offColor = Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onToggle() }
    ) {
        if (!isOnAtBottom) {
            Text(if (isOn) "ON" else "OFF", fontSize = 9.sp, color = if (isOn) onColor else offColor, fontWeight = FontWeight.Bold)
        }

        val alignPosition = when {
            isOn && !isOnAtBottom -> Alignment.TopCenter
            isOn && isOnAtBottom -> Alignment.BottomCenter
            !isOn && !isOnAtBottom -> Alignment.BottomCenter
            else -> Alignment.TopCenter
        }

        Box(
            modifier = Modifier
                .width(20.dp)
                .height(46.dp)
                .background(if (isOn) Color(0xFF4CAF50) else Color(0xFFE0E0E0), RoundedCornerShape(4.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp)),
            contentAlignment = alignPosition
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .padding(2.dp)
                    .background(Color.White, RoundedCornerShape(2.dp))
            )
        }

        if (isOnAtBottom) {
            Text(if (isOn) "ON" else "OFF", fontSize = 9.sp, color = if (isOn) onColor else offColor, fontWeight = FontWeight.Bold)
        }

        Text("$pinNumber", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("$weight", fontSize = 8.sp, color = Color.Gray)
    }
}

/* =====================================================================
   功能二：良豪 / 宏力 HEX 16 進制雙旋鈕對照頁面 (SW1高位 / SW2低位)
   ===================================================================== */
@Composable
fun HexKnobScreen() {
    var mode by remember { mutableIntStateOf(0) }
    var groupText by remember { mutableStateOf("10") }
    var pointText by remember { mutableStateOf("3") }
    var addressText by remember { mutableStateOf("42") }

    var sw1Val by remember { mutableIntStateOf(2) }
    var sw2Val by remember { mutableIntStateOf(10) }

    val hexChars = listOf("0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F")

    fun updateKnobsFromGroupPoint() {
        val g = groupText.toIntOrNull() ?: 1
        val p = pointText.toIntOrNull() ?: 1
        val addr = ((g - 1) * 4) + (p - 1) + 4
        if (addr in 4..255) {
            sw1Val = addr / 16
            sw2Val = addr % 16
        }
    }

    fun updateKnobsFromAddress() {
        val addr = addressText.toIntOrNull() ?: 0
        if (addr in 0..255) {
            sw1Val = addr / 16
            sw2Val = addr % 16
        }
    }

    val currentAddress = (sw1Val * 16) + sw2Val
    val hexString = "${hexChars[sw1Val]}${hexChars[sw2Val]}"
    
    val currentGroup = if (currentAddress >= 4) ((currentAddress - 4) / 4) + 1 else 0
    val currentPoint = if (currentAddress >= 4) ((currentAddress - 4) % 4) + 1 else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("良豪 / 宏力 HEX 雙旋鈕對照", fontSize = 18.sp, FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("對應 16 進制 0~F 雙旋鈕刻度 (SW1 高位 / SW2 低位)", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == 0,
                onClick = { mode = 0; updateKnobsFromGroupPoint() },
                label = { Text("良豪 [組號-點號]") }
            )
            FilterChip(
                selected = mode == 1,
                onClick = { mode = 1; updateKnobsFromAddress() },
                label = { Text("直接位址數字") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (mode == 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = groupText,
                    onValueChange = { groupText = it.filter { c -> c.isDigit() }; updateKnobsFromGroupPoint() },
                    label = { Text("組號 (Group)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = pointText,
                    onValueChange = { pointText = it.filter { c -> c.isDigit() }; updateKnobsFromGroupPoint() },
                    label = { Text("點號 (1~4)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            OutlinedTextField(
                value = addressText,
                onValueChange = { addressText = it.filter { c -> c.isDigit() }; updateKnobsFromAddress() },
                label = { Text("請輸入位址數字 (1~255)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("實體旋鈕位置（點擊可切換數值）", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KnobDisplayItem(
                        label = "SW1 (高位 16¹)",
                        currentValue = hexChars[sw1Val],
                        onValueChange = { sw1Val = (sw1Val + 1) % 16 }
                    )

                    KnobDisplayItem(
                        label = "SW2 (低位 16⁰)",
                        currentValue = hexChars[sw2Val],
                        onValueChange = { sw2Val = (sw2Val + 1) % 16 }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("HEX 刻度碼：$hexString", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("反查與對照結果：", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• 十進位總位址：$currentAddress", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (currentAddress >= 4) {
                    Text("• 良豪對應編碼：$currentGroup-$currentPoint", fontSize = 15.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                } else {
                    Text("• 良豪對應編碼：低於最小起始點號 (1-1 號碼為 4)", fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("• 算式：SW1($sw1Val × 16) + SW2($sw2Val) = $currentAddress", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun KnobDisplayItem(label: String, currentValue: String, onValueChange: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onValueChange() }
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(32.dp))
                .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(currentValue, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text("點擊可切換", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
    }
}
