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
    val neuralBg = NeuralColors.Bg
    val neuralSurface = NeuralColors.Surface
    val neuralBorder = NeuralColors.Border
    val neuralPrimary = NeuralColors.Primary
    val neuralSecondary = NeuralColors.Secondary
    val inactive = NeuralColors.Muted

    val items = listOf(
        BottomItem.Home,
        BottomItem.Live,
        BottomItem.Missions,
        BottomItem.Reports
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        modifier = modifier,
        color = neuralBg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val itemColor = if (selected) neuralPrimary else inactive

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    color = if (selected) neuralSurface else Color.Transparent,
                    contentColor = itemColor,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) NeuralColors.BorderStrong else neuralBorder
                    ),
                    onClick = {
                        if (item.route == BottomItem.Live.route) {
                            onLiveFeedClick()
                        } else if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .align(Alignment.TopCenter)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color.Transparent, neuralPrimary, Color.Transparent)
                                        )
                                    )
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                                CompositionLocalProvider(LocalContentColor provides itemColor) {
                                    item.icon()
                                    Spacer(Modifier.height(2.dp))
                                    Text(item.label())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
