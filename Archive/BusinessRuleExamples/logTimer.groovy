/*RTPS:*/
%Template(name:="TMP_GROOVY_LIBRARY")

long startTime = currentTimeMillis() // get current time in milliseconds

sleep(3000) // sleep 3 seconds

logTimer(startTime)
logTimer(startTime, "Display Custom Message")
