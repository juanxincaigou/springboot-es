# -*- coding: utf-8 -*-


import os
import matplotlib.pyplot as plt 
import matplotlib.image as mpimg
import paddlehub as hub
import cv2
import sys


# path = sys.argv[1]
# test_img_path = os.listdir(path)
# os.chdir(path)

path = 'src/main/resources/static/img'
test_img_path = os.listdir(path)
os.chdir(path)

ocr = hub.Module(name="chinese_ocr_db_crnn_mobile")
np_images =[cv2.imread(image_path) for image_path in test_img_path]
results = ocr.recognize_text(
                    images=np_images,
                    use_gpu=False,
                    output_dir='ocr_result',
                    visualization=False,
                    box_thresh=0.5,
                    text_thresh=0.5)
# order = 1
for result in results:
    # print(order)
    # order = order + 1
    data = result['data']
    save_path = result['save_path']
    infomations = ''
    for infomation in data:
        infomations = infomations+" "+infomation['text']
        # print(infomation['text'], '\nconfidence: ', infomation['confidence'], '\ntext_box_position: ', infomation['text_box_position'])
    print(infomations)





# 服务端可以加载大模型，效果更好
# ocr = hub.Module(name="chinese_ocr_db_crnn_server")