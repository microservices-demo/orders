import requests

class Api:
    def noResponse(self, url):
        try:
            r = requests.get(url, timeout=5)
        except requests.exceptions.ConnectionError:
            return True
        except requests.exceptions.ReadTimeout:
            return True
        return False
