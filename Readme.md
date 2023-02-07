# Scala coding challenge

The responses would be too slow if the application processed the whole example file every time, so I decided to load
the data to PostgreSQL at server startup and serve the requests from there.
This causes the startup to be a bit slow but in return the responses are fast.

## Requirements
- docker
- docker-compose
- sbt

## How to run

Start the PostgreSQL docker container `amazon-reviews-postgres` with `./start.sh -d`. It will use port `25432`.

The web service can be started with the following command:
`sbt "run -f <path_to_json_file>"`

After the server is started it is able to process requests on `localhost:8080`

### Testing

There is an integration test which also uses a PostgreSQL docker container (`amazon-reviews-postgres-test`). This will use
port `35432`. The test container can be started with `./start.sh -t`

Both docker containers can be removed with `./stop.sh`

## Details

If the example file is not in the required format, the web service won't start and the program will exit.
With the larger example file, the setup process was ~1 minute.

It will only load data that is strictly required to calculate best rated products. The `overall` field is treated as an integer.
To avoid any conflict with the different example files, the PostgreSQL table is cleaned at startup.

#### Request:
- For every path other than `/amazon/best-rated` it returns 404 Not Found.
- It handles malformed and invalid requests.
- It can also return Internal server error for unexpected errors, e.g. postgres is not available.

#### Response:
For the example query the API will return:
`[{"asin":"B000JQ0JNS","average_rating":4.5},{"asin":"B000NI7RW8","average_rating":3.6666666666666667}]`

The precision for the second rating is different from the example in the challenge description. This is intended,
as the calculation is done by PostgreSQL. Even if `quill-jdbc-zio` uses BigDecimal for aggregation, it will return double precision.
A possible solution for this is returning the `sum` and `count` aggregations from postgres and calculating the average in the scala code.
The sorting would still be done by PostgreSQL, so that could result in inconsistencies.

### Improvements
These are not implemented but probably would be required in production:
- configure logging
- connection pool for PostgreSQL
- indices for the postges table to minimize sequential scans