import os
import logging

logging.basicConfig(level=logging.DEBUG)


# get a filename from the resource system (relative to)
def resource_filename(file):
    return os.path.join(os.path.join(os.path.dirname(__file__), 'res'), file)
