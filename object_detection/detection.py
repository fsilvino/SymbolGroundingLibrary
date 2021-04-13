import _thread
import socket
import hashlib
import os
import pathlib
import numpy as np
import six.moves.urllib as urllib
import sys
import tarfile
import tensorflow as tf
import zipfile
import json

from collections import defaultdict
from io import StringIO
from matplotlib import pyplot as plt
from PIL import Image
from IPython.display import display
from random import randint

from object_detection.utils import ops as utils_ops
from object_detection.utils import label_map_util
from object_detection.utils import visualization_utils as vis_util

def sendAnswer(msg):
    tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    tcp.connect(('localhost', 49999))
    tcp.send(msg)
    tcp.close()

class ObjectDetection:

    def __init__(self):
        print('ObjectDetection init started')
        utils_ops.tf = tf.compat.v1
        tf.gfile = tf.io.gfile

        self.detectionsByImageHash = {}
        self.lastImagesHashes = {}

        dir_path = os.path.dirname(os.path.realpath(__file__))
        configFilePath = dir_path + "/config.json"
        print("Loading configuration from " + configFilePath)
        with open(configFilePath) as json_file:
            self.config = json.load(json_file)

        self.PATH_TO_LABELS = self.config["PATH_TO_LABELS"]
        print("PATH_TO_LABELS: " + self.PATH_TO_LABELS)

        self.PATH_TO_TEST_IMAGES_DIR = pathlib.Path(self.config["PATH_TO_TEST_IMAGES_DIR"])
        print("PATH_TO_TEST_IMAGES_DIR: " + self.config["PATH_TO_TEST_IMAGES_DIR"])

        self.model_name = self.config["MODEL_NAME"]
        print("MODEL_NAME: " + self.model_name)

        print('loading category index...')
        try:
            self.category_index = label_map_util.create_category_index_from_labelmap(self.PATH_TO_LABELS, use_display_name=True)
            print('category index loaded')
        except Exception as e:
            print('Failed loading category index', e)

        self.detection_model = None

    def load_model(self):
        print("loading model...")
        if self.detection_model != None:
            return

        base_url = self.config["MODEL_BASE_URL"]
        print("BASE_URL: " + base_url)

        model_file = self.model_name + '.tar.gz'
        model_origin = base_url + model_file
        print("Loading model from: " + model_origin)

        model_dir = tf.keras.utils.get_file(fname=self.model_name, origin=model_origin, untar=True)

        model_dir = pathlib.Path(model_dir)/"saved_model"

        model = tf.saved_model.load(str(model_dir))
        model = model.signatures['serving_default']

        self.detection_model = model
        print("model loaded.")
    

    def run_inference_for_single_image(self, model, image):
        image = np.asarray(image)
        # The input needs to be a tensor, convert it using `tf.convert_to_tensor`.
        input_tensor = tf.convert_to_tensor(image)
        # The model expects a batch of images, so add an axis with `tf.newaxis`.
        input_tensor = input_tensor[tf.newaxis,...]

        # Run inference
        output_dict = model(input_tensor)

        #print("Printing output_dict...")
        #print(output_dict)

        # All outputs are batches tensors.
        # Convert to numpy arrays, and take index [0] to remove the batch dimension.
        # We're only interested in the first num_detections.
        num_detections = int(output_dict.pop('num_detections'))
        output_dict = {key:value[0, :num_detections].numpy() 
                        for key,value in output_dict.items()}
        output_dict['num_detections'] = num_detections

        # detection_classes should be ints.
        output_dict['detection_classes'] = output_dict['detection_classes'].astype(np.int64)
        
        # Handle models with masks:
        if 'detection_masks' in output_dict:
            # Reframe the the bbox mask to the image size.
            detection_masks_reframed = utils_ops.reframe_box_masks_to_image_masks(
                    output_dict['detection_masks'], output_dict['detection_boxes'],
                    image.shape[0], image.shape[1])
            detection_masks_reframed = tf.cast(detection_masks_reframed > 0.5,
                                            tf.uint8)
            output_dict['detection_masks_reframed'] = detection_masks_reframed.numpy()
            
        return output_dict

    def deleteFile(self, file):
        if os.path.exists(file):
            os.remove(file)

    def show_inference(self, model, image_path, basePath, fileNumber):
        print("running detection...")
        # the array based representation of the image will be used later in order to prepare the
        # result image with boxes and labels on it.
        image_np = np.array(Image.open(image_path))
        # Actual detection.
        output_dict = self.run_inference_for_single_image(model, image_np)
        # Visualization of the results of a detection.
        
        vis_util.visualize_boxes_and_labels_on_image_array(
            image_np,
            output_dict['detection_boxes'],
            output_dict['detection_classes'],
            output_dict['detection_scores'],
            self.category_index,
            instance_masks=output_dict.get('detection_masks_reframed', None),
            use_normalized_coordinates=True,
            line_thickness=8)

        currentDetectionJson = []
        for i in range(output_dict['num_detections']):
            currentDetectionJson.append({
                'percept': 'detected',
                'score': output_dict['detection_scores'][i].astype(float),
                'class': self.category_index[output_dict['detection_classes'][i]],
                'box': output_dict['detection_boxes'][i].tolist()
            })
            
        imageResult = Image.fromarray(image_np)
        jsonName = basePath + f"agent.perception{fileNumber}.json"
        imageName = basePath + f"agent.perception{fileNumber}.jpg"

        self.deleteFile(jsonName)
        self.deleteFile(imageName)

        f = open(jsonName, "w")
        json.dump(currentDetectionJson, f, indent=4)
        f.close()
        
        imageResult.save(imageName, "JPEG")
        print("detection result saved.")
        return currentDetectionJson

    def calculateHash(self, inputFile):
        BUF_SIZE = 65536

        md5 = hashlib.md5()
        sha1 = hashlib.sha1()

        with open(inputFile, 'rb') as f:
            while True:
                data = f.read(BUF_SIZE)
                if not data:
                    break
                md5.update(data)
                sha1.update(data)

        return "{0}".format(md5.hexdigest())

    def listChangedImages(self):
        imagesPaths = list(map(lambda i: {"path": i, "hash": None}, sorted(list(self.PATH_TO_TEST_IMAGES_DIR.glob("*.jpg")))))
        newHashes = {}
        shouldDetect = False

        for img in imagesPaths:
            imgPath = img["path"]
            hash = self.calculateHash(imgPath)
            img["hash"] = hash
            newHashes[imgPath] = hash
            if (imgPath not in self.lastImagesHashes) or (self.lastImagesHashes[imgPath] != hash):
                # if there is at least one file to detect, run detection to all images for the sake of simplicity
                shouldDetect = True

        self.lastImagesHashes = newHashes

        if shouldDetect:
            return imagesPaths
        else:
            return []

    def detect(self, shouldAnswer):
        self.load_model()

        print('finding changed test images...')
        testImagePaths = self.listChangedImages()
        qtd = len(testImagePaths)
        print(f'{qtd} test images found')
        answerMsg = b'unchanged'
        if qtd > 0:

            basePath = self.config["RESULT_BASE_PATH"] + "/"

            if not os.path.exists(basePath):
                os.mkdir(basePath)

            detections = []

            for image in testImagePaths:
                image_path = image["path"]
                hash = image["hash"]
                if hash in self.detectionsByImageHash:
                    detections.append(self.detectionsByImageHash[hash]["detection_result"])
                else:
                    detection_result = self.show_inference(self.detection_model, image_path, basePath, len(detections))
                    self.detectionsByImageHash[hash] = { "hash": hash, "path": image["path"], "detection_result": detection_result }
                    detections.append(detection_result)

            finalDetectionResult = sum(detections, [])

            jsonName = basePath + f"agent.perception.json"

            f = open(jsonName, "w")
            json.dump(finalDetectionResult, f, indent=4)
            f.close()

            answerMsg = b'ok'

        if shouldAnswer:
            _thread.start_new_thread(sendAnswer, tuple([answerMsg]))

    