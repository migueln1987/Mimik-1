Style:
(is a conditional) (source (source item)) (find content) -> (action)

**Notes**
= Matchers
1) input matchers (`bb`) process index values as "no valid input" and are skipped
2) output matchers (`dd`) can use index/ groups from the input matcher (`bb`)

= Source item inputs (`a1`)
1. Allowed inputs are `a-zA-Z0-9_`
 - Regex `\w`

= Source var items (`a2`)
1. Allowed inputs must start with a letter, then alpha-numeric
 - Regex `[a-zA-Z]\w*`

= Matchers (`bb`)/ (`dd`)
1. When including the char `}`, the char after it needs to be escaped
 - Current limitation of the parser
 - if the last char is '}', then no escape is needed: `...->{aa}}` => `aa}`
 - **todo**; fix this in a future update
    Example 1: `request:body:{aa }  bb}->cc`
    Becomes 1: `request:body:{aa }\  bb}->cc`

Example 2: `request:body:{w}->{aa }  b}`
Becomes 2: `request:body:{w}->{aa }\ b}`

2. Using variable in the matcher
 - `@{...}` will attempt to use scoped then bound vars
 - `@{&...}` will only use scoped vars

= Output actions (`->....`)
1. 
1. Appending `&` before a char assigns the variable as scoped only
 - scoped vars exist only within it's action sequence list

# Request/ Response

## Request

### Conditionals

| Operation                   | Definition  (continue if...)               |
| --------------------------- | ------------------------------------------ |
| ?request:head               | header has any data                        |
| ?request:head:{bb}          | any header key matches `bb`                |
| ?request:head[a1]           | headers contains `a1`                      |
| ?request:head[a1]&#x3A;{bb} | header `a1` exists, and value matches `bb` |
| ?request:body               | body is not null                           |
| ?request:body:{bb}          | body matches `bb`                          |

### To Actions

-   Request `Actions` are limited to `To Variables` only, due to the `Request` having already been finalized

| Operation                      | Definition                                               | Var Type |
| ------------------------------ | -------------------------------------------------------- | -------- |
| request:head->cc               | 'has any keys' to var `cc`                               | Bool     |
| request:head:{bb}->cc          | 'first matching header key of `bb`' to var `cc`          | String   |
| request:head[a1]->cc           | 'does `a1` exist' to var `cc`                            | Bool     |
| request:head[a1]&#x3A;{bb}->cc | 'matched value contents `bb` of header `a1`' to var `cc` | String   |
| request:body->cc               | 'isNotNull' to var `cc`                                  | Bool     |
| request:body:{bb}->cc          | 'content matching `bb`' to var `cc`                      | String   |
| request:body`...`->&ee         | saves contents of  to local var `ee`                     | String   |

## Response

### Conditionals

| Operation                    | Definition  (continue if...)               |
| ---------------------------- | ------------------------------------------ |
| ?response:head               | header has any data                        |
| ?response:head:{bb}          | any header key matches `bb`                |
| ?response:head[a1]           | headers contains `a1`                      |
| ?response:head[a1]&#x3A;{bb} | header `a1` exists, and value matches `bb` |
| ?response:body               | body is not null                           |
| ?response:body:{bb}          | body matches `bb`                          |

### Actions

#### Header items

| Operation                         | Definition                                                            | Action To | var type          |
| --------------------------------- | --------------------------------------------------------------------- | --------- | ----------------- |
| response:head->cc                 | 'has any keys' to var `cc`                                            | var       | Bool              |
| response:head:{bb}->cc            | 'first matching header key of `bb` to var `cc`'                       | var       | String (or empty) |
| response:head[a1]->cc             | 'does `a1` exist' to var `cc`                                         | var       | Bool              |
| response:head[a1]&#x3A;{bb}->cc   | 'matched contents of `bb` of header `a1`' to var `cc`                 | var       | String (or empty) |
| response:head:{bb}->{dd}          | replace all header keys matching `bb` with the content of `dd`        | self      | --                |
| response:head[a1]->{dd}           | 'content of `dd`' saved to the header `a1` (created if needed)        | self      | --                |
| response:head[a1]&#x3A;{bb}->{dd} | 'matching contents of `bb` of header `a1`' will be replaced with `dd` | self      | --                |

#### Body items

| Operation                | Definition                                                | Action To |
| ------------------------ | --------------------------------------------------------- | --------- |
| response:body->cc        | 'is body not null' to var `cc`                            | var       |
| response:body:{bb}->cc   | 'content matching `bb`' to var `cc`                       | var       |
| response:body->{dd}      | 'content of `dd`' to the body                             | self      |
| response:body:{bb}->{dd} | replace matching content of `bb` with the content of `dd` | self      |

# Variables
== Var prefixes ==
| prefix | action           | example |
| ------ | ---------------- | ------- |
|        | Bound and Scoped | var     |
| s      | Scoped           | svar    |
| b      | Bound            | bvar    |

## Conditionals

| Operation          | Definition  (continue if...)     |
| ------------------ | -------------------------------- |
| ?var               | any variable are set (not empty) |
| ?var:{bb}          | any var names match `bb`         |
| ?var[a2]           | var `a2` is not empty            |
| ?var[a2]&#x3A;{bb} | content of `a2` matches `bb`     |

## Actions

| Operation               | Definition                                          | Action To | var type |
| ----------------------- | --------------------------------------------------- | --------- | -------- |
| var->cc                 | 'there are set variables' to var `cc`               | var       | Bool     |
| var[a2]->cc             | 'var `a2` is set' to var cc                         | var       | Bool     |
| var{bb}->cc             | 'first var name matching `bb`' to var `cc`          | var       |          |
| var[a2]&#x3A;{bb}->cc   | 'matching content `bb` of var `a2`' to var `cc`     | var       |          |
| var[a2]->{dd}           | write content `dd` to var `a2`                      | self      |          |
| var[a2]&#x3A;{bb}->{dd} | replace matching content `bb` of var `a2` with `dd` | self      |          |

# Uses

## Conditionals

| Operation               | Definition  (continue if...)                       |
| ----------------------- | -------------------------------------------------- |
| ?use:{`a`}              | is `a`                                             |
| ?use:{>`a`}             | greater than `a`                                   |
| ?use:{>=`a`}            | greater or equals `a`                              |
| ?use:{&lt;`a`}          | less than `a`                                      |
| ?use:{&lt;=`a`}         | less or equals `a`                                 |
| ?use:{`a`..`b`}         | between range `a` and `b` (inclusive)              |
| ?use:{`a`,`b`..`c`,`d`} | single (`a`), ranged (`b` to `c`), or single (`d`) |

## Actions

| Operation         | Definition                           | Action To |
| ----------------- | ------------------------------------ | --------- |
| use:{`...`}->cc   | save content to var `cc`             | var       |
| use->{dd}         | set content of `dd` to field         | self      |
| use:{`...`}->{dd} | save (valid) content dd to use field | self      |
