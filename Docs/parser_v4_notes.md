# Pre notes

## Variable Prefix

Note: only applies to retrieving variables, not storing

| Command | Sequence | Chapter | Bounds |
| ------- | :------: | :-----: | :----: |
| name    |     Y    |    N    |    N   |
| &name   |     N    |    Y    |    N   |
| %name   |     N    |    N    |    Y   |
| ^name   |     Y    |    Y    |    Y   |
| &^name  |     N    |    Y    |    Y   |
| %^name  |     N    |    N    |    Y   |

## Variable Suffix

| Command    | definition   | Results var                          |
| ---------- | ------------ | ------------------------------------ |
| name       | Default      | name                                 |
| name?      | Exists       | name?                                |
| name#      | Count        | name#                                |
| name@      | Cond. Result | name@                                |
| name\_#    | Spread       | name_0, name_1, etc.                 |
| name\_#0   | Index        | name_0                               |
| name\_#1   | Index        | name_1                               |
| name\_#xxx | Index        | name_xxx (or last index)<sup>1</sup> |
| name\_?    | Last Index   | name\_?                              |

Comments:
1) If the last index is used, "name\_?" and "name#" are created instead of "index_xxx"

# Part 1

| item               | data on     | flag A | flag B |
| ------------------ | ----------- | ------ | ------ |
| req.head           | keys        | -1     | 0      |
| req.head{mat}      | keys        | -1     | .1     |
| req.head[key]      | keys/values | -1     | ..2    |
| req.head[key]{mat} | values      | -1     | ...3   |
| res.head           | keys        | 1      | 0      |
| res.head{mat}      | keys        | 1      | .1     |
| res.head[key]      | keys/values | 1      | ..2    |
| res.head[key]{mat} | values      | 1      | ...3   |
| req.body           | content     | -1     | 0      |
| req.body{mat}      | content     | -1     | .1     |
| res.body           | content     | 1      | 0      |
| res.body{mat}      | content     | 1      | .1     |
| var                | keys        | 0      | 0      |
| var{mat}           | keys        | 0      | .1     |
| var[key]           | keys        | 0      | ..2    |
| var[key]{mat}      | values      | 0      | ...3   |
| &var               | keys        | 1      | 0      |
| &var{mat}          | keys        | 1      | .1     |
| &var[key]          | keys        | 1      | ..2    |
| &var[key]{mat}     | values      | 1      | ...3   |
| %var               | keys        | ..2    | 0      |
| %var{mat}          | keys        | ..2    | .1     |
| %var[key]          | keys        | ..2    | ..2    |
| %var[key]{mat}     | values      | ..2    | ...3   |
| use                | value       | 1      | 0      |
| use{mat}           | value       | 1      | .1     |
| use[other]         | value       | ..2    | ..2    |
| use[other]{mat}    | value       | ..2    | ...3   |

## Flag B

| Flag | meaning                | view       |
| ---- | ---------------------- | ---------- |
| 0    | source root            | xx         |
| 1    | source keys            | xx{zz}     |
| 2    | source's data at key   | xx[yy]     |
| 3    | source's key's value/s | xx[yy]{zz} |

## Flag chart

| item               | flag A | flag B | Exists? | Count | Cond. | Spread |
| ------------------ | ------ | ------ | ------- | ----- | ----- | ------ |
| req.head           | -1     | 0      | yes     | yes   | yes   | yes    |
| req.head{mat}      | -1     | .1     | yes     | yes   | yes   | yes    |
| req.head[key]      | -1     | ..2    | yes     | yes   | yes   | yes    |
| req.head[key]{mat} | -1     | ...3   | yes     | yes   | yes   | yes    |
| res.head           | 1      | 0      | yes     | yes   | yes   | yes    |
| res.head{mat}      | 1      | .1     | yes     | yes   | yes   | yes    |
| res.head[key]      | 1      | ..2    | yes     | yes   | yes   | yes    |
| res.head[key]{mat} | 1      | ...3   | yes     | yes   | yes   | yes    |
| req.body           | -1     | 0      | yes     | no    | yes   | no     |
| req.body{mat}      | -1     | .1     | yes     | yes   | yes   | yes    |
| res.body           | 1      | 0      | yes     | no    | yes   | no     |
| res.body{mat}      | 1      | .1     | yes     | yes   | yes   | yes    |
| var                | 0      | 0      | yes     | yes   | yes   | yes    |
| var{mat}           | 0      | .1     | yes     | yes   | yes   | yes    |
| var[key]           | 0      | ..2    | yes     | no    | yes   | no     |
| var[key]{mat}      | 0      | ...3   | yes     | yes   | yes   | yes    |
| &var               | .1     | 0      | yes     | yes   | yes   | yes    |
| &var{mat}          | .1     | .1     | yes     | yes   | yes   | yes    |
| &var[key]          | .1     | ..2    | yes     | no    | yes   | no     |
| &var[key]{mat}     | .1     | ...3   | yes     | yes   | yes   | yes    |
| %var               | ..2    | 0      | yes     | yes   | yes   | yes    |
| %var{mat}          | ..2    | .1     | yes     | yes   | yes   | yes    |
| %var[key]          | ..2    | ..2    | yes     | no    | yes   | no     |
| %var[key]{mat}     | ..2    | ...3   | yes     | yes   | yes   | yes    |
| use                | 1      | 0      | no      | no    | yes   | no     |
| use{mat}           | 1      | .1     | no      | no    | yes   | no     |
| use[other]         | ..2    | ..2    | no      | no    | yes   | no     |
| use[other]{mat}    | ..2    | ...3   | no      | no    | yes   | no     |

