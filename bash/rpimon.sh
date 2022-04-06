#!/bin/sh

#sysinit:
#socat -t 10 TCP-LISTEN:4696,fork EXEC:/root/rpimon.sh &
#envoi pour tests depuis linux:
#echo start | socat - TCP:192.168.1.19:4696 

STDIN=$(cat) #Recuperation du message envoye par l emetteur

case "$STDIN" in
 start)
    logger start_rpimon
    kill `pidof test-launch`
    /root/gst_respawn.sh &
    ;;
 stop)
    logger stop_rpimon   
    kill -s SIGINT `pidof gst_respawn.sh`
    poweroff
    ;;  
 *)
    echo $STDIN >> /root/LOG_rpimon
esac


