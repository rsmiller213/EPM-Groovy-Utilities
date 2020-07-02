/*RTPS:*/
%Template(name:="TMP_GROOVY_LIBRARY")

long startTime = currentTimeMillis() // get current time in milliseconds

try {
	// Grab the Cube & Application
	Cube cube = rule.getCube()
	Application app = cube.getApplication()
	println "App : $app | Cube : $cube"

	// Get Grid from DataForm
	DataGrid gridForm = operation.getGrid()

	// Get Mbr Map from Data Grid
	Map<String,List<Map<String,List<String>>>> mapMapList = getEditedMbrMap(gridForm)

	println "Log a 2 Deep Nested Map with a List"
	logPrettyMap(mapMapList)

	println ""

	println "Log a 3 Deep Nested Map with a List"
	Map<String,Map<String,Map<String,List<String>>>> mapMapMapList = [:]
	mapMapMapList["MapOne"] = mapMapList
	mapMapMapList["MapTwo"] = mapMapList
	logPrettyMap(mapMapMapList)

	println ""

	println "Log a 4 Deep Nested Map with a List"
	Map<String,Map<String,Map<String,Map<String,List<String>>>>> mapMapMapMapList = [:]
	mapMapMapMapList["MapOne-One"] = mapMapMapList
	mapMapMapMapList["MapOne-Two"] = mapMapMapList
	mapMapMapMapList["MapTwo-One"] = mapMapMapList
	mapMapMapMapList["MapTwo-Two"] = mapMapMapList
	logPrettyMap(mapMapMapMapList)

	println ""

	println "Log a List with a 2 Deep Nested Map with a List"
	List lstMapMapList = []
	lstMapMapList << mapMapList
	lstMapMapList << mapMapList
	logPrettyMap(lstMapMapList)


} catch (Exception e) {
	println e
}

logTimer(startTime, "Data Grid Def Examples")




