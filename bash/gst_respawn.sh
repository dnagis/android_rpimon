#!/bin/sh


until gst-launch-1.0 rpicamsrc bitrate=500000 annotation-mode=0x00000008 annotation-text-colour=0x000000 vflip=true hflip=true ! 'video/x-h264, width=640, height=480, profile=high' ! h264parse ! mpegtsmux ! filesink location=/root/capture-`date '+%H-%M-%S'`.ts
do
    logger "gst a crashe avec exit code = $?.  Respawning.."
    sleep 1
done

