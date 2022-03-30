#!/bin/sh

STDIN=$(cat) #Recuperation du message envoye par l emetteur

case "$STDIN" in
 start)
    kill `pidof test-launch`
    gst-launch-1.0 -e rpicamsrc bitrate=500000 annotation-mode=0x00000008 annotation-text-colour=0x000000 vflip=true hflip=true ! \
 'video/x-h264, width=640, height=480, profile=high' ! h264parse ! mp4mux ! filesink location=/root/capture.mp4    
    ;;
 stop)
    kill -s SIGINT `pidof gst-launch-1.0`
    echo `date` on vient de kill >> /root/LOG_Rx
    while pgrep -x "gst-launch-1.0" > /dev/null
    do
      echo `date` un passage while >> /root/LOG_Rx
      sleep 1
    done
    echo `date` apres while >> /root/LOG_Rx
#    poweroff
    ;;  
 *)
    echo $STDIN >> /root/LOG_Rx
esac


