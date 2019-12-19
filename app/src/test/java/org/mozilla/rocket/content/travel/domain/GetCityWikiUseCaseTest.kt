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

    @Test
    fun `Fullwidth paired parentheses`() {

        testString = "沖繩縣（日語：沖縄県／おきなわけん〔おきなはけん〕 Okinawa ken */?，琉球語：沖縄／ウチナー Ucinaa）是日本最西南側的一個縣，縣廳所在地是那霸市。沖繩縣由琉球群島中的沖繩群島、先島群島以及太平洋中的大東群島組成，隔海和九州的鹿兒島縣相鄰。全縣由160個島嶼組成，其中49個有人居住，面積約2,281平方公里，是日本陸地面積第四小的縣。但沖繩縣包括了廣大的海域面積，東西寬約1,000公里，南北長約400公里，算上海域面積的沖繩縣面積則相當於本州、四國、九州面積總和的一半。沖繩縣全境屬於亞熱帶氣候，全年氣候溫暖且降水充沛。"
        expectedResult = "沖繩縣是日本最西南側的一個縣，縣廳所在地是那霸市。沖繩縣由琉球群島中的沖繩群島、先島群島以及太平洋中的大東群島組成，隔海和九州的鹿兒島縣相鄰。全縣由160個島嶼組成，其中49個有人居住，面積約2,281平方公里，是日本陸地面積第四小的縣。但沖繩縣包括了廣大的海域面積，東西寬約1,000公里，南北長約400公里，算上海域面積的沖繩縣面積則相當於本州、四國、九州面積總和的一半。沖繩縣全境屬於亞熱帶氣候，全年氣候溫暖且降水充沛。"

        Assert.assertEquals(expectedResult, testString.trimContentWithinParentheses())
    }

    @Test
    fun `Fullwidth unpaired parentheses`() {

        testString = "沖繩縣（日語：沖縄県／おきなわけん〔おきなはけん〕 Okinawa ken */?，琉球語：沖縄／ウチナー Ucinaa 是日本最西南側的一個縣，縣廳所在地是那霸市。沖繩縣由琉球群島中的沖繩群島、先島群島以及太平洋中的大東群島組成，隔海和九州的鹿兒島縣相鄰。全縣由160個島嶼組成，其中49個有人居住，面積約2,281平方公里，是日本陸地面積第四小的縣。但沖繩縣包括了廣大的海域面積，東西寬約1,000公里，南北長約400公里，算上海域面積的沖繩縣面積則相當於本州、四國、九州面積總和的一半。沖繩縣全境屬於亞熱帶氣候，全年氣候溫暖且降水充沛。"
        expectedResult = "沖繩縣"

        Assert.assertEquals(expectedResult, testString.trimContentWithinParentheses())
    }

    @Test
    fun `Hybrid paired parentheses`() {

        testString = "沖繩縣（日語：沖縄県／おきなわけん〔おきなはけん〕 Okinawa ken */?，琉球語：沖縄／ウチナー Ucinaa）是日本最西南側的一個縣(，縣廳所在地是那霸市)。沖繩縣由琉球群島中的沖繩群島、先島群島以及太平洋中的大東群島組成，隔海和九州的鹿兒島縣相鄰。全縣由160個島嶼組成，其中49個有人居住，面積約2,281平方公里，是日本陸地面積第四小的縣。但沖繩縣包括了廣大的海域面積，東西寬約1,000公里，南北長約400公里，算上海域面積的沖繩縣面積則相當於本州、四國、九州面積總和的一半。沖繩縣全境屬於亞熱帶氣候，全年氣候溫暖且降水充沛。"
        expectedResult = "沖繩縣是日本最西南側的一個縣。沖繩縣由琉球群島中的沖繩群島、先島群島以及太平洋中的大東群島組成，隔海和九州的鹿兒島縣相鄰。全縣由160個島嶼組成，其中49個有人居住，面積約2,281平方公里，是日本陸地面積第四小的縣。但沖繩縣包括了廣大的海域面積，東西寬約1,000公里，南北長約400公里，算上海域面積的沖繩縣面積則相當於本州、四國、九州面積總和的一半。沖繩縣全境屬於亞熱帶氣候，全年氣候溫暖且降水充沛。"

        Assert.assertEquals(expectedResult, testString.trimContentWithinParentheses())
    }

    @Test
    fun `Mixed paired parentheses`() {

        testString = "沖繩縣（日語：沖縄県／おきなわけん〔おきなはけん〕 Okinawa ken */?，琉球語：沖縄／ウチナー Ucinaa)是日本最西南側的一個縣(，縣廳所在地是那霸市)。沖繩縣由琉球群島中的沖繩群島、先島群島以及太平洋中的大東群島組成，隔海和九州的鹿兒島縣相鄰。全縣由160個島嶼組成，其中49個有人居住，面積約2,281平方公里，是日本陸地面積第四小的縣。但沖繩縣包括了廣大的海域面積，東西寬約1,000公里，南北長約400公里，算上海域面積的沖繩縣面積則相當於本州、四國、九州面積總和的一半。沖繩縣全境屬於亞熱帶氣候，全年氣候溫暖且降水充沛。"
        expectedResult = "沖繩縣是日本最西南側的一個縣。沖繩縣由琉球群島中的沖繩群島、先島群島以及太平洋中的大東群島組成，隔海和九州的鹿兒島縣相鄰。全縣由160個島嶼組成，其中49個有人居住，面積約2,281平方公里，是日本陸地面積第四小的縣。但沖繩縣包括了廣大的海域面積，東西寬約1,000公里，南北長約400公里，算上海域面積的沖繩縣面積則相當於本州、四國、九州面積總和的一半。沖繩縣全境屬於亞熱帶氣候，全年氣候溫暖且降水充沛。"

        Assert.assertEquals(expectedResult, testString.trimContentWithinParentheses())
    }
}