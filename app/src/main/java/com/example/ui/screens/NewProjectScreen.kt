package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(
    onNavigateBack: () -> Unit,
    onCreateProject: (name: String, building: String, address: String, description: String, floorCount: Int, mode: String) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var buildingName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var floorCountText by remember { mutableStateOf("3") }
    var description by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("INDOOR_OUTDOOR") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("새 프로젝트", color = TextPrimary, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text("프로젝트 이름 *") },
                placeholder = { Text("예: 학교 본관 정밀 스캔") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            OutlinedTextField(
                value = buildingName,
                onValueChange = { buildingName = it },
                label = { Text("건물 이름 *") },
                placeholder = { Text("예: 과학고 본관") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("주소") },
                placeholder = { Text("예: 서울특별시 서초구 반포대로 12") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            OutlinedTextField(
                value = floorCountText,
                onValueChange = { floorCountText = it.filter { char -> char.isDigit() } },
                label = { Text("층수") },
                placeholder = { Text("예: 3") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("설명") },
                placeholder = { Text("공간 스캔 목적 및 특이사항") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Text("스캔 방식", color = TextPrimary, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("INDOOR" to "실내", "OUTDOOR" to "실외", "INDOOR_OUTDOOR" to "실내 + 실외").forEach { (mode, label) ->
                    val isSelected = selectedMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMode = mode },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val floors = floorCountText.toIntOrNull()?.coerceIn(1, 100) ?: 1
                    onCreateProject(
                        projectName.ifBlank { "새 공간 프로젝트" },
                        buildingName.ifBlank { "지정되지 않은 건물" },
                        address,
                        description,
                        floors,
                        selectedMode
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("프로젝트 생성", color = DarkBackground, fontSize = 16.sp)
            }
        }
    }
}