## Default result

| item               | type   | first...   |
| ------------------ | ------ | ---------- |
| req.head           | string | key        |
| req.head{mat}      | string | match      |
| req.head[key]      | string | value      |
| req.head[key]{mat} | string | match      |
| res.head           | string | key        |
| res.head{mat}      | string | match      |
| res.head[key]      | string | value      |
| res.head[key]{mat} | string | match      |
| req.body           | string | everything |
| req.body{mat}      | string | match      |
| res.body           | string | everything |
| res.body{mat}      | string | match      |
| var                | string | key        |
| var{mat}           | string | match      |
| var[key]           | string | value      |
| var[key]{mat}      | string | match      |
| &var               | string | key        |
| &var{mat}          | string | match      |
| &var[key]          | string | value      |
| &var[key]{mat}     | string | match      |
| %var               | string | key        |
| %var{mat}          | string | match      |
| %var[key]          | string | value      |
| %var[key]{mat}     | string | match      |
| use                | int    | value      |
| use{mat}           | bool   | result     |
| use[other]         | int    | value      |
| use[other]{mat}    | bool   | result     |

## Exist results

Returns: Boolean

| Command            | prefix  | of item |
| ------------------ | ------- | ------- |
| req.head           | has     | data    |
| req.head{mat}      | matches | key     |
| req.head[key]      | has     | key     |
| req.head[key]{mat} | matches | value   |
| res.head           | has     | data    |
| res.head{mat}      | matches | key     |
| res.head[key]      | has     | key     |
| res.head[key]{mat} | matches | value   |
| req.body           | has     | data    |
| req.body{mat}      | matches | data    |
| res.body           | has     | data    |
| res.body{mat}      | matches | data    |
| var                | has     | data    |
| var{mat}           | matches | key     |
| var[key]           | has     | key     |
| var[key]{mat}      | matches | value   |
| &var               | has     | data    |
| &var{mat}          | matches | key     |
| &var[key]          | has     | key     |
| &var[key]{mat}     | matches | value   |
| %var               | has     | data    |
| %var{mat}          | matches | key     |
| %var[key]          | has     | key     |
| %var[key]{mat}     | matches | value   |

## Count results

Returns: Int

| Command            | size of       |
| ------------------ | ------------- |
| req.head           | keys          |
| req.head{mat}      | matches       |
| req.head[key]      | keys == [key] |
| req.head[key]{mat} | matches       |
| res.head           | keys          |
| res.head{mat}      | matches       |
| res.head[key]      | keys == [key] |
| res.head[key]{mat} | matches       |
| req.body{mat}      | matches       |
| res.body{mat}      | matches       |
| var                | keys          |
| var{mat}           | matches       |
| var[key]{mat}      | matches       |
| &var               | keys          |
| &var{mat}          | matches       |
| &var[key]{mat}     | matches       |
| %var               | keys          |
| %var{mat}          | matches       |
| %var[key]{mat}     | matches       |

## Conditional Result results

| command            | flag B | definition            |
| ------------------ | ------ | --------------------- |
| req.head           | 0      | has data              |
| req.head{mat}      | .1     | any key matches       |
| req.head[key]      | ..2    | contains key          |
| req.head[key]{mat} | ...3   | [key]'s value matches |
| res.head           | 0      | has data              |
| res.head{mat}      | .1     | any key matches       |
| res.head[key]      | ..2    | contains key          |
| res.head[key]{mat} | ...3   | [key]'s value matches |
| req.body           | 0      | not null              |
| req.body{mat}      | .1     | matches               |
| res.body           | 0      | not null              |
| res.body{mat}      | .1     | matches               |
| var                | 0      | has data              |
| var{mat}           | .1     | any key matches       |
| var[key]           | ..2    | contains key          |
| var[key]{mat}      | ...3   | [key]'s value matches |
| &var               | 0      | has data              |
| &var{mat}          | .1     | any key matches       |
| &var[key]          | ..2    | contains key          |
| &var[key]{mat}     | ...3   | [key]'s value matches |
| %var               | 0      | has data              |
| %var{mat}          | .1     | any key matches       |
| %var[key]          | ..2    | contains key          |
| %var[key]{mat}     | ...3   | [key]'s value matches |
| use                | 0      | value                 |
| use{mat}           | .1     | value matches         |
| use[other]         | ..2    | value                 |
| use[other]{mat}    | ...3   | value matches         |

