package com.brunoafk.calendardnd.data.calendar

import com.brunoafk.calendardnd.domain.model.KeywordMatchMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarRepositoryTest {

    @Test
    fun `keywords match any keyword case-insensitively`() {
        val title = "Weekly Sync with Team"
        val pattern = "sync, meeting"

        val matches = matchesTitleKeyword(
            title = title,
            requireTitleKeyword = true,
            titleKeyword = pattern,
            matchMode = KeywordMatchMode.KEYWORDS,
            caseSensitive = false,
            matchAll = false,
            excludeMatches = false
        )

        assertTrue(matches)
    }

    @Test
    fun `keywords fail when no match`() {
        val matches = matchesTitleKeyword(
            title = "Lunch Break",
            requireTitleKeyword = true,
            titleKeyword = "sync, planning",
            matchMode = KeywordMatchMode.KEYWORDS,
            caseSensitive = false,
            matchAll = false,
            excludeMatches = false
        )

        assertFalse(matches)
    }

    @Test
    fun `regex matches when pattern is valid`() {
        val matches = matchesTitleKeyword(
            title = "Project Kickoff 2025",
            requireTitleKeyword = true,
            titleKeyword = "Kickoff\\s\\d{4}",
            matchMode = KeywordMatchMode.REGEX,
            caseSensitive = false,
            matchAll = false,
            excludeMatches = false
        )

        assertTrue(matches)
    }

    @Test
    fun `regex fails when pattern is invalid`() {
        val matches = matchesTitleKeyword(
            title = "Project Kickoff",
            requireTitleKeyword = true,
            titleKeyword = "Kickoff(",
            matchMode = KeywordMatchMode.REGEX,
            caseSensitive = false,
            matchAll = false,
            excludeMatches = false
        )

        assertFalse(matches)
    }

    @Test
    fun `blank keyword requires no match`() {
        val matches = matchesTitleKeyword(
            title = "Project Kickoff",
            requireTitleKeyword = true,
            titleKeyword = "   ",
            matchMode = KeywordMatchMode.KEYWORDS,
            caseSensitive = false,
            matchAll = false,
            excludeMatches = false
        )

        assertFalse(matches)
    }

    @Test
    fun `keyword not required always matches`() {
        val matches = matchesTitleKeyword(
            title = "Anything",
            requireTitleKeyword = false,
            titleKeyword = "",
            matchMode = KeywordMatchMode.KEYWORDS,
            caseSensitive = false,
            matchAll = false,
            excludeMatches = false
        )

        assertTrue(matches)
    }
}
