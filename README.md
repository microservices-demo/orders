[![Build Status](https://travis-ci.org/microservices-demo/orders.svg?branch=master)](https://travis-ci.org/microservices-demo/orders) [![Coverage Status](https://coveralls.io/repos/github/microservices-demo/orders/badge.svg?branch=master)](https://coveralls.io/github/microservices-demo/orders?branch=master)
[![](https://images.microbadger.com/badges/image/weaveworksdemos/orders.svg)](http://microbadger.com/images/weaveworksdemos/orders "Get your own image badge on microbadger.com")

# orders
A microservices-demo service that provides ordering capabilities.

This build is built, tested and released by travis.

# Test
`./test/test.sh < python testing file >`. For example: `./test/test.sh unit.py`

# Build
`GROUP=weaveworksdemos COMMIT=test ./scripts/build.sh`

# Push
`GROUP=weaveworksdemos COMMIT=test ./scripts/push.sh`

## Redesign

This microservices will shortly go through a redesign to allow for
multi-step checkouts. A rough sketch of the flow is below:

![Orders flow](./Orders-flow.png)
