# Mimik

Mimik is a API mocking server build on Ktor (written in Kotlin).
The purpose of this server is to return pre-recorded responses (tapes) or mock requests (using the Mimik Mock PUT call).

## Key features
- Requests to localhost are re-directed to a pre-configured external address
- New network requests are recorded and played back (as cached responses)
- Using Mimik mock, limited use responses are used to override mimik tape recordings
- Using tapes/ mocks, network calls can be called without online access


## How to setup - mobile
### iOS
  1. Change `domainProtocol` and `domainName` variables in corresponding
    .xcconfig file (e.g. **DemoConfig**, **DevConfig** or **QAConfig**) to be
    `http` and `localhost:4321` accordingly (if mime is run locally on default
    port)

  2. Add next Property List item to applications' [Info.plist](https://cardservices-git-dev.onefiserv.net/cardvalet/ios/blob/e-22044-cfc/CardValet/CardValet/Application/Info.plist)
    right under first <dict> open tag (to open plist in xml presentation right
    click on the file in XCode -> Open As -> Source Code) to allow application
    making http (not secured) calls to localhost:4321 domain:

  ```xml
  <key>NSAppTransportSecurity</key>
  <dict>
      <key>NSAllowsArbitraryLoads</key>
      <true/>
      <key>NSExceptionDomains</key>
      <dict>
          <key>localhost:4321</key>
          <dict>
              <key>NSExceptionAllowsInsecureHTTPLoads</key>
              <true/>
              <key>NSIncludesSubdomains</key>
              <true/>
          </dict>
      </dict>
  </dict>
  ```

### Android
  Switch your `Build Variants` to "mockDemo-x86" or a variant starting with "mock"
  
  Until the PR is approved, in `app/build.gradle`, the mock urls need to be updated to `... 10.0.2.2:4321 ...`
  instead of the current `... 10.0.2.2:3000 ...`

## How to setup - Mimik server
### Starting
- `./gradlew run`

### Using tapes (pre-recorded files or creating new)
Pre-recorded tapes are saved in the root `Tapes` directory.
Configuration is set in `resources/okreplay.properties` as `okreplay.tapeRoot`

## Using mock mimic recordings
The mimic server includes a live interaction to force temporary mock response for given api requests.

Current configuration includes:

 - Fiserv mock url
 http://0.0.0.0:4321/fiserver/mock?opId={opID name}

 - Header configuration
 
  | Header           | Usage | Data Type | Default | Range                         |
  |------------------|-------|-----------|---------|-------------------------------|
  | mockMethod       |  O    | String    | PUT     | any HTTP method               |
  | mockResponseCode |  O    | Int       | 200     | standard http response codes  |
  | mockUse          |  O    | Any       |         | any length of string is valid |
  | mockUses         |  O    | Int/ Any  |         | -999 to 999 or any            |

 - Response body (string, json, int, Any)

#### Descriptions
mockMethod:
The expected method which mimik will be looking for.
> Examples: GET PUT POST PATCH DELETE

mockResponseCode: 
Response code which the mimik mock will return. 
The requested response code will be attempted to be parsed into a default HTTP response code, 
defaulting to 200 when unable to.

mockUse:
When a value is set (any string/ int/ bool), the mimik mock will interpret the request 
as "enable this response for a 1-time use".

mockUses:
Positive values will set the mock usage variable, with each request to that endpoint decrementing the mock usage count.
Setting a negative value will decrement the usage counter to a maximum of 0.
A non-integer value (or a value of 0) will reset the mock counter to 0.

### Example
```shell script
curl -X PUT \
  'http://0.0.0.0:4321/fiserver/mock?opId=CFC_ELIGIBILITY_AND_GET_TOKEN&apiVersion=v2.0' \
  -H 'MockUse: true' \
  -H 'content-length: 512' \
  -H 'mockMethod: POST' \
  -H 'mockResponseCode: 200' \
  -d '{
    "apiVersion": "v2.0",
    "opId": "CFC_ELIGIBILITY_AND_GET_TOKEN",
    "responseStatus": {
        "message": "Success",
        "responseCode": 0,
        "operationTraceId": "27f316faondotcc6089optrc4705ccapiad51abbba7f09cb8-7-5-5",
        "additionalInfo": {
            "amount": 0,
            "FI_NAME": "Fiserv",
            "accessCode": "",
            "maxAmount": 200,
            "maxLifeSpan": 900,
            "isUserEnrolled": false,
            "FI_CONTACT": "8658769876"
        }
    }
}'
```

## View tapes/ recordings
`In Progress`

## Edit tapes/ recordings
`In Progress`