Note:

-   Uses's value, when used as a conditional, is compared to "is enabled"

## Spread results

Returns: String

| item               | items   |
| ------------------ | ------- |
| req.head           | keys    |
| req.head{mat}      | matches |
| req.head[key]      | values  |
| req.head[key]{mat} | matches |
| res.head           | keys    |
| res.head{mat}      | matches |
| res.head[key]      | values  |
| res.head[key]{mat} | matches |
| req.body{mat}      | matches |
| res.body{mat}      | matches |
| var                | keys    |
| var{mat}           | matches |
| var[key]{mat}      | matches |
| &var               | keys    |
| &var{mat}          | matches |
| &var[key]{mat}     | matches |
| %var               | keys    |
| %var{mat}          | matches |
| %var[key]{mat}     | matches |

# Part 2

| item               | strings       | list size | flag B | out type    | meaning            |
| ------------------ | ------------- | --------- | ------ | ----------- | ------------------ |
| req.head           | size / ""     | 1         | 0      | true/ false | has Data           |
| req.head{mat}      | key names     | many      | .1     | cond\*      | has Key            |
| req.head[key]      | [key]s values | many      | ..2    | string      | value              |
| req.head[key]{mat} | [key]s values | many      | ...3   | string      | matched            |
| res.head           | size / ""     | 1         | 0      | true/ false | has Data           |
| res.head{mat}      | key names     | many      | .1     | true/ false | has Key            |
| res.head[key]      | [key]s values | many      | ..2    | string      | value              |
| res.head[key]{mat} | [key]s values | many      | ...3   | string      | matched            |
| req.body           | body / ""     | 1         | 0      | true/ false | has Data           |
| req.body{mat}      | body          | many      | .1     | array       | matched            |
| res.body           | body / ""     | 1         | 0      | true/ false | has Data           |
| res.body{mat}      | body          | many      | .1     | array       | matched            |
| var                | size / ""     | 1         | 0      | true/ false | has Data           |
| var{mat}           | var names     | many      | .1     | true/ false | has Key            |
| var[key]           | [key]s value  | 1         | ..2    | string      | value              |
| var[key]{mat}      | [key]s values | many      | ...3   | array       | matched            |
| &var               | size / ""     | 1         | 0      | true/ false | has Data           |
| &var{mat}          | var names     | many      | .1     | string      | has Key            |
| &var[key]          | [key]s value  | 1         | ..2    | string      | value              |
| &var[key]{mat}     | [key]s values | many      | ...3   | array       | matched            |
| %var               | size / ""     | 1         | 0      | true/ false | has Data           |
| %var{mat}          | var names     | many      | .1     | true/ false | has Key            |
| %var[key]          | [key]s value  | 1         | ..2    | string      | value              |
| %var[key]{mat}     | [key]s values | many      | ...3   | array       | matched            |
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
| &var               | has Data    | true          |
| &var{mat}          | has Key     | true          |
| &var[key]          | value       | not empty     |
| &var[key]{mat}     | matched     | not empty     |
| %var               | has Data    | true          |
| %var{mat}          | has Key     | true          |
| %var[key]          | value       | not empty     |
| %var[key]{mat}     | matched     | not empty     |
| use                | is enabled  | true          |
| use{mat}           | value       | compare       |
| use[other]         | has chapter | true          |
| use[other]{mat}    | value       | compare       |

| command   | operation              | Meaning                                                                                |
| --------- | ---------------------- | -------------------------------------------------------------------------------------- |
| request   | act, cont              | Standard action; do action and continue                                                |
| ?request  | (act + cont) if true   | Continue only if Cond was `true`                                                       |
| !request  | (act + cont) if false  | Continue only if Cond was `false`                                                      |
| ~?request | (act) if true, cont    | Continue, but only process action if Cond was `true`                                   |
| ~!request | (act) if false, cont   | Continue, but only process action if Cond was `false`                                  |
| ~request  | (act) if prior was `~` | Process this command only if prefixed with one starting with `~` and it was successful |

| command   | require state | true -> cont | false -> cont | true -> act | false -> act |
| --------- | :-----------: | :----------: | :-----------: | :---------: | :----------: |
| request   |       N       |       Y      |       Y       |      Y      |       Y      |
| ?request  |       Y       |       Y      |       N       |      Y      |       N      |
| !request  |       Y       |       N      |       Y       |      N      |       Y      |
| ~?request |       N       |       Y      |       Y       |      Y      |       N      |
| ~!request |       N       |       Y      |       Y       |      N      |       Y      |
| ~request  |       Y       |       Y      |       Y       |      ~      |       ~      |

# Final Part

== eof
