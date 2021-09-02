import urllib.request
import json
import yaml

class translator():
    def __init__(self):
        with open('papago_data.yaml') as f:
            data = yaml.load(f, Loader=yaml.FullLoader)
        self.client_id = data['client_id']
        self.client_secret = data['client_secret']
        self.url = "https://naveropenapi.apigw.ntruss.com/nmt/v1/translation"

    def en2ko(self, en):
        data = "source=en&target=ko&text=" + en
        request = urllib.request.Request(self.url)
        request.add_header("X-NCP-APIGW-API-KEY-ID",self.client_id)
        request.add_header("X-NCP-APIGW-API-KEY",self.client_secret)
        
        response = urllib.request.urlopen(request, data=data.encode("utf-8"))

        rescode = response.getcode()
        if rescode == 200:
            response_body = json.loads(response.read())
            ko = response_body['message']['result']['translatedText']
            return ko
        else:
            print("Error Code:" + rescode)
            return en

    def ko2en(self, ko):
        data = "source=ko&target=en&text=" + ko
        request = urllib.request.Request(self.url)
        request.add_header("X-NCP-APIGW-API-KEY-ID",self.client_id)
        request.add_header("X-NCP-APIGW-API-KEY",self.client_secret)
        
        response = urllib.request.urlopen(request, data=data.encode("utf-8"))

        rescode = response.getcode()
        if rescode == 200:
            response_body = json.loads(response.read())
            en = response_body['message']['result']['translatedText']
            return en
        else:
            print("Error Code:" + rescode)
            return ko