import argparse
import sys
import unittest
import os
import urllib
from util.Api import Api
from time import sleep
from util.Docker import Docker
from util.Dredd import Dredd


class ServiceMock:
    container_name = ''
    hostname = ''

    def start_container(self):
        command = ['docker', 'run', '-d',
                   '--name', self.container_name,
                   '-h', self.container_name,
                   '-v', "{0}:{1}".format(os.getcwd(), "/data/"),
                   '-e', 'FLASK_APP=/data/test/json-server/server.py',
                   'weaveworksdemos/json-server',
                   '--port', '80']
        Docker().execute(command)
        sleep(2)

    def cleanup(self):
        Docker().kill_and_remove(self.container_name)

    def __init__(self, container_name, hostname):
        self.container_name = container_name
        self.hostname = hostname


class OrdersContainerTest(unittest.TestCase):
    TAG = "latest"
    COMMIT = ""
    container_name = Docker().random_container_name('orders')
    mongo_container_name = Docker().random_container_name('orders-db')

    def __init__(self, methodName='runTest'):
        super(OrdersContainerTest, self).__init__(methodName)
        self.users_mock = ServiceMock("users-orders-mock", "users-orders-mock")
        self.payment_mock = ServiceMock("payment", "payment")
        self.shipping_mock = ServiceMock("shipping", "shipping")
        self.ip = ""

    def setUp(self):
        self.users_mock.start_container()
        self.payment_mock.start_container()
        self.shipping_mock.start_container()
        Docker().start_container(container_name=self.mongo_container_name, image="mongo", host="orders-db")

        command = ['docker', 'run',
                   '-d',
                   '--name', OrdersContainerTest.container_name,
                   '-h', OrdersContainerTest.container_name,
                   '--link',
                   OrdersContainerTest.mongo_container_name,
                   '--link',
                   self.users_mock.container_name,
                   '--link',
                   self.payment_mock.container_name,
                   '--link',
                   self.shipping_mock.container_name,
                   'weaveworksdemos/orders:' + self.COMMIT]
        Docker().execute(command, dump_streams=True)
        self.ip = Docker().get_container_ip(OrdersContainerTest.container_name)

    def tearDown(self):
        Docker().kill_and_remove(OrdersContainerTest.container_name)
        Docker().kill_and_remove(OrdersContainerTest.mongo_container_name)
        self.users_mock.cleanup()
        self.payment_mock.cleanup()
        self.shipping_mock.cleanup()

    def test_api_validated(self):
        limit = 30
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
