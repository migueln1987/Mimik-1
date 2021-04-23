package mimik.mockHelpers

import mimik.helpers.attractors.RequestAttractors
import okhttp3.RequestData

/**
 * Creates [RequestAttractors] from this [RequestData]
 */
val RequestData.toAttractors: RequestAttractors
    get() = RequestAttractors(this)
