package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Project
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    projects: List<Project>,
    onSelectProject: (Project) -> Unit,
    onNavigateToNewProject: () -> Unit,
    onNavigateToScan: (Project) -> Unit,
    onNavigateTo3D: (Project) -> Unit,
    onLaunchDemo: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SpaceScan AI", color = TextPrimary, fontSize = 20.sp)
                        Text("공간을 스캔하고 실제 3D 건물을 만들어보세요.", color = TextSecondary, fontSize = 12.sp)
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = onLaunchDemo,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusYellow)
                    ) {
                        Text("DEMO 체험", fontSize = 11.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Big New Project Action Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToNewProject() },
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(PrimaryBlue.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("+ 새 프로젝트 만들기", color = TextPrimary, fontSize = 17.sp)
                            Text("실제 스마트폰 카메라로 새 공간을 스캔합니다", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Recent Projects Header
            item {
                Text(
                    "최근 프로젝트 (${projects.size})",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (projects.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("아직 프로젝트가 없습니다.", color = TextPrimary, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("새 프로젝트를 생성하여 공간 3D 스캔을 시작하세요.", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToNewProject,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Text("새 프로젝트 만들기", color = DarkBackground)
                            }
                        }
                    }
                }
            } else {
                items(projects) { project ->
                    ProjectCard(
                        project = project,
                        onCardClick = { onSelectProject(project) },
                        onScanClick = { onNavigateToScan(project) },
                        on3DClick = { onNavigateTo3D(project) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onCardClick: () -> Unit,
    onScanClick: () -> Unit,
    on3DClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(project.name, color = TextPrimary, fontSize = 16.sp)
                        if (project.isDemo) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = StatusYellow) {
                                Text("DEMO", color = DarkBackground, fontSize = 9.sp)
                            }
                        }
                    }
                    Text(
                        "${project.buildingName} · ${project.floorCount}개 층",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Badge(containerColor = PrimaryBlue) {
                    Text("스캔 ${project.scanProgress}%", color = DarkBackground, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-floor progress indicators
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (f in 1..project.floorCount.coerceAtMost(3)) {
                    val floorName = "${f}F"
                    val floorProg = (project.scanProgress - (f - 1) * 20).coerceIn(0, 100)
                    FloorProgressBar(floorName = floorName, progress = floorProg)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = on3DClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("3D 보기", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onScanClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("계속 스캔", color = DarkBackground, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun FloorProgressBar(floorName: String, progress: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(floorName, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp))
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
            color = if (progress >= 80) StatusGreen else if (progress > 30) PrimaryBlue else StatusYellow,
            trackColor = DarkSurfaceBorder,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("$progress%", color = TextMuted, fontSize = 10.sp, modifier = Modifier.width(32.dp))
    }
}
