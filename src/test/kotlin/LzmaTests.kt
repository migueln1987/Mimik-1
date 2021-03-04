import mimik.helpers.lzma.LZMA_Decode
import mimik.helpers.lzma.LZMA_Encode
import org.junit.Assert
import org.junit.Test

class LzmaTests {
    // todo; convert from a string, to a class converted to a json string
    val input = """
            {
              "ID": 278286289,
              "Name": 1694132605,
              "Commands": [{
                "isValid": true,

                "isCond": true,
                "isOpt": false,
                "condSrc": 0,

                "srcType": 1,
                "isHead": true,
                "isBody": false,
                "varLevel": 1,
                "varSearchUp": false,

                "source_hasItems": false,
                "source_name": "null",
                "source_match": "null",

                "hasAction": true,
                "act_name": "PiI2Yjlc",
                "act_nExists": false,
                "act_nCount": true,
                "act_nResult": true,
                "act_nSpread": true,
                "act_nSpreadType": 11,
                "act_scopeLevel": 0,
                "act_match": "qpZPhate5"
              }, {
                "isValid": true,

                "isCond": true,
                "isOpt": false,
                "condSrc": 1,

                "srcType": 1,
                "isHead": false,
                "isBody": true,
                "varLevel": 0,
                "varSearchUp": false,

                "source_hasItems": true,
                "source_name": "null",
                "source_match": "zWpxBBSW",

                "hasAction": true,
                "act_name": "AHvkLRq",
                "act_nExists": true,
                "act_nCount": true,
                "act_nResult": false,
                "act_nSpread": false,
                "act_nSpreadType": 18,
                "act_scopeLevel": -1,
                "act_match": "BVAcdmj"
              }, {
                "isValid": true,

                "isCond": false,
                "isOpt": false,
                "condSrc": -1,

                "srcType": 1,
                "isHead": true,
                "isBody": true,
                "varLevel": 1,
                "varSearchUp": true,

                "source_hasItems": true,
                "source_name": "BtWLn",
                "source_match": "NIyqHGo9",

                "hasAction": true,
                "act_name": "UbvzW",
                "act_nExists": true,
                "act_nCount": true,
                "act_nResult": true,
                "act_nSpread": true,
                "act_nSpreadType": -8,
                "act_scopeLevel": 1,
                "act_match": "null"
              }]
            }
        """.trimIndent().replace("\n", "")

    @Test
    fun test2() {
        val output_enc = LZMA_Encode { input }
        val result = LZMA_Decode(output_enc)
        val resultStr = String(result)
        Assert.assertEquals(input, resultStr)
    }
}
