package unitTests.helperTests

import helpers.attractors.RequestAttractorBit
import helpers.attractors.RequestAttractors
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class RequestAttractorsTests {
    lateinit var textObject: RequestAttractors

    @Before
    fun setup() {
        textObject = RequestAttractors()
    }

    @Test
    fun matchCountReqRatio() {
        val bitA = RequestAttractorBit("matchevery.*")
        val bitB = RequestAttractorBit("match.*")
        val input = "matcheverything"
        val ratioA = bitA.hardValue.length / input.length.toDouble()
        val ratioB = bitB.hardValue.length / input.length.toDouble()

        val matchBitA = listOf(bitA)
        val responseA = textObject.getMatchCount(matchBitA, input)

        val matchBitB = listOf(bitB)
        val responseB = textObject.getMatchCount(matchBitB, input)

        Assert.assertEquals(responseA.Required, responseB.Required)
        Assert.assertEquals(responseA.MatchesReq, responseB.MatchesReq)
        Assert.assertTrue(responseA.reqRatio > responseB.reqRatio)

        Assert.assertEquals(responseA.reqRatio, ratioA, 0.1)
        Assert.assertEquals(responseB.reqRatio, ratioB, 0.1)
    }

    @Test
    fun matchCountOptRatio() {
        val dummyReq = RequestAttractorBit(".*")
        val bitA = RequestAttractorBit("matchevery.*") { it.optional = true }
        val bitB = RequestAttractorBit("match.*") { it.optional = true }
        val input = "matcheverything"
        val ratioA = bitA.hardValue.length / input.length.toDouble()
        val ratioB = bitB.hardValue.length / input.length.toDouble()

        val matchBitA = listOf(dummyReq, bitA)
        val responseA = textObject.getMatchCount(matchBitA, input)

        val matchBitB = listOf(dummyReq, bitB)
        val responseB = textObject.getMatchCount(matchBitB, input)

        Assert.assertEquals(responseA.MatchesOpt, responseB.MatchesOpt)
        Assert.assertTrue(responseA.optRatio > responseB.optRatio)

        Assert.assertEquals(responseA.optRatio, ratioA, 0.1)
        Assert.assertEquals(responseB.optRatio, ratioB, 0.1)
    }
}
