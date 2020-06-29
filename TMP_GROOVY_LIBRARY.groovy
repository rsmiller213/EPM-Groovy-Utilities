/* **********************************************************
*
*	Globals
*
* **********************************************************/
class Globals{
	static Boolean debug = false
}
/* **********************************************************
*
*	Logging Functions
*
* **********************************************************/
def logTimer (long timeStart, String msg="Time Elapsed"){
	/**
	* Prints the time elapsed to job console between a provided start time and now formatted as hh:mm:ss.SSS
	* @param timeStart : The Starting Time
	* @param msg : Optional message to display
	*/
	long Elapsed = currentTimeMillis() - timeStart
	int ms = (int) (Elapsed % 1000)
	int s = ((int) (Elapsed / 1000)) % 60
	int m = ((int) (Elapsed / (1000*60))) % 60
	int h = ((int) (Elapsed / (1000*60*60))) % 24
	println "$msg : ${String.format("%02d:%02d:%02d.%03d", h,m,s,ms)}"
}


def logUser () {
	/**
	* Prints the User Running this Script to the job console
	*/
	println "Executing User : ${operation.user.fullName}"
}


def logRTPS () {
	/**
	* Prints the RTPS to the job console
	*/
	println '********************** BEGIN RTP PRINT **********************'
	println "Run Time Prompts : ${rtps}"
	println '********************** END RTP PRINT **********************'
}


def logGrid (DataGrid grid) {
	/**
	* Logs the entire DataGrid to the job console in a format to be copied/pasted into SmartView
	* @param grid : a DataGrid object that will be used
	*/
	def pov = grid.pov
	def cols = grid.columns
	def rows = grid.rows
	def rowDimCount = rows[0].headers.size()
	
	println '********************** BEGIN GRID PRINT **********************'

	// Print POV
	pov*.essbaseMbrName.each{ povMbrs ->
		// Account for Row Dims
		print ",".multiply(rowDimCount)
		// for the number of columns present in the grid, print the POV
		cols[0]*.essbaseMbrName.size().times{
			print "$povMbrs,"
		}
		
		//print line return
		println ''
	}
	
	// Print Column Headers
	for (int i=0; i < cols.size(); i++) {
		println "${','.multiply(rowDimCount)}${cols[i]*.essbaseMbrName.join(',')}"
	}
	
	// Print Row Headers + Data
	grid.rows.each{ row ->
		println "${row.headers*.essbaseMbrName.join(',')},${row.data*.data.join(',')}"
	}
	
	println '********************** END GRID PRINT **********************'
	
}


def logPrettyMap(Map root, int level=0){
	String pfx = " ".multiply(level * 3)
	//println "$pfx|ROOT : $root"
	root.each { key, val ->
		if (val instanceof Map) {
			println "$pfx" + "$key : "
			logPrettyMap((Map) val, level+1)
		} else if (val instanceof List) {
			List temp = val as List
			if(temp[0] instanceof Map) {
				println "$pfx" + "$key : "
				if (temp.size() == 1) {
					logPrettyMap((Map) temp[0], level+1)
				} else {
					temp.eachWithIndex{ mapVal, i ->
						println "$pfx   " + "Segment ${i+1} :"
						logPrettyMap((Map) mapVal, level+2)
					}
				}
			} else {
				println "$pfx" + "$key : $val"
			}
		} else {
			println "$pfx" + "$key : $val"
		}
	}
}

def logPrettyMap(List root, int level=0){
	String pfx = " ".multiply(level * 3)
	 root.eachWithIndex{ item, i ->
	 	if (item instanceof Map) {
		  	println "$pfx" + "Item ${i+1}"
		  	logPrettyMap((Map) item, level+1)
		  }
	 }
}



/* **********************************************************
*
*	Data Grid Functions
*
* **********************************************************/
Map<String,String> getPOVMap (DataGrid grid, boolean log=false) {
	/**
	* Will parse a data grid to get the POV Dimension / Members
	* @param grid : a DataGrid object that will be used to grab the POV from
	* @param log : will log the results to job console
	* @return : a map of the POV dimensions / members
	*/
	Map<String,String> mapPOV = [:]
	
	grid.pov.each{ povMbrs ->
		mapPOV[povMbrs.dimName] = povMbrs.mbrName
	}
	
	if (log || Globals.debug) {
		println '********************** BEGIN POV PRINT **********************'
		println "Logger : $log | Debug : ${Globals.debug}"
		println "POV : "
		mapPOV.each{key,value ->
			println "   $key : $value "
		}
		println '********************** END POV PRINT **********************'
	}
	return mapPOV
}


