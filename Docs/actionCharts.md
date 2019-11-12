# Mock API

## PUT
```mermaid
sequenceDiagram
participant in as Call
participant m as Mimik
participant g as TapeCatalog
participant t as Tape
participant c as Mock

in->>m: PUT
opt No "mock" parameters
	m-->>in: BadRequest (400)
end

m->>g: Find tape by "tape_name"
alt Tape exists
	g-->>m: Tape
else No tape found
	m->>g: Query by route path & queries
	alt Qualifying tape found
		g-->>m: Tape
	else No matching tape
		m->>t: Create new Tape
		Note over t: Tape {name, url, allowliverecordings}
		t-->g: Add new tape
		g-->>m: New Tape
	end
end
opt New Tape
	m->>t: Set filters by "mockFilter_{x}"
end
Note over m: Save if "tape_save" is true

opt "Tape_Only" = true
	m-->>in: Created (201) or Found (302)
end

m->>t: Find mock by "name"
alt Mock exists
	t-->>m: Mock
else No mock found
	t->>c: Create a new mock
	c-->t: Add new mock
	t-->>m: New Mock
end

opt Using existing tape
	t-->>m: Append tape filters to new call filters
end
m->>c: Set filters

alt "Live" is not true & "Await" is not true
	Note over m: Create mock http response
	m->>c: Set mock http response
else
	m->>c: Set mock http response to null
end

m->>c: Set "use" value
Note over m: Save if tape can be saved

m-->>in: Created (201) or Found (302)
```

## ~~PATCH~~

# General
## RequestAttractors (mockFilter_{filter})
`Ratio` is defined by "how much of the literal regex matches"<br>
> Example: <br>
> "`matchevery`" would have a higher ratio than "`match.*`" for an input of `matcheverything`<br>
> "matchevery" = 0.8%<br>
> "match.*" = 0.46%

`avoid` ratio will always equal `1` if it matches

`{...} Matches? = ...`:
- No attractors + testing data = `Fail`
- Attractor has data + testing data is empty = `Fail`
- Any Attractor's `allowAllInputs` is true = `Pass`
- All Attractor values are empty = `Pass`
  - And the attractor's `Except` is false

```mermaid
sequenceDiagram
participant a as RequestAttractors
participant f as findBest
participant m as AttractorMatches
participant c as Custom
participant p as Path
participant q as Param
participant b as Body

a->>f: request
Note over a,f: {Source<Map>, path?, queries?, body?, custom?}
opt Source is empty
	f-->>a: NotFound (404)
end

loop source map
	f->>c: (Custom) Matches?
	alt Fails
		c-->>f: no match
	else Pass
		c-->>f: continue
	end
	f->>p: Path Matches?
	alt Fails
		p-->>f: no match
	else Pass
		p-->>f: continue
	end
	f->>q: Param Matches?
	alt Fails
		q-->>f: no match
	else Pass
		q-->>f: continue
	end
	f->>b: Body Matches?
	alt Fails
		b-->>f: no match
	else Pass
		b-->>f: continue
	end
end

opt Required matchers = 0
	f-->>a: NotFound (404)
end

f->>m: findBestMatch
alt Match by Required
	alt None
		m-->>f: null
	else Single match
		m-->>f: match
	else Multiple
		m-->>m: Continue to "Required Ratio"
	end
else Match by Required Ratio
	alt None
		m-->>f: null
	else Single match
		m-->>f: match
	else Multiple
		m-->>m: Continue to "Optional"
	end
else Match by Optional
	alt None
		m-->>f: null
	else Single match
		m-->>f: match
	else Multiple
		m-->>m: Continue to "Optional Ratio"
	end
else Match by Optional Ratio
	alt None or Multiple
		m-->>f: null
	else Single match
		m-->>f: match
	end
end

alt Matches returned null
	f-->>a: Conflict (409)
else Single match
	f-->>a: {Match} + Found (302)
end
```
