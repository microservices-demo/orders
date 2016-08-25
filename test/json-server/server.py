from flask import Flask, url_for, jsonify, make_response

app = Flask(__name__)

@app.route('/customers/<custid>')
def api_cusutomers(custid):
    resp = make_response(jsonify({ "id": custid,
	  "firstName": "Test",
	  "lastName": "Test",
	  "username": "testymctestface"
	})
    )
    resp.headers['Content-Type'] = 'application/hal+json'
    return resp

@app.route('/cards/<custid>')
def api_cards(custid):
    resp = make_response(jsonify({
	    "id": custid,
	    "longNum": "23232*****2131",
	    "expires": "12/18",
	    "ccv": "940"
	}))
    resp.headers['Content-Type'] = 'application/hal+json'
    return resp


@app.route('/addresses/<custid>')
def api_addresses(custid):
    resp = make_response(jsonify({
	    "id": custid,
	    "number": "12",
	    "street": "Cleverstreet",
	    "city": "Tinytown",
	    "postcode": "1923eq",
	    "country": "Cambodia"
	}))
    resp.headers['Content-Type'] = 'application/hal+json'
    return resp

@app.route('/carts/<custid>')
def api_carts(custid):
    resp = make_response(jsonify({
	    "id": custid
	}))
    resp.headers['Content-Type'] = 'application/hal+json'
    return resp


@app.route('/carts/<custid>/items')
def api_carts_items(custid):
    resp = make_response(jsonify(
        [{
            "id": custid,
            "quantity": 10,
            "unitPrice": 1.99,
            "itemId": "abc123"
        }]))
    resp.headers['Content-Type'] = 'application/json'
    return resp

@app.route('/paymentAuth', methods=['POST'])
def api_payment_auth():
    resp = make_response(jsonify(
        {
            "authorised": True
        }))
    resp.headers['Content-Type'] = 'application/json'
    return resp