Map<String,Map<String,List<String>>> getGridMbrMap (DataGrid grid, Boolean log=false, Predicate pred = {true}) {
	/**
	* Will iterate through a grid and build a map of all the dimensions / members that fit the predicate (filter)
	* @param grid : a DataGrid object that will be used to grab the edited cells from
	* @param log : will log the results to job console
	* @return : a map of dimensions / members requested
	*/
	// Build Initial Map Object
	Map<String,Map<String,List<String>>> mapGridMbrs = [:]
	// Get the POV Dimensions of the Grid
	mapGridMbrs["pov"] = [:]
	grid.pov.each { povDims -> mapGridMbrs["pov"][povDims.dimName] = []}
	// Get the Column Dimensions of the Grid
	mapGridMbrs["cols"] = [:]
	grid.columns.each{ columns -> 
		columns[0].each{ col ->
			DataGrid.HeaderCell tempHdr = col as DataGrid.HeaderCell
			mapGridMbrs["cols"][tempHdr.dimName] = []
		}
	}
	// Get the Row Dimensions of the Grid
	mapGridMbrs["rows"] = [:]
	grid.rows[0].headers.each{ rowDims -> mapGridMbrs["rows"][rowDims.dimName] = []}
	
	// Build  Cell Map
	grid.dataCellIterator(pred).each{ cell ->
		["pov","cols","rows"].each { context ->
			mapGridMbrs[context].each{ dim, mbr ->
				List<String> lstValue = mapGridMbrs[context].get(dim,[])
				lstValue.add(cell.getMemberName(dim))
				mapGridMbrs[context][dim] = lstValue.unique()
			}
		}
		
	}

	// Write Results to Log
	if (log || Globals.debug) {
		println '********************** BEGIN GRID DIM PRINT **********************'
		println "Logger : $log | Debug : ${Globals.debug}"
		println 'Unique Cell Members : '
		["pov","cols","rows"].each { context ->
			println "   $context : "
			mapGridMbrs[context].each{ dim, mbr ->
				println "	  $dim : $mbr"
			}
		}
		//logPrettyMap(mapGridMbrs)
		println '********************** END GRID DIM PRINT **********************'
	}

	return mapGridMbrs
}
Map<String,Map<String,List<String>>> getGridMbrMap (DataGrid grid, Predicate pred) {
	return getGridMbrMap(grid,false,pred)
}
Map<String,Map<String,List<String>>> getEditedMbrMap (DataGrid grid, boolean log=false) {
	return getGridMbrMap(grid,log,{DataCell cell -> cell.edited})
}


