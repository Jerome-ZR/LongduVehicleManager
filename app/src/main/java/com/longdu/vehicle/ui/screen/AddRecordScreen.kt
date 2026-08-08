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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.longdu.vehicle.data.database.AppDatabase
import com.longdu.vehicle.data.entity.RecordType
import com.longdu.vehicle.repository.VehicleRepository
import com.longdu.vehicle.viewmodel.MaintenanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * 添加/编辑维修保养记录页面
 * recordId != null 时为编辑模式
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordScreen(plate: String, onBack: () -> Unit, recordId: Long? = null) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val vm: MaintenanceViewModel = viewModel()
    val allVehicles by vm.allVehicles.collectAsStateWithLifecycle()
    val selectedPlate by vm.selectedPlate.collectAsStateWithLifecycle()
    val selectedType by vm.selectedType.collectAsStateWithLifecycle()
    val mileage by vm.mileage.collectAsStateWithLifecycle()
    val cost by vm.cost.collectAsStateWithLifecycle()
    val desc by vm.description.collectAsStateWithLifecycle()
    val shop by vm.shopName.collectAsStateWithLifecycle()
    val location by vm.location.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val isEdit = recordId != null

    // 编辑模式：从数据库加载记录预填充
    LaunchedEffect(recordId) {
        if (recordId == null) { vm.setPlate(plate); return@LaunchedEffect }
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getInstance(ctx)
            val repo = VehicleRepository(db.vehicleDao(), db.maintenanceRecordDao(), db.partDao(), db.reminderRuleDao())
            repo.getAllRecords().collect { records ->
                records.find { it.id == recordId }?.let { vm.loadForEdit(it); return@collect }
            }
        }
    }
    LaunchedEffect(saved) { if (saved) { vm.resetSaveFlag(); onBack() } }

    var plateDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (isEdit) "编辑记录" else "添加记录") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } })
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // 车牌号下拉选择
            ExposedDropdownMenuBox(expanded = plateDropdownExpanded, onExpandedChange = { plateDropdownExpanded = it }) {
                OutlinedTextField(
                    value = selectedPlate, onValueChange = {},
                    readOnly = true, label = { Text("车牌号") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = plateDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = plateDropdownExpanded, onDismissRequest = { plateDropdownExpanded = false }) {
                    allVehicles.forEach { v ->
                        DropdownMenuItem(text = { Text(v.plateNumber) }, onClick = { vm.setPlate(v.plateNumber); plateDropdownExpanded = false })
                    }
                }
            }

            // 类型选择
            Text("记录类型", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type, onClick = { vm.setType(type) },
                        label = { Text(when(type) { RecordType.MAINTENANCE -> "保养"; RecordType.REPAIR -> "维修"; RecordType.INSPECTION -> "年检"; RecordType.INSURANCE -> "保险" }) }
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(mileage, { vm.setMileage(it) }, label = { Text("里程(km)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(cost, { vm.setCost(it) }, label = { Text("费用(元)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            }

            OutlinedTextField(desc, { vm.setDescription(it) }, label = { Text("项目描述") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(shop, { vm.setShopName(it) }, label = { Text("维修厂") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(location, { vm.setLocation(it) }, label = { Text("地点") }, modifier = Modifier.weight(1f), singleLine = true)
            }

            Button(onClick = { vm.saveRecord() }, modifier = Modifier.fillMaxWidth()) {
                Text("💾 保存记录")
            }
        }
    }
}
