package com.longdu.vehicle.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.longdu.vehicle.data.entity.Part
import com.longdu.vehicle.data.entity.PartCategory
import com.longdu.vehicle.viewmodel.VehicleDetailViewModel

/**
 * 添加配件页面 — 配件独立管理，默认关联"通用"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPartScreen(plate: String, onBack: () -> Unit) {
    val vm: VehicleDetailViewModel = viewModel()

    var partName by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(PartCategory.OTHER) }
    var price by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var catExpanded by remember { mutableStateOf(false) }
    var remark by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("添加配件") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } })
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(partName, { partName = it }, label = { Text("配件名称 *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(brand, { brand = it }, label = { Text("品牌") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(supplier, { supplier = it }, label = { Text("供应商") }, modifier = Modifier.weight(1f), singleLine = true)
            }

            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                OutlinedTextField(
                    value = category.name, onValueChange = {}, readOnly = true,
                    label = { Text("分类") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    PartCategory.entries.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat.name) }, onClick = { category = cat; catExpanded = false })
                    }
                }
            }

            OutlinedTextField(price, { price = it }, label = { Text("价格(元)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(remark, { remark = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            Button(onClick = {
                if (partName.isBlank()) return@Button
                vm.addPart(Part(
                    plateNumber = plate.ifBlank { "通用" }, partName = partName, brand = brand,
                    category = category, price = price.toDoubleOrNull() ?: 0.0, supplier = supplier, remark = remark
                ))
                onBack()
            }, modifier = Modifier.fillMaxWidth()) {
                Text("➕ 添加配件")
            }
        }
    }
}
