# PUT
## Create a new tape
```shell script
  curl --request PUT \
  --url http://0.0.0.0:4321/mock \
  --header 'mockTape_Name: Google' \
  --header 'mockTape_Only: true' \
  --header 'mockTape_Save: true' \
  --header 'mockTape_Url: http://google.com'
```

## Applying a mock to an existing tape (named google)
```shell script
curl --request PUT \
  --url http://0.0.0.0:4321/mock \
  --header 'Content-Type: application/json' \
  --header 'mockMethod: POST' \
  --header 'mockName: GetMail' \
  --header 'mockResponseCode: 200' \
  --header 'mockRoute_Path: /mail' \
  --header 'mockTape_Name: google' \
  --header 'mockUse: always' \
  --data '{\n    "Data": false\n}'
```