DataGridDefinition getGridDefFromMap(Map gridMap, log=false, Map<String,Boolean> suppress=[:], Cube cube=rule.getCube()){
	/**
	* Will build a DataGridDefinition from a supplied map of type Map<String,Map<String,List<String>>>
	*	where the root map keys are [pov,cols,rows]
	* @param gridMap : a map object that defines the pov, cols, and rows
	* @param log : will log the results to job console
	* @param cube : a the cube to build the grid definition from
	* @param suppress : Suppression Options as Map<String,Boolean>, keys are : [suppCols,suppRows,suppRowsNative,suppBlocks]
	* @return : a grid definition to be used to build the grid
	*/

	// Check Params
	if(!suppress["suppCols"]) {suppress["suppCols"] = false}
	if(!suppress["suppRows"]) {suppress["suppRows"] = false}
	if(!suppress["suppRowsNative"]) {suppress["suppRowsNative"] = false}
	if(!suppress["suppBlocks"]) {suppress["suppBlocks"] = false}


	DataGridDefinitionBuilder bldr = cube.dataGridDefinitionBuilder()
	bldr.setSuppressMissingColumns(suppress["suppCols"])
	bldr.setSuppressMissingRows(suppress["suppRows"])
	bldr.setSuppressMissingRowsNative(suppress["suppRowsNative"])
	bldr.setSuppressMissingBlocks(suppress["suppBlocks"])
	// Add POV
	if (gridMap["pov"] instanceof Map) {
		bldr.addPov(gridMap["pov"].keySet() as List,gridMap["pov"].values() as List)
	} else if (gridMap["pov"] instanceof List) {
		List lstTemp = gridMap["pov"] as List
		Map mapTemp = lstTemp[0] as Map
		bldr.addPov(mapTemp.keySet() as List,mapTemp.values() as List)
	}
	// Add Cols
	if (gridMap["cols"] instanceof Map) {
		bldr.addColumn(gridMap["cols"].keySet() as List,gridMap["cols"].values() as List)
	} else if (gridMap["cols"] instanceof List) {
		List lstTemp = gridMap["cols"] as List
		lstTemp.each { item ->
			Map mapTemp = item as Map
			bldr.addColumn(mapTemp.keySet() as List,mapTemp.values() as List)
		}
	}
	// Add Rows
	if (gridMap["rows"] instanceof Map) {
		bldr.addRow(gridMap["rows"].keySet() as List,gridMap["rows"].values() as List)
	} else if (gridMap["rows"] instanceof List) {
		List lstTemp = gridMap["rows"] as List
		lstTemp.each { item ->
			Map mapTemp = item as Map
			bldr.addRow(mapTemp.keySet() as List, mapTemp.values() as List)
		}
	}
	DataGridDefinition dg = bldr.build()

	if (log || Globals.debug) {
		println '********************** BEGIN GRID DEF PRINT **********************'
		println "Logger : $log | Debug : ${Globals.debug}"
		println "Suppression : "
		println "   Suppress Columns : ${dg.isSuppressMissingColumns()}"
		println "   Suppress Rows : ${dg.isSuppressMissingRows()}"
		println "   Suppress Rows (Native) : ${dg.isSuppressMissingRowsNative()}"
		println "   Suppress Blocks : ${dg.isSuppressMissingBlocks()}"
		println "POV : "
		println "   ${dg.pov.getMembers().flatten()}"
		println "Columns : "
		dg.columns.each{ col ->
			println "   Segment : ${col.getMembers().flatten()}"
		}
		println "Rows : "
		dg.rows.each{ row ->
			println "   Segment : ${row.getMembers().flatten()}"
		}
		//println "   ${dg.rows*.getMembers().flatten()}"
		println '********************** END GRID DEF PRINT **********************'
	}

	return dg
}
DataGridDefinition getGridDefFromMap(Map<String,Map<String,List<String>>> gridMap, Map<String,Boolean> suppress, Cube cube=rule.getCube()){
	// Did not provide log param
	return getGridDefFromMap(gridMap,false,suppress,cube)
}
DataGridDefinition getGridDefFromMap(Map<String,Map<String,List<String>>> gridMap, Cube cube){
	//Did not provide log or suppress map params
	return getGridDefFromMap(gridMap,false,[:],cube)
}
DataGridDefinition getGridDefFromMap(Map<String,Map<String,List<String>>> gridMap, Boolean log, Cube cube){
	// did not provide suppress map param
	return getGridDefFromMap(gridMap,log,[:],cube)
}



/* **********************************************************
*
*	Metadata Functions
*
* **********************************************************/
List<Member> getStoredMbrs(List<Member> mbrs){
	/**
	* Will iterate through a list and return only the stored members
	* @param mbrs : a list of of level zero members
	* @return : a list of stored members
	*/
	List<Member> mbrReturn = []
	mbrs*.each{ Member mbr ->
		Map mbrCur = mbr.toMap()
		if(["never share","store"].contains(mbrCur["Data Storage"])) {
			mbrReturn << mbr
		}
	}

	return mbrReturn
}

List<Member> getStoredMbrs(List<String> mbrs, Dimension dim){
	/**
	* Will iterate through a list of Member Names and return only the stored members
	* @param mbrs : a list of strings of member names
	* @param dim : dimension the list belongs to
	* @param cube : the cube to build the list from
	* @return : a list of stored members as string
	*/
	List<Member> mbrReturn = []
	mbrs.each{ mbr ->
		Member mbrCur = dim.getMember(mbr)
		if(["never share","store"].contains(mbrCur.toMap()["Data Storage"])) {
			mbrReturn << mbrCur
		}
	}

	return mbrReturn
}