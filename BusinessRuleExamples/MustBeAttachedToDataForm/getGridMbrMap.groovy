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

	// Initiate Map
	Map<String,Map<String,List<String>>> mapGridMbrs = [:]
	
	mapGridMbrs = getGridMbrMap(gridForm)
	println "Simple Request : getGridMbrMap(DataGrid) : $mapGridMbrs"
	
	println ""
	
	println "With Logger : getGridMbrMap(DataGrid, Boolean)"
	mapGridMbrs = getGridMbrMap(gridForm,true)
	
	println ""
	
	mapGridMbrs = getGridMbrMap(gridForm,{DataCell cell -> cell.isMissing()})
	println "With Custom Filter : getGridMbrMap(DataGrid, Predicate) - Filter is Missing Cells : $mapGridMbrs"
	
	println ""
	
	mapGridMbrs = getEditedMbrMap(gridForm)
	println "Only Edited Members : getEditedMbrMap(DataGrid) : $mapGridMbrs"
	
	
} catch (Exception e) {
	println e
}

logTimer(startTime, "Data Grid Map Examples")




