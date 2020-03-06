# Part 1

| item               | flag A | flag B | strings           |
| ------------------ | ------ | ------ | ----------------- |
| req.head           | -1     | 0      | size / ""         |
| req.head{mat}      | -1     | 1      | list of keys      |
| req.head[key]      | -1     | 2      | all [key]s values |
| req.head[key]{mat} | -1     | 3      | all [key]s values |
| res.head           | 1      | 0      | size / ""         |
| res.head{mat}      | 1      | 1      | list of keys      |
| res.head[key]      | 1      | 2      | all [key]s values |
| res.head[key]{mat} | 1      | 3      | all [key]s values |
| req.body           | -1     | 0      | body / ""         |
| req.body{mat}      | -1     | 1      | body              |
| res.body           | 1      | 0      | body / ""         |
| res.body{mat}      | 1      | 1      | body              |
| var                | 0      | 0      | size / ""         |
| var{mat}           | 1 & 2  | 1      | list of keys      |
| var[key]           | 1 & 2  | 2      | all [key]s values |
| var[key]{mat}      | 1 & 2  | 3      | all [key]s values |
| svar               | 1      | 0      | size / ""         |
| svar{mat}          | 1      | 1      | list of keys      |
| svar[key]          | 1      | 2      | all [key]s values |
| svar[key]{mat}     | 1      | 3      | all [key]s values |
| bvar               | 2      | 0      | size / ""         |
| bvar{mat}          | 2      | 1      | list of keys      |
| bvar[key]          | 2      | 2      | all [key]s values |
| svar[key]{mat}     | 2      | 3      | all [key]s values |
| use                | 1      | 0      | true/ false       |
| use{mat}           | 1      | 1      | value / ""        |
| use[other]         | 2      | 2      | true/ false       |
| use[other]{mat}    | 2      | 3      | value / ""        |

## Flag B

| Flag | meaning                   | view       |
| ---- | ------------------------- | ---------- |
| 0    | source root               | xx         |
| 1    | source content/ key names | xx{zz}     |
| 2    | source's key(s) exist?    | xx[yy]     |
| 3    | source's key(s) value     | xx[yy]{zz} |

# Part 2

| item               | strings       | list size | flag B | out type    | meaning            |
| ------------------ | ------------- | --------- | ------ | ----------- | ------------------ |
| req.head           | size / ""     | 1         | 0      | true/ false | has Data           |
| req.head{mat}      | key names     | many      | .1     | true/ false | has Key            |
| req.head[key]      | [key]s values | many      | ..2    | string      | value              |
| req.head[key]{mat} | [key]s values | many      | ...3   | string      | matched            |
| res.head           | size / ""     | 1         | 0      | true/ false | has Data           |
| res.head{mat}      | key names     | many      | .1     | true/ false | has Key            |
| res.head[key]      | [key]s values | many      | ..2    | string      | value              |
| res.head[key]{mat} | [key]s values | many      | ...3   | string      | matched            |
| req.body           | body / ""     | 1         | 0      | true/ false | has Data           |
| req.body{mat}      | body          | 1         | .1     | string      | matched            |
| res.body           | body / ""     | 1         | 0      | true/ false | has Data           |
| res.body{mat}      | body          | 1         | .1     | string      | matched            |
| var                | size / ""     | 1         | 0      | true/ false | has Data           |
| var{mat}           | var names     | many      | .1     | true/ false | has Key            |
| var[key]           | [key]s values | many      | ..2    | string      | value              |
| var[key]{mat}      | [key]s values | many      | ...3   | string      | matched            |
| svar               | size / ""     | 1         | 0      | true/ false | has Data           |
| svar{mat}          | var names     | many      | .1     | string      | has Key            |
| svar[key]          | [key]s values | many      | ..2    | string      | value              |
| svar[key]{mat}     | [key]s values | many      | ...3   | string      | matched            |
| bvar               | size / ""     | 1         | 0      | true/ false | has Data           |
| bvar{mat}          | var names     | many      | .1     | true/ false | has Key            |
| bvar[key]          | [key]s values | many      | ..2    | string      | value              |
| bvar[key]{mat}     | [key]s values | many      | ...3   | string      | matched            |
| use                | true/ false   | 1         | 0      | true/ false | is enabled         |
| use{mat}           | value         | 1         | .1     | string      | value              |
| use[other]         | true/ false   | 1         | ..2    | true/ false | chapter exists     |
| use[other]{mat}    | value         | 1         | ..3    | string      | value (from other) |

| flagB | result | mResult |
| ----- | ------ | ------- |
| 0     | yes    | no      |
| 1     | yes    | yes     |
| 2     | yes    | no      |
| 3     | yes    | yes     |

# Part 3

| input              | data        | isCond search |
| ------------------ | ----------- | ------------- |
| req.head           | has Data    | true          |
| req.head{mat}      | has Key     | true          |
| req.head[key]      | value       | not empty     |
| req.head[key]{mat} | matched     | not empty     |
| res.head           | has Data    | true          |
| res.head{mat}      | has Key     | true          |
| res.head[key]      | value       | not empty     |
| res.head[key]{mat} | matched     | not empty     |
| req.body           | has Data    | true          |
| req.body{mat}      | matched     | not empty     |
| res.body           | has Data    | true          |
| res.body{mat}      | matched     | not empty     |
| var                | has Data    | true          |
| var{mat}           | has Key     | true          |
| var[key]           | value       | not empty     |
| var[key]{mat}      | matched     | not empty     |
| svar               | has Data    | true          |
| svar{mat}          | has Key     | true          |
| svar[key]          | value       | not empty     |
| svar[key]{mat}     | matched     | not empty     |
| bvar               | has Data    | true          |
| bvar{mat}          | has Key     | true          |
| bvar[key]          | value       | not empty     |
| bvar[key]{mat}     | matched     | not empty     |
| use                | is enabled  | true          |
| use{mat}           | value       | compare       |
| use[other]         | has chapter | true          |
| use[other]{mat}    | value       | compare       |

| command   | operation             |
| --------- | --------------------- |
| request   | act, cont             |
| ?request  | (act + cont) if true  |
| !request  | (act + cont) if false |
| ~?request | (act) if true, cont   |
| ~!request | (act) if false, cont  |

| command   | require state | true -> cont | false -> cont | true -> act | false -> act |
| --------- | :-----------: | :----------: | :-----------: | :---------: | :----------: |
| request   |       O       |       X      |       X       |      X      |       X      |
| ?request  |       X       |       X      |       O       |      X      |       O      |
| !request  |       X       |       O      |       X       |      O      |       X      |
| ~?request |       O       |       X      |       X       |      X      |       O      |
| ~!request |       O       |       X      |       X       |      O      |       X      |
| ~request  |       X       |       X      |       X       |      ~      |       ~      |

# Final Part

== eof
