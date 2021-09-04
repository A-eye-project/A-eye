from flask import Flask, request
import cv2
import numpy as np
from papago import translator

from Oscar_Scripts.encoder.vinvl_encoder import Encoder as VinVL_Encoder
from Oscar_Scripts.decoder.captioning_decoder import Decoder as Captioning_Decoder
from Oscar_Scripts.decoder.gqa_decoder import Decoder as GQA_Decoder
import torch
import json
import base64

from service_streamer import ThreadedStreamer

CAPTIONING_CHECKPOINT = '/workspace/Oscar/output/checkpoint-59-554820/'
GQA_CHECKPOINT = '/workspace/shared/gqa_output/checkpoint-9'
DEVICE = 'cuda'

vinvl_encoder = VinVL_Encoder()
captioning_decoder = Captioning_Decoder(CAPTIONING_CHECKPOINT)
gqa_decoder = GQA_Decoder(GQA_CHECKPOINT)
papago = translator()

app = Flask(__name__)

encode_streamer = ThreadedStreamer(vinvl_encoder.encode, batch_size = 4, max_latency = 0.3)
captioning_streamer = ThreadedStreamer(captioning_decoder.decode, batch_size = 4, max_latency = 0.1)
gqa_streamer = ThreadedStreamer(gqa_decoder.decode, batch_size = 4, max_latency = 0.1)

@app.route('/IC', methods = ['POST'])
def IC():
    file = request.get_data()
    img = cv2.imdecode(np.frombuffer(file, np.uint8), cv2.IMREAD_UNCHANGED)
    print("IC")
    out = encode_streamer.predict([img])
    caption = captioning_streamer.predict([out[0]])[0]
    print(caption)
    en = 'There is ' + caption
    ko = en
    ko =  papago.en2ko(en)
    print(ko)
    return ko


def get_answer(out):
    for temp in out:
        arr = []
        for value in temp.values():
            arr.append(value)
        arr = torch.nn.functional.softmax(torch.Tensor(arr), dim=0)
        argmax = arr.argmax().numpy().item()
        if arr[argmax].numpy().item() / arr.sum() > 0.7:
            return list(temp.keys())[argmax]
        else:
            return 0

@app.route('/VQA', methods = ['POST'])
def VQA():
    data = request.get_data()
    data = data.decode('cp949')
    
    data = json.loads(data)

    img = cv2.imdecode(np.frombuffer(base64.b64decode(data['Image']), np.uint8), cv2.IMREAD_UNCHANGED)
    question = data['Question']
    question = papago.ko2en(question)
    print(question)
    out = encode_streamer.predict([img])
    out[0].append(question)
    out = gqa_streamer.predict(out)
    print(out)
    answer = get_answer(out)
    if answer != 0:
        if 'color' in question:
            answer = answer + ' color'
        elif 'weather' in question:
            answer = answer + ' weather'
        ko = papago.en2ko(answer)
    else:
        ko = "알 수 없습니다."
    return ko

app.run(host='0.0.0.0', port=5121)



