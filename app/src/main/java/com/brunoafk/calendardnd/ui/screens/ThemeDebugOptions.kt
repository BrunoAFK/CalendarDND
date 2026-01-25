package com.brunoafk.calendardnd.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.ui.navigation.AppRoutes

data class ThemeDebugItem(
    val title: String,
    val subtitle: String,
    val route: String
)

@Composable
fun themeDebugItems(): List<ThemeDebugItem> = listOf(
    ThemeDebugItem(
        title = stringResource(R.string.debug_tools_main_view_default_title),
        subtitle = stringResource(R.string.debug_tools_main_view_default_subtitle),
        route = AppRoutes.STATUS
    ),
    ThemeDebugItem(
        title = stringResource(R.string.debug_tools_main_view_v2_title),
        subtitle = stringResource(R.string.debug_tools_main_view_v2_subtitle),
        route = AppRoutes.STATUS_V2
    ),
    ThemeDebugItem(
        title = stringResource(R.string.debug_tools_main_view_v3_title),
        subtitle = stringResource(R.string.debug_tools_main_view_v3_subtitle),
        route = AppRoutes.STATUS_V3
    ),
    ThemeDebugItem(
        title = stringResource(R.string.debug_tools_main_view_v4_title),
        subtitle = stringResource(R.string.debug_tools_main_view_v4_subtitle),
        route = AppRoutes.STATUS_V4
    ),
    ThemeDebugItem(
        title = stringResource(R.string.debug_tools_main_view_v5_title),
        subtitle = stringResource(R.string.debug_tools_main_view_v5_subtitle),
        route = AppRoutes.STATUS_V5
    )
)
