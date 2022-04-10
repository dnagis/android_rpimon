#!/bin/sh

#lancement gst-launch-1.0 wrappe dans une enveloppe de respawn crash-proof

#ts --> mp4
#gst-launch-1.0 filesrc location=capture-08-22-12.ts ! tsdemux ! h264parse ! mp4mux ! filesink location=out.mp4
#
#gst-play-1.0 --videosink vaapisink out.mp4

pipeline_current () {
gst-launch-1.0 rpicamsrc bitrate=500000 annotation-mode=0x00000008 annotation-text-colour=0x000000 vflip=true hflip=true ! \
'video/x-h264, width=640, height=480, profile=high' ! h264parse ! mpegtsmux ! filesink location=/root/capture-`date '+%H-%M-%S'`.ts	
}

#systeme de respawn
#mimer un crash de gstreamer: kill -s KILL `pidof gst-launch-1.0` 

until pipeline_current
do
    logger "gst a crashe avec exit code = $?.  Respawning.."
    sleep 1
done

