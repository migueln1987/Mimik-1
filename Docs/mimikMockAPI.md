# Mock API
The mimik server allows the developer to call [PUT](#put) or [PATCH](#patch) to set mock data for passthrough data to use.<br>
Mock data includes:
- [Headers](#params)
- [Body](#body)

## URL
http://0.0.0.0:4321/mock

## Params (Header key/ values)
All mock parameters must prefix with "mock".
Each sub-category will prepend it's prefix unless no prefix is mentioned.

Parameters are not case sensitive, but formatted as such for readability.

Examples:
> Tape's 'AllowRecordings' equals 'true' -> `mockTape_AllowRecordings : true`
>
> Http request 'HeaderIn_{Key}' of 'Session' equals '123' -> `mockHeaderIn_Session : 123`

### General [Tapes and Recordings]
| Param | Type    | Default | Action |
|-------|---------|---------|--------|
| Live  | Boolean | `false` | When set as `true`, the request will always return a live response. |

### Filtering (prefix: "Filter_") [Tapes and Recordings]
Filtering is used for both Tape and Mock interactions.
Tapes; filter new (unknown) calls into the tape.<br>
Mocks; filter calls to known mocks.<br>

Note: Including filtering data in a mock request, which will use an existing tape, 
will apply the filter only to the mock plus append the tape's filters.
<br>

| Param | Type  | Default | Action |
|-------|-------|---------|--------|
| Path  | Regex | `null`  | Sub-path of the url<br> Example: `/route/sub/path/` |
| Query<sup>1</sup> | Regex<sup>2</sup> | `null`| Queries(s) in the url<br> Example: `Key1=Value1&Key2=Value2` |
| ~~Header~~<sup>1,3</sup> | Regex | `null` | Headers in the request<br>Example: `HeaderKey : Value`|
| Body<sup>1</sup> | Regex | `null`/ `.*`<sup>4</sup> | Text to search for within the message body<br> Example: `countryCode.{0,8}US` |

1. Postfix options
   - `~` will mark the filter as `optional`. Otherwise, it is `required`.
   - `!` will mark the filter as `avoid`; the filter must not match to be true.
   - [Sequence Diagram](actionCharts.md#requestattractors-mockfilter_filter)
     - `Ratio` is defined by "how much of the literal regex matches"
     - `avoid` ratio will always equal `1` if it matches
2. Values in the form of `Key1=Value1&Key2=Value2` will be split into `Key1=Value1` and `Key2=Value2`<br>
3. Will be enabled in a future update
4. If no filter is mentioned, then `.*` (accept all body text) is added automatically to mocks whose method always has a body.
   - Methods with bodies: `POST`, `PUT`, `PATCH`, `PROPATCH`, and `REPORT`

### Tape (prefix: "Tape_")
| Param            | Type    | Default | Action |
|------------------|---------|---------|--------|
| Name<sup>1</sup> | String  | Tape hash code (Int) | When looking for which tape to use, this is used as a check. <br>Otherwise, the newly created tape will use this value.  |
| URL<sup>2</sup>  | String  | Empty string    | Setting this value will allow calls directed through this tape, to access live data. |
| AllowRecordings  | Boolean | `true`  | The tape will disable new recordings (live calls) from being added. |
| Save<sup>3</sup> | Boolean | `false` |Setting this value will ensure the tape is saved to a physical file. |
| Only             | Boolean | `false` | The call will go as far as to create the tape, then finish. |

1: Name to define this tape for furture uses<br>
  - Currently only supports direct string names, no sub-directory ("something/dir/filename") names.<br>
  - Name is case-insensitive. but the case will be applied to the new filename.<br>
  
2: Only needed for new calls going through the tape.<br>
3: All other (hard) recordings on this tape will also be saved to file too.

### Recordings
#### General
| Param            | Type        | Default | Action |
|------------------|-------------|--------|--------|
| Name<sup>1</sup> | String | Empty string |Assigning a name allows re-using/ writing to the same mock. | 
| Use          | String/ Int<sup>2</sup> | `always`| Sets the request's expiration usage |
| ReadOnly     | Boolean | - |True: "Use" wil be set to "always" (unless `use=disabled`) |
| Await | Boolean | - | Mock will not include any response data, but will instead be populated on first use (through a live call) |

1: Not assigning a name will create a new mock each time, regardless if the data is the same.

2:
- String values (no expiration):
  - "always" = This request will always be used when possible
  - "disable" = The request exists, but suspended
- Int (limited mock):
  - 0: Interaction has exhausted it's uses and acts as "disabled"
  - ( > 0): Uses until the interaction is disabled

#### HTTP Request
##### == HTTP data ==
| Param        | Type        | Default | Action |
|--------------|-------------|---------|--------|
| Method<sup>1</sup>       | String | `GET` | The type of call to listen for.<br> Example: GET, POST, DELETE, etc. |
| ~~HeaderIn_{Key}~~<sup>1,2</sup> | Any | `Content-Type : text/plain` | Attractor headers |

1. Will be deprecated here, and moved to `Filter` in a future story
2. Replace {Key} to be the expected header to look for.

#### HTTP Response
##### == Status items ==
| Param                       | Type   | Default | Action |
|-----------------------------|--------|---------|--------|
| ResponseCode                | Int    | `200` | Code to return as the HTTP Response status |
| HeaderOut_{Key}<sup>1</sup> | String | `Content-Type : text/plain` |Response headers |

1: Replace {Key} with the returning header key.

## Body [Recordings]
Body data is used based on the included HTTP Method. 
Refer to [HTTP Methods](#http-methods) for details.

## HTTP Methods
### PUT
When creating a mock, the entire body is used as the response body
- [Sequence Diagram](actionCharts.md#put)
- [Creating a tape](examples.md#basic_createtape)
- [Creating a tape and mock in the same call](examples.md#basic_apply)
- [Creating a mock, being applied to a tape](examples.md#basic_retrieve)

### PATCH
- [Sequence Diagram](actionCharts.md#patch)
- ~~Update a tape~~
- ~~Update a mock~~

## HTTP Mock Responses
- Created (201)
- Found (302)
- InternalServerError (500)
