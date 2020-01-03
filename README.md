# Mimik

Mimik is a API mocking server build on Ktor (written in Kotlin).
The purpose of this server is to
- return pre-recorded responses (chapters via tapes)
- create a testing environment to ensure expected calls are called
- enable offline (no internet access) device testing (except for "live response" chapters)

## Key features
- Requests to localhost (`port 2202`) are handled by mimik to return pre-configured responses as as live server would
  - Offline access
- New network requests are filtered to tapes based on tape attractors to use similar configs as other like requests
  - Seamless integration with new API calls
- Using Mimik mock, limited use responses are used to override mimik tape recordings
  - Temporary responses to re-create server state changes
  - Note: can also be done via test bounds with ~~header/~~ body replacements
- And more within -> [Feature list](Docs/features.md)

## How to setup - mobile
### iOS
  1. Change `domainProtocol` and `domainName` variables in corresponding
    .xcconfig file (e.g. **DemoConfig**, **DevConfig** or **QAConfig**) to be
    `http` and `localhost:2202` accordingly (if mimik is run locally on default
    port)

  2. Add next Property List item to applications' [Info.plist](https://cardservices-git-dev.onefiserv.net/cardvalet/ios/blob/develop/CardValet/CardValet/Application/Info.plist)
    right under first <dict> open tag (to open plist in xml presentation right
    click on the file in XCode -> Open As -> Source Code) to allow application
    making http (not secured) calls to localhost:2202 domain:

  ```xml
  <key>NSAppTransportSecurity</key>
  <dict>
      <key>NSAllowsArbitraryLoads</key>
      <true/>
      <key>NSExceptionDomains</key>
      <dict>
          <key>localhost:2202</key>
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
  
  **NOTE**<br>
  Until the PR is approved, in `app/build.gradle`, the mock urls need to be updated to `... 10.0.2.2:2202 ...`
  instead of the current `... 10.0.2.2:3000 ...`

## How to setup - Mimik server
### Starting
- `./gradlew run`

### Creating Tapes/ Interactions
- [Via HTTP page](Docs/mimikHttp.md)
- [Via Mock API](Docs/mimikMockAPI.md)

### Tapes (location)
Mimik accepts any json interactions under the `Tapes` directory (and sub-directories).

## Using mock mimic recordings
If the mimik server is able to find which tape to use (via tape/ mock attractor values), 
then the server will automatically process the request and return a response.

If no matching tape/ mock is found, a new tape will be created and the call is added to that tape.<br>
>Note: The new interaction will be an `await` type. Adding a response manually or a routing url to 
the host tape (and using the call again) will enable the interaction as fulfilled.

