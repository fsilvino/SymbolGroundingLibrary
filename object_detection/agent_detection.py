import os
import pathlib
from pathlib import Path
import numpy as np
import os
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

"""Import the object detection module."""

from object_detection.utils import ops as utils_ops
from object_detection.utils import label_map_util
from object_detection.utils import visualization_utils as vis_util

"""Patches:"""

# patch tf1 into `utils.ops`
utils_ops.tf = tf.compat.v1

# Patch the location of gfile
tf.gfile = tf.io.gfile

"""# Model preparation

## Variables

Any model exported using the `export_inference_graph.py` tool can be loaded here simply by changing the path.

By default we use an "SSD with Mobilenet" model here. See the [detection model zoo](https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/tf1_detection_zoo.md) for a list of other models that can be run out-of-the-box with varying speeds and accuracies.

## Loader
"""

def load_model(model_name):
  base_url = 'http://download.tensorflow.org/models/object_detection/'
  model_file = model_name + '.tar.gz'
  model_dir = tf.keras.utils.get_file(fname=model_name, origin=base_url + model_file, untar=True)

  model_dir = pathlib.Path(model_dir)/"saved_model"

  model = tf.saved_model.load(str(model_dir))
  model = model.signatures['serving_default']

  return model

"""## Loading label map
Label maps map indices to category names, so that when our convolution network predicts `5`, we know that this corresponds to `airplane`.  Here we use internal utility functions, but anything that returns a dictionary mapping integers to appropriate string labels would be fine
"""

# List of the strings that is used to add correct label for each box.
#PATH_TO_LABELS = 'models/research/object_detection/data/mscoco_label_map.pbtxt'
PATH_TO_LABELS = 'models/research/object_detection/data/oid_v4_label_map.pbtxt'
#model_name = 'ssd_mobilenet_v1_coco_2017_11_17'
#model_name = 'ssd_resnet101_v1_fpn_shared_box_predictor_oid_512x512_sync_2019_01_20'
#model_name = 'faster_rcnn_inception_resnet_v2_atrous_oid_2018_01_28'
#model_name = 'faster_rcnn_resnet50_coco_2018_01_28'
#model_name = 'facessd_mobilenet_v2_quantized_320x320_open_image_v4'
model_name = 'faster_rcnn_inception_resnet_v2_atrous_oid_v4_2018_12_12'

category_index = label_map_util.create_category_index_from_labelmap(PATH_TO_LABELS, use_display_name=True)

"""For the sake of simplicity we will test on 2 images:"""

# If you want to test the code with your images, just add path to the images to the TEST_IMAGE_PATHS.
#PATH_TO_TEST_IMAGES_DIR = pathlib.Path('models/research/object_detection/test_images')
PATH_TO_TEST_IMAGES_DIR = pathlib.Path('D:\\GitHub\\tcc\\teste\\imagens-teste')
TEST_IMAGE_PATHS = sorted(list(PATH_TO_TEST_IMAGES_DIR.glob("*.jpg")))

"""# Detection

Load an object detection model:
"""

detection_model = load_model(model_name)

"""Check the model's input signature, it expects a batch of 3-color images of type uint8:"""


"""And retuns several outputs:"""

#detection_model.output_dtypes

#detection_model.output_shapes

"""Add a wrapper function to call the model, and cleanup the outputs:"""

def run_inference_for_single_image(model, image):
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

"""Run it on each test image and show the results:"""

def deleteFile(file):
  if os.path.exists(file):
    os.remove(file)

def show_inference(model, image_path):
  # the array based representation of the image will be used later in order to prepare the
  # result image with boxes and labels on it.
  image_np = np.array(Image.open(image_path))
  # Actual detection.
  output_dict = run_inference_for_single_image(model, image_np)
  # Visualization of the results of a detection.
  
  vis_util.visualize_boxes_and_labels_on_image_array(
      image_np,
      output_dict['detection_boxes'],
      output_dict['detection_classes'],
      output_dict['detection_scores'],
      category_index,
      instance_masks=output_dict.get('detection_masks_reframed', None),
      use_normalized_coordinates=True,
      line_thickness=8)

  detectionJson = []

  for i in range(output_dict['num_detections']):
    detectionJson.append({
      'score': output_dict['detection_scores'][i].astype(float),
      'class': category_index[output_dict['detection_classes'][i]],
      'box': output_dict['detection_boxes'][i].tolist()
    })
    
  imageResult = Image.fromarray(image_np)
  
  basePath = "D:\\GitHub\\tcc\\teste\\resultado-deteccao\\"
  jsonName = basePath + "agent.perception.json"
  imageName = basePath + "agent.perception.jpg"

  deleteFile(jsonName)
  deleteFile(imageName)

  f = open(jsonName, "w")
  json.dump(detectionJson, f, indent=4)
  f.close()
  
  imageResult.save(imageName, "JPEG")

  #display(imageResult)

for image_path in TEST_IMAGE_PATHS:
  show_inference(detection_model, image_path)


"""## Instance Segmentation"""

# model_name = "mask_rcnn_inception_resnet_v2_atrous_coco_2018_01_28"
# masking_model = load_model("mask_rcnn_inception_resnet_v2_atrous_coco_2018_01_28")

"""The instance segmentation model includes a `detection_masks` output:"""

# masking_model.output_shapes

# for image_path in TEST_IMAGE_PATHS:
#  show_inference(masking_model, image_path)