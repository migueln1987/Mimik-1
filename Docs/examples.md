# PUT
## Basic
### Create a new tape<a name="basic_createtape" />
```shell script
  curl --request PUT \
  --url http://0.0.0.0:4321/mock \
  --header 'mockTape_Name: Google' \
  --header 'mockTape_Only: true' \
  --header 'mockTape_Url: http://google.com'
```
> Postman returns: Status: `201 (Created)` or `302 (Found)`

### Applying a mock<a name="basic_apply" />
To an existing tape (named google)
```shell script
curl --request PUT \
  --url http://0.0.0.0:4321/mock \
  --header 'mockTape_Name: google' \
  --header 'mockName: GetMail' \
  --header 'mockMethod: POST' \
  --header 'mockRoute_Path: /mail' \
  --header 'Content-Type: application/json' \
  --header 'mockResponseCode: 200' \
  --data '{\n    "Data": false\n}'
```
> Postman returns: Status: `201 (Created)` or `302 (Found)`

### Retrieving data<a name="basic_retrieve" />
```shell script
  curl --request POST \
  --url http://0.0.0.0:4321/mail
```
#### Prefaced with creating the companion mock
>Response: `200 (OK)`<br>
>Body: `{\n    "Data": false\n}`
#### Independently called
>Response: `404 (Not Found)`<br>
>Also: New tape is created with request
