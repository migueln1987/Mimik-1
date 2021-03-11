# 0. Home Page
>- Welcome screen to Mimik's configuration
>- Formatting: Grid
>- [localhost:4321](localhost:4321)
>- [{host}.com/gui]({host}.com/gui)

|||
|---|---|
| [Connections](#1-connections)
| [Containers](#1-containers) | [Tapes viewer](#1-tapes-viewer)
| [Logs](#1-logs) | [Settings](#1-settings)


# 1. Connections
>- Unique connections and their statuses
>- Formatting: Rows
<details>
<summary>URL Examples</summary>

| Generic |
|---|
| [localhost:4321/connections](localhost:4321/connections)
| [{host}.com/gui/connections]({host}.com/gui/connections)
</details>

<details>
<summary>View</summary>

| Item | Format |
|---|---|
| IP address
| Bound state<sup>1</sup> | `none`, `no container`, `no tape`, etc.
| [bound data](#2-bound-editor) (or `none`) | Data contains: linking and vars
| Buttons | [bound setup](#2-bound-editor), delete

1: (wip) Hovering over or selecting the drop-down will show the current states the 
bound has passed through til failure/ success
</details>


# 1. Containers
>- Viewing of collections which host mock "environments"
>- Formatting: Rows
<details>
<summary>URL Examples</summary>

| Generic|
|---|
| [localhost:4321/containers](localhost:4321/containers)
| [{host}.com/gui/containers]({host}.com/gui/containers)
</details>

<details>
<summary>View</summary>

| Item | Format |
|---|---|
| Name
| Tapes | Count, +[editor](#1-tapes-viewer)<sup>1</sup>
| Hooks | Count, +[editor](#2-hook-editor)
| Bounds | Count, +[editor](#2-bound-editor)
| Variables | Count
| Buttons | [edit](#2-container-editor), delete

1: tapes editor includes scope contained tapes
</details>


# 1. Tapes viewer
>- Viewing of tapes contained on the server, optionally filtered by container
>- Formatting: Rows
<details>
<summary>URL Examples</summary>

| Generic |
|---|
| [localhost:4321/tapes](localhost:4321/tapes)
| [{host}.com/gui/tapes]({host}.com/gui/tapes)

| Filtered by container |
|---|
| [localhost:4321/tapes?box={container_name}](localhost:4321/tapes?box={container_name})
| [{host}.com/gui/tapes?box={container_name}]({host}.com/gui/tapes?box={container_name})
</details>

<details>
<summary>View</summary>

| Item | Format |
|---|---|
| Name
| Scope | `none` or [container](#1-containers)
| Configs (maybe?)
| Attractors | Count, +[editor](#2-attractor-editor))
| Chapters | Count, +[editor](#2-chapter-editor))
| Buttons | [edit](#2-tape-editor), delete
</details>


# 1. Logs
>- Reports of Mimik and it's components' actions
>- List of logs which can be viewed in a new window (or side preview)
>- Formatting: Rows
<details>
<summary>URL Examples</summary>

| Generic |
|---|
| [localhost:4321/logs](localhost:4321/logs)
| [{host}.com/gui/logs]({host}.com/gui/logs)
</details>

<details>
<summary>View</summary>

| Filtering by | Format |
|---|---|
| Date | `range`, `to/ from`
| Type | `system`, `container`, `tape`, `chapter`, etc.
| Flag | `debug`, `info`, `warn`, `verbose`

| Item | Format |
|---|---|
| Info type | `debug`, `info`, `warn`, `verbose`
| Date/ time | |
| Flag header (tbd) | |
| Buttons | [view](#2-log-viewer), export, delete
</details>


# 1. Settings
>- General configuration of Mimik
>- Formatting: Table

<details>
<summary>URL</summary>

| Generic |
|---|
| [localhost:4321/settings](localhost:4321/settings)
| [{host}.com/gui/settings]({host}.com/gui/settings)
</details>

- TBD


# 2. Bound Editor
>- Data specific to this bound
>- Formatting: Table

<details>
<summary>URL Examples</summary>

| Create New |
|---|
| [localhost:4321/edit?bound=](localhost:4321/edit?bound=)
| [{host}.com/gui/edit?bound=]({host}.com/gui/edit?bound=)

| Edit (by creation/ session ID) |
|---|
| [localhost:4321/edit?bound_id={bound_id}](localhost:4321/edit?bound_id={bound_id})
| [{host}.com/gui/edit?bound_id={bound_id}]({host}.com/gui/edit?bound_id={bound_id})

| Edit (by name)  |
|---|
| [localhost:4321/edit?box={container_name}&bound={bound_name}](localhost:4321/edit?box={container_name}&bound={bound_name})
| [{host}.com/gui/edit?box={container_name}&bound={bound_name}]({host}.com/gui/edit?box={container_name}&bound={bound_name})
</details>

<details>
<summary>View</summary>

| Item | Format |
|---|---|
| Linking items (hook attractors)
| Container | `none` or [view](#2-container-editor)
| Variables
</details>


# 2. Tape Editor
>- Stateless view of a container for chapters
>- Formatting: Table
<details>
<summary>URL examples</summary>

| Create new|
|---|
| [localhost:4321/edit?tape=](localhost:4321/edit?tape=)
| [{host}.com/gui/edit?tape=]({host}.com/gui/edit?tape=)

| Edit / View|
|---|
| [localhost:4321/edit?tape={tape_name}](localhost:4321/edit?tape={tape_name})
| [{host}.com/gui/edit?tape={tape_name}]({host}.com/gui/edit?tape={tape_name})

| Container scoped|
|---|
| [localhost:4321/edit?box={container_name}&tape={tape_name}](localhost:4321/edit?box={container_name}&tape={tape_name})
| [{host}.com/gui/edit?box={container_name}&tape={tape_name}]({host}.com/gui/edit?box={container_name}&tape={tape_name})
</details>

<details>
<summary>View</summary>

| Item | Format |
|---|---|
| Name
| Scope | `none` or [container](#1-containers)
| Configs (maybe?)
| Attractors
| Chapters
</details>


# 2. Hook Editor
>- Defines how a bound is discovered and set up
>- Formatting: Table

<details>
<summary>URL Examples</summary>

| Create new |
|---|
| [localhost:4321/edit?hook=](localhost:4321/edit?hook=)
| [{host}.com/gui/edit?hook=]({host}.com/gui/edit?hook=)

| View / Edit (by ID)|
|---|
| [localhost:4321/edit?hook={hook_id}](localhost:4321/edit?hook={hook_id})
| [{host}.com/gui/edit?hook={hook_id}]({host}.com/gui/edit?hook={hook_id})

| View / Edit (by name)|
|---|
| [localhost:4321/edit?box={box_name}&hook={hook_name}](localhost:4321/edit?box={box_name}&hook={hook_name})
| [{host}.com/gui/edit?box={box_name}&hook={hook_name}]({host}.com/gui/edit?box={box_name}&hook={hook_name})
</details>

<details>
<summary>View</summary>

| Item | Format |
|---|---|
| Attractors
| Actions<sup>1</sup>
| Initial Variables
| Initial Chap states

1: Actions are called on during conversion of a successful hook to a bound
</details>


# 2. Container Editor
>- Editing of stateful items
>- Formatting: Table
<details>
<summary>URL Examples</summary>

| Create new |
|---|
| [localhost:4321/edit?box=](localhost:4321/edit?box=)
| [{host}.com/gui/edit?box=]({host}.com/gui/edit?box=)

| Edit / View |
|---|
| [localhost:4321/edit?box={container_name}](localhost:4321/edit?box={container_name})
| [{host}.com/gui/edit?box={container_name}]({host}.com/gui/edit?box={container_name})
</details>

<details>
<summary>View</summary>

| Item | Format |
|---|---|
| Name
| Tapes | 
| Hooks
| Bounds
| Variables
</details>


# 2. Attractor Editor


# 2. Chapter Editor

# 2. Log viewer
>- Individual report of
>- Formatting: Rows
<details>
<summary>URL Examples</summary>

| Generic |
|---|
| [localhost:4321/logs?log={log_name}](localhost:4321/logs?log={log_name})
| [{host}.com/gui/logs?log={log_name}]({host}.com/gui/logs?log={log_name})
</details>

- TBD
