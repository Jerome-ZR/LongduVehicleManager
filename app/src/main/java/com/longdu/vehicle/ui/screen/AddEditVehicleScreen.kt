package com.longdu.vehicle.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.longdu.vehicle.viewmodel.VehicleListViewModel
import java.time.LocalDate

/**
 * 添加/编辑车辆页面 — 表单输入
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVehicleScreen(onBack: () -> Unit, editPlate: String? = null) {
    val vm: VehicleListViewModel = viewModel()

    var plate by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2020") }
    var vin by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("0") }
    var purchaseDate by remember { mutableStateOf("") }
    var intervalKm by remember { mutableStateOf("3000") }
    var maintainRule by remember { mutableStateOf("1年或3000公里") }
    var bodyNum by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editPlate != null) "编辑车辆" else "添加车辆") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(plate, { plate = it }, label = { Text("车牌号 *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(brand, { brand = it }, label = { Text("品牌") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(model, { model = it }, label = { Text("型号") }, modifier = Modifier.weight(1f), singleLine = true)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(year, { year = it }, label = { Text("年份") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(color, { color = it }, label = { Text("颜色") }, modifier = Modifier.weight(1f), singleLine = true)
            }

            OutlinedTextField(vin, { vin = it }, label = { Text("VIN码") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(mileage, { mileage = it }, label = { Text("当前里程(km)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(intervalKm, { intervalKm = it }, label = { Text("保养间隔(km)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }

            OutlinedTextField(purchaseDate, { purchaseDate = it }, label = { Text("购买日期 (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(maintainRule, { maintainRule = it }, label = { Text("保养规则") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(bodyNum, { bodyNum = it }, label = { Text("车身编号") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(owner, { owner = it }, label = { Text("使用人") }, modifier = Modifier.weight(1f), singleLine = true)
            }

            OutlinedTextField(remark, { remark = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            Button(onClick = {
                if (plate.isBlank()) return@Button
                var pd: LocalDate? = null
                try { purchaseDate.takeIf { it.isNotBlank() }?.let { pd = LocalDate.parse(it) } } catch (_: Exception) {}
                vm.addVehicle(
                    plateNumber = plate, brand = brand, model = model, year = year.toIntOrNull() ?: 2020,
                    vinCode = vin, mileage = mileage.toDoubleOrNull() ?: 0.0, purchaseDate = pd,
                    maintainIntervalKm = intervalKm.toDoubleOrNull() ?: 3000.0, maintainRule = maintainRule,
                    bodyNumber = bodyNum.toIntOrNull(), color = color, ownerName = owner, remark = remark
                )
                onBack()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (editPlate != null) "💾 更新" else "➕ 添加车辆")
            }
        }
    }
}
