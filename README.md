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
  
  **NOTE**<br>
  Until the PR is approved, in `app/build.gradle`, the mock urls need to be updated to `... 10.0.2.2:4321 ...`
  instead of the current `... 10.0.2.2:3000 ...`

## How to setup - Mimik server
### Starting
- `./gradlew run`

### Creating Tapes/ Interactions
- [Via HTTP page](Docs/mimikHttp.md)
- [Via Mock API](Docs/mimikMockAPI.md)

### Tapes (location)
Mimik accepts any json interactions under the `Tapes` directory.
New tapes are saved to `Tapes/NewTapes`.<br>

## Using mock mimic recordings
If the mimik server is able to find which tape to use (via tape/ chapter attractor values), 
then the server will automatically process the request and return a response.

