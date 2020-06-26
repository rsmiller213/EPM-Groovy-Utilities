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
	Map<String,Map<String,List<String>>> mapGridMbrs = getEditedMbrMap(gridForm)
	
    // Map Before
    println "Map Before : $mapGridMbrs"
    // Update & Add Period to Map
    ["Jul","Aug","Sep"].each { per ->
    	mapGridMbrs["cols"]["Period"] << per
    }
    println "Map After Adding Dec : $mapGridMbrs"

    DataGridDefinition gdForm 
    DataGrid gridNew
    
    println "===== Simple Request with Logger"
    gdForm = getGridDefFromMap(mapGridMbrs,true)
    
    println ""
    
    // Load & Log New Grid
    gridNew = cube.loadGrid(gdForm, false)
    logGrid(gridNew)
    
    println ""
    
	println "===== Using Suppression Options"
    gdForm = getGridDefFromMap(mapGridMbrs,true,[suppCols:true])
    
    println ""
    
    // Load & Log New Grid
    gridNew = cube.loadGrid(gdForm, false)
    logGrid(gridNew)
    
} catch (Exception e) {
	println e
} finally {
	gridNew.close()
}

logTimer(startTime, "Data Grid Def Examples")