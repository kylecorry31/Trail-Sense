import os
import sys
import time
import subprocess
from pathlib import Path
from watchdog.observers import Observer 
from watchdog.events import FileSystemEventHandler

class Watcher:

  DIRECTORY_TO_WATCH = os.path.join(os.getcwd(), "src")

  def __init__(self):
    self.observer = Observer()

  def run(self):
    event_handler = Handler()
    subprocess.call(["python", "build.py", "local_config.yaml"])
    self.observer.schedule(event_handler, self.DIRECTORY_TO_WATCH, recursive=True)
    self.observer.start()
    print("Watching for changes in " + self.DIRECTORY_TO_WATCH)
    try:
      while True:
        time.sleep(5)
    except:
      self.observer.stop()
      print("Error")
      self.observer.join()

class Handler(FileSystemEventHandler):
  
  def run_build():
    print("Starting build")
    subprocess.call(["python", "build.py", "local_config.yaml"])

  @staticmethod
  def on_any_event(event):
    if event.is_directory:
      return None
        
    path = Path(event.src_path)
    if path.is_file():
      if event.event_type == "modified" or event.event_type == "created" or event.event_type == "moved":
        print("Change detected in %s", event.src_path)
        Handler.run_build()

if __name__ == "__main__":
  subprocess.Popen(["python", "-m", "http.server", "8000", "--directory", "_site"])
  w = Watcher() 
  w.run()