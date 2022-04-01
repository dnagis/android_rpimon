#!/bin/sh

#sysinit:
#socat -t 10 TCP-LISTEN:4696,fork EXEC:/root/rpimon.sh &
#envoi pour tests depuis linux:
#echo start | socat - TCP:192.168.1.19:4696 

STDIN=$(cat) #Recuperation du message envoye par l emetteur

case "$STDIN" in
 start)
    echo `date` start >> /root/LOG_rpimon
    kill `pidof test-launch`
    gst-launch-1.0 -e rpicamsrc bitrate=500000 annotation-mode=0x00000008 annotation-text-colour=0x000000 vflip=true hflip=true ! \
 'video/x-h264, width=640, height=480, profile=high' ! h264parse ! mp4mux ! filesink location=/root/capture.mp4    
    ;;
 stop)
    echo `date` stop >> /root/LOG_rpimon    
    kill -s SIGINT `pidof gst-launch-1.0`
    while pgrep -x "gst-launch-1.0" > /dev/null
    do
      echo `date` un passage while >> /root/LOG_rpimon
      sleep 1
    done
    echo `date` apres while >> /root/LOG_rpimon
    poweroff
    ;;  
 *)
    echo $STDIN >> /root/LOG_rpimon
esac


