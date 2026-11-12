package com.sloth.registerapp.presentation.app.components

import com.sloth.registerapp.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sloth.registerapp.presentation.app.theme.NeuralColors

sealed class BottomItem(
    val route: String,
    val label: @Composable () -> String,
    val icon: @Composable () -> Unit
) {
    object Home : BottomItem("dashboard", { stringResource(R.string.bottom_nav_home) }, { Icon(Icons.Filled.Home, contentDescription = null) })
    object Live : BottomItem("camera", { stringResource(R.string.bottom_nav_live) }, { Icon(Icons.Filled.PlayCircle, contentDescription = null) })
    object Missions : BottomItem("mission", { stringResource(R.string.bottom_nav_missions) }, { Icon(Icons.Filled.List, contentDescription = null) })
    object Reports : BottomItem("report", { stringResource(R.string.bottom_nav_reports) }, { Icon(Icons.Filled.Assessment, contentDescription = null) })
}

@Composable
fun BottomNavBar(
    navController: NavController,
    onLiveFeedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(BottomItem.Home, BottomItem.Live, BottomItem.Missions, BottomItem.Reports)
    val stackEntry by navController.currentBackStackEntryAsState()
    val route = stackEntry?.destination?.route
    Surface(
        modifier = modifier,
        color = NeuralColors.Bg,
        border = BorderStroke(1.dp, NeuralColors.Border),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items.forEach { item ->
                val selected = route == item.route
                Surface(
                    modifier = Modifier.weight(1f).height(57.dp),
                    onClick = {
                        if (item.route == BottomItem.Live.route) {
                            onLiveFeedClick()
                        } else if (route != item.route) {
                            navController.navigate(item.route) {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    color = if (selected) NeuralColors.SurfaceAlt else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) NeuralColors.BorderStrong else Color.Transparent
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val tint = if (selected) Color.White else NeuralColors.Muted
                        CompositionLocalProvider(LocalContentColor provides tint) {
                            item.icon()
                            Spacer(Modifier.height(3.dp))
                            ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                                Text(
                                    text = item.label(),
                                    color = tint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
