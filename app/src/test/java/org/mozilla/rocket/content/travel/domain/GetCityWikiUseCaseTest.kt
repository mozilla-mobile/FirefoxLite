package org.mozilla.rocket.content.travel.domain

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class GetCityWikiUseCaseTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var testString: String
    private lateinit var expectedResult: String

    @Test
    fun `Without parenthesis`() {

        testString = "Taipei ; Mandarin: [t\\u02b0\\u01ceip\\u00e8i]; Hokkien POJ: T\\u00e2i-pak, officially known as Taipei City, is the capital and a special municipality of Taiwan "
        expectedResult = "Taipei ; Mandarin: [t\\u02b0\\u01ceip\\u00e8i]; Hokkien POJ: T\\u00e2i-pak, officially known as Taipei City, is the capital and a special municipality of Taiwan "

        Assert.assertEquals(expectedResult, testString.trimContentWithinParentheses())
    }

    @Test
    fun `Single pair parentheses`() {

        testString = "Taipei (; Mandarin: [t\\u02b0\\u01ceip\\u00e8i]; Hokkien POJ: T\\u00e2i-pak), officially known as Taipei City, is the capital and a special municipality of Taiwan "
        expectedResult = "Taipei , officially known as Taipei City, is the capital and a special municipality of Taiwan "

        Assert.assertEquals(expectedResult, testString.trimContentWithinParentheses())
    }

    @Test
    fun `Double pairs parentheses`() {

        testString = "Taipei (; Mandarin: ([t\\u02b0\\u01ceip\\u00e8i];) Hokkien POJ: T\\u00e2i-pak), officially known as Taipei City, is the capital and a special municipality of Taiwan "
        expectedResult = "Taipei , officially known as Taipei City, is the capital and a special municipality of Taiwan "

        Assert.assertEquals(expectedResult, testString.trimContentWithinParentheses())
    }

    @Test
    fun `Triple pairs parentheses`() {

        testString = "Taipei (; Mandarin: ([t\\u0(2b0\\u)01ceip\\u00e8i];) Hokkien POJ: T\\u00e2i-pak), officially known as Taipei City, is the capital and a special municipality of Taiwan "
        expectedResult = "Taipei , officially known as Taipei City, is the capital and a special municipality of Taiwan "

        Assert.assertEquals(expectedResult, testString.trimContentWithinParentheses())
    }

    @Test
    fun `Multiple pairs parentheses`() {

        testString = "Taipei (; Mandarin: ([t\\u0(2b0\\u)01ceip\\u00e8i];) Hokkien (POJ:) T\\u00e2i-pak), officially known as Taipei City, is the capital and a special municipality of Taiwan "
        expectedResult = "Taipei , officially known as Taipei City, is the capital and a special municipality of Taiwan "

        Assert.assertEquals(expectedResult, testString.trimContentWithinParentheses())
    }

    @Test
    fun `Multiple single pair parentheses`() {

        testString = "(Taipei ;) Mandarin: ([t\\u02b0\\u01ceip\\u00e8i];) Hokkien POJ: T\\u00e2i-pak, officially known as Taipei City, is the capital and a (special municipality of Taiwan) "
        expectedResult = " Mandarin:  Hokkien POJ: T\\u00e2i-pak, officially known as Taipei City, is the capital and a  "

        Assert.assertEquals(expectedResult, testString.trimContentWithinParentheses())
    }

    @Test
    fun `Unpaired parentheses`() {

        testString = "Taipei (; Mandarin: [t\\u02b0\\u01ceip\\u00e8i]; Hokkien POJ: T\\u00e2i-pak, officially known as Taipei City, is the capital and a special municipality of Taiwan "
        expectedResult = "Taipei "

        Assert.assertEquals(expectedResult, testString.trimContentWithinParentheses())
    }

    @Test
    fun `Multiple unpaired parentheses`() {

        testString = "Taipei (; Mandarin: [t\\u02b0\\u01ceip\\u00e8i]; (Hokkien POJ: T\\u00e2i-pak, )officially known as Taipei City, is the capital and a special municipality of Taiwan "
        expectedResult = "Taipei "

        Assert.assertEquals(expectedResult, testString.trimContentWithinParentheses())
    }
}