package unitTests.helperTests

import mimik.helpers.attractors.RequestAttractorBit
import mimik.helpers.attractors.RequestAttractors
import mimik.helpers.attractors.getMatches
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

        val matchBitA = listOf(bitA)
        val responseA = matchBitA.getMatches(input)

        val matchBitB = listOf(bitB)
        val responseB = matchBitB.getMatches(input)

        Assert.assertEquals(responseA.Required, responseB.Required)
        Assert.assertEquals(responseA.MatchesReq, responseB.MatchesReq)
        Assert.assertTrue(responseA.reqLiterals > responseB.reqLiterals)

        Assert.assertEquals(9, responseA.reqLiterals)
        Assert.assertEquals(5, responseB.reqLiterals)
    }

    @Test
    fun matchCountOptRatio() {
        // Add a dummy "Req", as "optionals" are skipped if "Req" fails
        val dummyReq = RequestAttractorBit(".*")
        val bitA = RequestAttractorBit("matchevery.*") { it.optional = true }
        val bitB = RequestAttractorBit("match.*") { it.optional = true }
        val input = "matcheverything"

        val matchBitA = listOf(dummyReq, bitA)
        val responseA = matchBitA.getMatches(input)

        val matchBitB = listOf(dummyReq, bitB)
        val responseB = matchBitB.getMatches(input)

        Assert.assertEquals(responseA.MatchesOpt, responseB.MatchesOpt)
        Assert.assertTrue(responseA.optLiterals > responseB.optLiterals)

        Assert.assertEquals(9, responseA.optLiterals)
        Assert.assertEquals(5, responseB.optLiterals)
    }
}
