#!/bin/sh

date

gst-launch-1.0 -e rpicamsrc bitrate=500000 annotation-mode=0x00000008 annotation-text-colour=0x000000 vflip=true hflip=true ! 'video/x-h264, width=640, height=480, profile=high' ! h264parse ! mp4mux ! filesink location=/root/capture.mp4 &> /dev/null &
echo 'capture started, resultat pgrep:'
pgrep gst-launch-1.0
sleep 5
echo 'fin de la capture, on kill'
kill -s SIGINT `pidof gst-launch-1.0`

while pgrep -x "gst-launch-1.0" > /dev/null; do
      echo "gst-launch-1.0 encore running... on attends un peu..."
      sleep 1
done  

echo "gst-launch-1.0 a l air killed, on pourrait poweroff tranquille"


