#Features

1. Mimik
   1. Tape
      - Loading of tape data
        - Accept tapes regardless of directory depth
          > Note: Must be within the approved `{project}/Tapes` directory
        - ~~Loading of tapes from a (local) directory outside of the project~~
      - Accept new calls via filters
      - ~~Allow passthrough calls via flag & filter/s~~
   2. Mock
      - Http Response
        - Response code: `200 (OK) `, `404 (Not Found)`, etc. 
        - Headers
          - ~~Dynamic content~~
        - Body
          - ~~Dynamic content~~
      - Filters (request attractors)
        - Path: `/sub/path/`
        - ~~Header/s~~: `Key: value`
        - Parameter: `Key1=Value1&Key2=Value2`
        - Body: `{ "json": true }`
      - Limited Use interactions (mock expires/ disables after a defined usage limit)
      - `Await`: response is created on demand (for initializing Mimik)

2. API (`localhost:4321/mock`)
   - PUT
   - ~~PATCH~~
   
3. Http Mimik Editor (`localhost:4321/`)
   - ~~Import~~
     - ~~From~~
       - ~~Local file/directory~~
       - ~~URL?~~
     - ~~Filter mocks to import~~
   - Tapes
     - Create
     - ~~Edit~~
       - ~~Data~~
       - ~~Change sub-directory~~
     - Delete
   - ~~Chapters~~
     - ~~Create~~
       - ~~With live response~~
     - ~~Modify~~
     - ~~Move to another tape~~
     - ~~Clone to new mock~~
     - ~~Real-time monitoring of changes~~
   - ~~Logging~~
   - ~~Metrics~~

4. Live calls (`localhost:2202`)
   - Http Methods
     - GET
     - POST
   - Await mocks (set `Response` via a live call)
   - Create new tape/ mock for
     - New (unknown) calls (to a configured tape)
     - No tape is configured to accept the call
     
5. General
   - ~~Automatically updating it's data via changing the tape data files~~
