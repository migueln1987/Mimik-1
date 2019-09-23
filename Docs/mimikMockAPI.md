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

### General (Tapes and Recordings)
#### Filtering (prefix: "Filter_")
Filtering is used for both Tape and Mock interactions.
Tapes; filter new (unknown) calls into the tape.<br>
Mocks; filter calls to known mocks.<br>

Note: Including filtering data in a mock request, which is going to an existing tape, 
will apply the filter only to the mock.
<br>

| Param | Type   | Action |
|-------|--------|--------|
| Path  | String | Sub-path of the url<br> Example: `/route/sub/path/` |
| Param<sup>1</sup> | Regex<sup>2</sup> | Parameter(s) in the url<br> Example: `Key1=Value1&Key2=Value2` |
| Body<sup>1</sup> | Regex | Text to search for within the message body<br> Example: `countryCode.{0,8}US` |

1: Appending a postfix of `~` will mark the filter as optional<br>
2: Values in the form of `Key1=Value1&Key2=Value2` will be split into `Key1=Value1` and `Key2=Value2`

### Tape (prefix: "Tape_")
| Param            | Type    | Default | Action |
|------------------|---------|---------|--------|
| Name<sup>1</sup> | String  | Random UUID | When looking for which tape to use, this is used as a check. <br>Otherwise, the newly created tape will use this value.  |
| Url<sup>2</sup>  | String  | Empty string    | Setting this value will allow calls directed through this tape, to access live data. |
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
| Param        | Use | Type        | Action |
|--------------|-----|-------------|--------|
| Use          | O  | String/ Int<sup>1</sup> | Sets the request's expiration usage |
| ReadOnly     | O  | Boolean | True: "Use" wil be set to "always" (unless `use=disabled`) |
| Name<sup>2</sup> | C  | String | Assigning a name allows re-using/ writing to the same mock. | 

1:
- String values (no expiration):
  - "always" = This request will always be used when possible
  - "disable" = The request exists, but suspended
- Int (memory-only mock):
  - 0: Interaction has exhausted it's uses and acts as "disabled"
  - ( > 0): Uses until the interaction is disabled

2: Not assigning a name will create a new mock each time, regardless if the data is the same.

#### HTTP Request
##### == Route (prefix: "Route_") ==
The following items are direct filters for calls to be matched against 

| Param | Type   | Action |
|-------|--------|--------|
| Path  | String | Sub-path of the url<br> Example: `/route/sub/path/` |
| Params | String | Parameters in the url<br> Example: `Key1=Value1&Key2=Value2` |

##### == HTTP data ==
| Param        | Type        | Action |
|--------------|-------------|--------|
| Method       | String | The type of call to listen for.<br>Default: `GET`<br> Example: GET, POST, DELETE, etc. |
| HeaderIn_{Key}<sup>1</sup> | Any | Attractor headers |

1: Replace {Key} to be the expected header to look for.

#### HTTP Response
##### == Status items ==
| Param                       | Type   | Action |
|-----------------------------|--------|--------|
| ResponseCode                | Int    | Code to return as the HTTP Response status <br>Default:`200` |
| HeaderOut_{Key}<sup>1</sup> | String | Response headers |

1: Replace {Key} with the returning header key.

## Body
Body data is used based on the included HTTP Method

## HTTP Methods
### PUT
When creating a mock, the entire body is used as the response body
- [Creating a tape](examples.md#basic_createtape)
- [Creating a tape and mock in the same call](examples.md#basic_apply)
- [Creating a mock, being applied to a tape](examples.md#basic_retrieve)

### PATCH
- Work in progress

## HTTP Mock Responses
- Created (201)
- Found (302)
- InternalServerError (500)
