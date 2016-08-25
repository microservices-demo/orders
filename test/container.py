import argparse
import sys
import unittest
import os
import urllib
from util.Api import Api
from time import sleep

from util.Docker import Docker
from util.Dredd import Dredd

class AccountsMock:
    container_name = 'accounts-orders-mock'
    mock_file = "https://raw.githubusercontent.com/microservices-demo/user/15f7830fcf0a24bfa89f01622884192a245719a9/apispec/mock.json"
    mock_local = "accounts-mock.json"
    def start_container(self):
        urllib.request.urlretrieve(self.mock_file, self.mock_local)
        command = ['docker', 'run',
                   '-d',
                   '--name', self.container_name,
                   '-w', os.getcwd(),
                   '-h', self.container_name,
                   '-v', "{0}:{1}".format(os.getcwd(), "/data/"),
                   'clue/json-server',
                   '/data/' + self.mock_local]
        Docker().execute(command)
        sleep(2)

    def cleanup(self):
        Docker().kill_and_remove(self.container_name)

class CartsMock:
    container_name = 'carts-orders-mock'
    mock_file = "https://raw.githubusercontent.com/microservices-demo/carts/75e6a963b7187be750c2ec36c56c0df64a2488e4/api-spec/mock.json"
    mock_local = "carts-mock.json"
    routes_file = "https://raw.githubusercontent.com/microservices-demo/carts/75e6a963b7187be750c2ec36c56c0df64a2488e4/api-spec/routes.json"
    routes_local = "carts-routes.json"
    def start_container(self):
        urllib.request.urlretrieve(self.mock_file, self.mock_local)
        urllib.request.urlretrieve(self.routes_file, self.routes_local)
        command = ['docker', 'run',
                   '-d',
                   '--name', self.container_name,
                   '-h', self.container_name,
                   '-w', os.getcwd(),
                   '-v', "{0}:{1}".format(os.getcwd(), "/data"),
                   'clue/json-server',
                   '/data/' + self.mock_local,
                   '--routes', '/data/' + self.routes_local]
        Docker().execute(command)
        sleep(2)

    def cleanup(self):
        Docker().kill_and_remove(self.container_name)


class OrdersContainerTest(unittest.TestCase):
    TAG = "latest"
    COMMIT = ""
    container_name = Docker().random_container_name('orders')
    mongo_container_name = Docker().random_container_name('orders-db')
    
    def __init__(self, methodName='runTest'):
        super(OrdersContainerTest, self).__init__(methodName)
        self.accounts_mock = AccountsMock()
        self.carts_mock = CartsMock()
        self.ip = ""
        
    def setUp(self):
        self.accounts_mock.start_container()
        self.carts_mock.start_container()
        Docker().start_container(container_name=self.mongo_container_name, image="mongo", host="orders-db")
        
        command = ['docker', 'run',
                   '-d',
                   '--name', OrdersContainerTest.container_name,
                   '-h', OrdersContainerTest.container_name,
                   '--link',
                   OrdersContainerTest.mongo_container_name,
                   '--link',
                   self.accounts_mock.container_name,
                   '--link',
                   self.carts_mock.container_name,
                   'weaveworksdemos/orders:' + self.COMMIT]
        Docker().execute(command)
        self.ip = Docker().get_container_ip(OrdersContainerTest.container_name)

    def tearDown(self):
        pass
        # Docker().kill_and_remove(OrdersContainerTest.container_name)
        # Docker().kill_and_remove(OrdersContainerTest.mongo_container_name)
        # self.accounts_mock.cleanup()
        # self.carts_mock.cleanup()

    def test_api_validated(self):
        limit = 20
        while Api().noResponse('http://' + self.ip + ':80/orders'):
            if limit == 0:
                self.fail("Couldn't get the API running")
            limit = limit - 1
            sleep(1)
        
        out = Dredd().test_against_endpoint(
            "orders", 'http://' + self.ip + ':80/',
            links=[self.mongo_container_name, self.container_name],
            env=[("MONGO_ENDPOINT", "mongodb://orders-db:27017/data")],
            dump_streams=True)
        self.assertGreater(out.find("0 failing"), -1)
        self.assertGreater(out.find("0 errors"), -1)
        print(out)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    default_tag = "latest"
    parser.add_argument('--tag', default=default_tag, help='The tag of the image to use. (default: latest)')
    parser.add_argument('unittest_args', nargs='*')
    args = parser.parse_args()
    OrdersContainerTest.TAG = args.tag

    if OrdersContainerTest.TAG == "":
        OrdersContainerTest.TAG = default_tag

    OrdersContainerTest.COMMIT = os.environ["COMMIT"]   
    # Now set the sys.argv to the unittest_args (leaving sys.argv[0] alone)
    sys.argv[1:] = args.unittest_args
    unittest.main()
