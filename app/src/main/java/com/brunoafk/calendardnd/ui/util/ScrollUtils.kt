package com.brunoafk.calendardnd.ui.util

import androidx.compose.foundation.lazy.LazyListState
suspend fun LazyListState.scrollToTopSmooth() {
    scrollToItem(0, 0)
}
