#Features

1. Mimik
   1. Tape
      - Loading of tape data
        - Local directory
          > Note: Must be within the approved `{project}/Tapes` directory
        - ~~Local directory outside of the project~~
        - ~~HTTP (or git) url~~
      - Accept new calls via attractors "regex/literal based filters"
      - Allow pass-through calls via flag/s & attractors/s
   2. Mock (via Chapter)
      - Http Response
        - Response code: `200 (OK) `, `404 (Not Found)`, etc. 
        - Headers
        - Body
          - Images are saved as base-64 to preserve data
      - Attractors
        - Path: `/sub/path/`
        - Headers: `Key: value`
        - Parameter: `Key1=Value1&Key2=Value2`
        - Body: `{ "json": true }`
      - Uses: Use interactions (mock expires/ disables after a defined usage limit)
      - `Await`: response is created on demand (for initializing Mimik)
      - `Live`: requests which fit this attractor will re-direct to the live API call
      
2. Http Mimik Editor (CLI input @ `localhost:4321/{path}}`)
  - Mimik init (`localhost:4321/mimik`)
    - HTTP **PUT** commands using headers and body data
  - Test bounds (**POST** `localhost:4321/tests`)
    - Start (`localhost:4321/tests/start`)
      - Test name (for ~~logging~~/ ~~metrics~~)
      - Enabled duration
      - Enabled tapes
      - Response replacement
        - Per Response (name) type (Body or ~~Header~~)
          > Using regex/ literal match and ~~regex/~~ literal value replacement
    - Append (`localhost:4321/tests/append`)
      - Tapes (by name)
    - Disable (`localhost:4321/tests/disable`)
      - Tapes (by name)
    - Modify (`localhost:4321/tests/modify`)
      - Content replacement
      - ~~Chapter uses~~
    - Stop (`localhost:4321/tests/stop`)
      - Test by name
      - Finalize test
        > Free's up a device which is/ was bound to a test 
   
3. Http Mimik Editor (HTTP browser @ `localhost:4321/`)
   - ~~Import~~
     - ~~From~~
       - ~~Local file/directory~~
       - ~~URL?~~
     - ~~Filter mocks to import~~
   - Tapes
     > Contains a list of Request/ Response chapters
     - Info
        - Tape (file) location
        - Tape size (in KB/ MB/ GB)
        -  Attractors
           - To add new requests to the tape
        - Chapters
          - How many are parented by this tape
          - Type of chapters (live, mock, awaiting)
     - Create/ Edit
       - Data
       - ~~Change sub-directory~~
     - Delete/ Remove (temporarily remove unless server restart)
       - ~~Bulk delete/ remove chapters~~
     - ~~Enable/ disable tapes~~
   - Chapters
     > Request/ Response data
     - Info
       - Chapter size (in KB/ MB/ GB)
     - Create
       - Via editor
       - Via live response generator
         - To current chapter
         - To new chapter
         - For instance testing
       - Live device call
     - View/ Edit
       - Request
       > Not required for chapter uses
       - Response
         - Response code
         - Headers
         - Body
           - JSON beautify option
           - ~~Inline image viewer~~ (if the body is image data)
           - ~~XML/ JSON/ HTML syntax coloring~~
     - Delete (within Edit Tape view)
     - ~~Move to another tape~~
     - Clone to new mock
     - Real-time monitoring of changes
       > Disabled by default, enabled per tape/ chapter editor
   - ~~Logging~~
     - Standard (not within a test bound)
       - Requests
         > Currently only displayed in console logs
       - ~~Match ratio (http request to chapters and tapes)~~
       - Duration each call took
         > Currently only displayed in console logs
       - If the request is towards a recording, await, or live call
     - Testing
       - ~~Request/ call order~~
       - ~~Match ratio (http request to active chapters)~~
       - ~~Duration each call took~~
         - ~~Retrieval~~
         - ~~Live/ await retrieval~~
       - ~~Export data for test viewing via~~
         - ~~CSV~~
         - ~~XML~~
   - ~~Metrics~~
     - ~~Latency per (internal) request~~
   - Testing
     - Enable/ disable specific tapes per test bound
     - Append new tapes the the current test bound
     - ~~Alter Chapter uses per testing bounds~~
     - ~~Test a specific device while allowing others to not be test bound~~
       - Currently the first device (with a unique ID in the header or body) is bound to the idle test bounds
     - ~~Parallel device support with different test bounds~~
     - Replace response content on the fly per testing bounds
       - Body
       - ~~Headers~~
     - ~~Receive files and append to testing logs~~

4. Live calls (`localhost:2202`)
   - Http Methods
     - GET
     - POST
   - Ping (GET: `localhost:2202`)
     > Useful only for tests, returns an empty body with code 200
     
5. General
   - ~~Automatically updating it's data via changing the tape data files~~
   - Initialize blank tapes via CLI HTTP calls
   - Start server via CLI
     > ./gradlew run
