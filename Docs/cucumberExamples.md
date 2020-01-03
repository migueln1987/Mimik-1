
## Start
> Required to start a test bound
>> Test is idle until a unique ID is seen in a live call header or body.
>> Test is ONLY applied to calls with include the unique ID

`Given Start 2m test named #{scenarioName} using tapes: xxx, yyy`
- Cucumber based step
- 2 minute test
- Defined test name
- Enable only the tapes: XXX and YYY

`step "Start 1h test named #{scenarioName} using tape: zzz"`
- Tests can run for hour/s if needed

`step "Start 30s test named using tape: zzz"`
- Tests can be as few a seconds
- Test names are optional
  - one will be given to you via the header named `handle`
  
  
## Append
> Append tapes to test bound
>> Tests are started if one isn't active
>> Invalid tape names are ignored

`Then Append tapes to the current test: xx, yy`
- Attempts to use the last started test handle
-- A new handle will be generated if no test is active

## Disable
> Disables tapes from the current test bound
>> If no test is running or the tapes are invalid,
>> then the action does nothing
`Then Disable tapes in the current test: xx, yy`


## Stop
> Stops the current test or all the tests

`Then Stop test: AA`
- Attempts to stop a single test "AA"

`Then Stop tests: AA, BB`
- Attempts to stop tests "AA" and "BB"

`Then Stop all tests`
- Stops all running tests and finalizes the test to release the unique ID bound

Note:  
Appending "##Finalize" to `Stop tests` (ex: `step Stop tests: AA, ##Finalize`) 
will stop and finalize the given test/s.

## Replace data in the response
> Given a Chapter name, type, and what to replace, data is changed on the fly for the test bounds
>> Only applies to Chapters in enabled tapes for the current test

### With a single chapter
```ruby
Given In the following response "AAA", apply the following replacements:
| Body | arm | leg |
```

```ruby
Given In the following response "AAA", apply the following replacements:
| Type | From | To  |
| Body | arm  | leg |
```
- Top row is ignored, as mimikConfig will view it as a valid Header row

### With multiple chapters
```ruby
Given In the following responses, apply the following replacements:
| AA | Body | arm   | leg  |
| BB | Body | wings | legs |
```

```ruby
Given In the following responses, apply the following replacements:
| Response | Type | From  | To   |
| AA       | Body | arm   | leg  |
| BB       | Body | wings | legs |
```
- Top row is ignored, as mimikConfig will view it as a valid Header row

