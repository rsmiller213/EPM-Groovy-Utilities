/* **********************************************************
*
*    Globals
*
* **********************************************************/
class Globals{
    static Boolean debug = false
    static String connLocalEPM = "EPM_API"
    static String defImportMode = "REPLACE"
    static String defExportMode = "STORE_DATA"
}
/* **********************************************************
*
*    Logging Functions
*
* **********************************************************/
def logTimer (long timeStart, String msg="Time Elapsed"){
    /**
    * Prints the time elapsed to job console between a provided start time and now formatted as hh:mm:ss.SSS
    * @param timeStart : The Starting Time
    * @param msg : Optional message to display
    */
    long elapsed = currentTimeMillis() - timeStart
    int ms = (int) (elapsed % 1000)
    int s = ((int) (elapsed / 1000)) % 60
    int m = ((int) (elapsed / (1000*60))) % 60
    int h = ((int) (elapsed / (1000*60*60))) % 24
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
    pov*.essbaseMbrName.each{povMbrs ->
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
            } else { println "$pfx" + "$key : $val" }
        } else { println "$pfx" + "$key : $val"}
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
*    Data Grid Functions
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

Map<String,List<Map<String,List<String>>>> getGridMbrMap (DataGrid grid,
                                                            Boolean log=false,
                                                            Predicate pred = {true})
{
    /**
    * Will iterate through a grid and build a map of all the dimensions / members that fit the predicate (filter)
    * @param grid : a DataGrid object that will be used to grab the edited cells from
    * @param log : will log the results to job console
    * @return : a map of dimensions / members requested
    */
    // Build Initial Map Object
    Map<String,List<Map<String,List<String>>>> mapGrid = [:]
    Map mapTemp = [:]
    List lstTemp = []
    // Get the POV Dimensions of the Grid
    mapGrid["pov"] = []
    grid.pov.each { povDims -> mapTemp[povDims.dimName] = []}
    mapGrid["pov"] << mapTemp
    // Get the Column Dimensions of the Grid
    mapTemp = [:]
    mapGrid["cols"] = []
    grid.columns.each{ columns ->
        columns[0].each{ col ->
            DataGrid.HeaderCell tempHdr = col as DataGrid.HeaderCell
            mapTemp[tempHdr.dimName] = []
        }
    }
    mapGrid["cols"] << mapTemp
    // Get the Row Dimensions of the Grid
    mapGrid["rows"] = []
    mapTemp = [:]
    grid.rows[0].headers.each{ rowDims -> mapTemp[rowDims.dimName] = []}
    mapGrid["rows"] << mapTemp
    // Build  Cell Map
    grid.dataCellIterator(pred).each{ cell ->
        // POV
        mapGrid["pov"][0].each { dim, mbr ->
            lstTemp = mapGrid["pov"][0].get(dim,[])
            lstTemp.add(cell.getMemberName(dim))
            mapGrid["pov"][0][dim] = lstTemp.unique()
        }
        ["cols","rows"].each{ context ->
            Map tempMap = [:]
            mapGrid[context][0].each {dim, mbr ->
                // get a map of all the dims/members for the context of this cell
                tempMap[dim] = cell.getMemberName(dim)
            }
            mapGrid[context] << tempMap
            mapGrid[context] = mapGrid[context].unique()
            //println "$context : ($iNumOfDims) $tempMap"
        }
    }

    // Drop initial col / map as it is blank
    mapGrid["cols"] = mapGrid["cols"].drop(1)
    mapGrid["rows"] = mapGrid["rows"].drop(1)

    // Split into logical segments by grouping the cols / rows
    ["cols","rows"].each{context ->
        // Grab list of dimensions in this context
        List lstDims = mapGrid[context][0].keySet() as List
        List<Map<String,List<String>>> lstOut = []
        // Group by all dimensions up to the inner most
        def result = mapGrid[context].groupBy{ o -> lstDims.dropRight(1).collect{o[it]}}.values()

        result.each { item ->
            lstOut << item*.keySet().flatten().unique().collectEntries{
                [(it): item*.get(it).findAll().flatten().unique()]
            }
        }
        // now we have grouped through all but the inner most dimension, regroup again just for the inner most
        result = lstOut.groupBy{o -> lstDims.takeRight(1).collect{o[it]}}.values()
        lstOut = []
        result.each { item ->
            lstOut << item*.keySet().flatten().unique().collectEntries{
                [(it): item*.get(it).findAll().flatten().unique()]
            }
        }
        // write it back into the context map
        mapGrid[context] = lstOut
    }

    // Write Results to Log
    if (log || Globals.debug) {
        println '********************** BEGIN GRID DIM PRINT **********************'
        println "Logger : $log | Debug : ${Globals.debug}"
        println 'Unique Cell Members : '
        logPrettyMap(mapGrid,1)
        println '********************** END GRID DIM PRINT **********************'
    }

    return mapGrid
}
Map<String,List<Map<String,List<String>>>> getGridMbrMap (DataGrid grid, Predicate pred) {
    return getGridMbrMap(grid,false,pred)
}
Map<String,List<Map<String,List<String>>>> getEditedMbrMap (DataGrid grid, boolean log=false) {
    return getGridMbrMap(grid,log,{DataCell cell -> cell.edited})
}

DataGridDefinition getGridDefFromMap(Map mapGrid, log=false,
                                        Map<String,Boolean> suppress=[:],
                                        Cube cube=rule.getCube())
{
    /**
    * Will build a DataGridDefinition from a supplied map of type Map<String,Map<String,List<String>>>
    *    where the root map keys are [pov,cols,rows]
    * @param mapGrid : a map object that defines the pov, cols, and rows
    * @param log : will log the results to job console
    * @param cube : a the cube to build the grid definition from
    * @param suppress : Suppression Options as Map<String,Boolean>, keys are :
                            [suppCols,suppRows,suppRowsNative,suppBlocks]
    * @return : a grid definition to be used to build the grid
    */

    // Check Params
    if(!suppress["suppCols"]) {suppress["suppCols"] = true}
    if(!suppress["suppRows"]) {suppress["suppRows"] = faltruese}
    if(!suppress["suppRowsNative"]) {suppress["suppRowsNative"] = true}
    if(!suppress["suppBlocks"]) {suppress["suppBlocks"] = true}

    DataGridDefinitionBuilder bldr = cube.dataGridDefinitionBuilder()
    bldr.setSuppressMissingColumns(suppress["suppCols"])
    bldr.setSuppressMissingRows(suppress["suppRows"])
    bldr.setSuppressMissingRowsNative(suppress["suppRowsNative"])
    bldr.setSuppressMissingBlocks(suppress["suppBlocks"])
    // Add POV
    if (mapGrid["pov"] instanceof Map) {
        bldr.addPov(mapGrid["pov"].keySet() as List,mapGrid["pov"].values() as List)
    } else if (mapGrid["pov"] instanceof List) {
        List lstTemp = mapGrid["pov"] as List
        Map mapTemp = lstTemp[0] as Map
        bldr.addPov(mapTemp.keySet() as List,mapTemp.values() as List)
    }
    // Add Cols
    if (mapGrid["cols"] instanceof Map) {
        bldr.addColumn(mapGrid["cols"].keySet() as List,mapGrid["cols"].values() as List)
    } else if (mapGrid["cols"] instanceof List) {
        List lstTemp = mapGrid["cols"] as List
        lstTemp.each { item ->
            Map mapTemp = item as Map
            bldr.addColumn(mapTemp.keySet() as List,mapTemp.values() as List)
        }
    }
    // Add Rows
    if (mapGrid["rows"] instanceof Map) {
        bldr.addRow(mapGrid["rows"].keySet() as List,mapGrid["rows"].values() as List)
    } else if (mapGrid["rows"] instanceof List) {
        List lstTemp = mapGrid["rows"] as List
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
        println '********************** END GRID DEF PRINT **********************'
    }

    return dg
}
DataGridDefinition getGridDefFromMap(Map<String,Map<String,List<String>>> mapGrid,
                                        Map<String,Boolean> suppress,
                                        Cube cube=rule.getCube())
{
    // Did not provide log param
    return getGridDefFromMap(mapGrid,false,suppress,cube)
}
DataGridDefinition getGridDefFromMap(Map<String,Map<String,List<String>>> mapGrid, Cube cube){
    //Did not provide log or suppress map params
    return getGridDefFromMap(mapGrid,false,[:],cube)
}
DataGridDefinition getGridDefFromMap(Map<String,Map<String,List<String>>> mapGrid, Boolean log, Cube cube){
    // did not provide suppress map param
    return getGridDefFromMap(mapGrid,log,[:],cube)
}

/* **********************************************************
*
*    Metadata Functions
*
* **********************************************************/
List<Member> getStoredMbrs(List<Member> mbrs){
	/**
    * Will iterate through a list and return only the stored members
    * @param mbrs : a list of of level zero members
    * @return : a list of stored members
    */
    List<Member> mbrReturn = []
    List<String> mbrIgnore = ["dynamic calc","shared"]
    //def sAccounts = mdxParams(app.getDimension('Account').getEvaluatedMembers("Lvl0Descendants(Account)" ,cube).findAll{!ignoreMbrs.contains(it.toMap()["Data Storage".toString()])}*.getName(MemberNameType.ESSBASE_NAME))
    mbrReturn = mbrs.findAll{!mbrIgnore.contains(it.toMap()["Data Storage".toString()])}
    /*mbrs*.each{ Member mbr ->
        Map mbrCur = mbr.toMap()
        if(["never share","store"].contains(mbrCur["Data Storage"])) {
            mbrReturn << mbr
        }
    }*/

    return mbrReturn
}

List<Member> getStoredMbrs(List<String> mbrs, Dimension dim){
    /**
    * Will iterate through a list of Member Names and return only the stored members
    * @param mbrs : a list of strings of member names
    * @param dim : dimension the list belongs to
    * @param cube : the cube to build the list from
    * @return : a list of stored members as members
    */
    List<Member> mbrReturn = []
    mbrs.each{ mbr ->
        Member mbrCur = dim.getMember(mbr)
        if(["never share","store"].contains(mbrCur.toMap()["Data Storage"])) { mbrReturn << mbrCur }
    }

    return mbrReturn
}

/* **********************************************************
*
*    MDX Functions
*
* **********************************************************/

String convertMDXFunctions(String mdx){

    def matches
    // Matches anything like '[*Descendants(*)]'
    def pDescendants = "(?i)\\[\\w*Descendants\\((.*?)\\)\\]"
    matches = (mdx =~ "$pDescendants").findAll()

    //TODO : Add other functions

    matches.each{ fnc, mbr ->
        String result
        switch (fnc){
            case ~"(?i)\\[IDescendants.*" :
                result = "{Descendants([$mbr],SELF)}"
                break
            case ~"(?i)\\[ILvl0Descendants.*" :
                result = "{Descendants([$mbr],[$mbr].dimension.levels(0))}"
                break
            case ~"(?i)\\[Descendants.*" :
                result = "{Descendants([$mbr])}"
                break
            default:
                result = fnc
        }
        mdx = mdx.replace((String)fnc,result)
    }
    return mdx
}

/* **********************************************************
*
*    API Functions
*
* **********************************************************/

class API {
    String connName
    Connection conn
    EpmScript script
    Application app
    Map<String,String> baseURLs

    // Constructor
    API(EpmScript script){
        this.script = script
        this.app = this.script.rule.cube.application
        this.connName = Globals.connLocalEPM
        this.conn = this.app.getConnection(connName)
        this.baseURLs = this.getBaseURLs()
    }
    API(EpmScript script, Connection conn){
        this.script = script
        this.conn = conn
        this.connName = this.conn.getName()
        this.baseURLs = this.getBaseURLs()
    }
    API(EpmScript script, String connName){
        this.script = script
        this.connName = connName
        this.app = this.script.rule.cube.application
        this.conn = this.app.getConnection(connName)
        this.baseURLs = this.getBaseURLs()
    }

    // Methods
    Map get(String url){
        HttpResponse<String> r = conn.get("$url").asString()
        if(!(200..299).contains(r.status)) {
            script.throwVetoException("Error ($r.status) : $r.statusText")
        } else {
            return ((Map) new JsonSlurper().parseText(r.body))
        }
    }

    Map<String,String> getBaseURLs() {
        Map res = [:]
        Map rMap = [:]
        String url = ""
        
        // Get Planning API URL
        url = '/HyperionPlanning/rest'
        rMap = get(url)
        rMap['items'].each{ item ->
            if (item['isLatest']) {
                res['PLN'] = "$url/${item['version']}"
            }
        }
        // Get Data Management API URL
        url = '/aif/rest'
        rMap = get(url)
        if (rMap['isLatest']){
            res['DM'] = "$url/${rMap['version']}"
        }
        // Get Migration API URL
        url = '/interop/rest'
        rMap = get(url)
        rMap['items'].each{ item ->
            if (item['latest']) {
                res['MIG'] = "$url/${item['version']}"
            }
        }
        return res        
    }

    Map executeJob(String url, Map mapPayload, boolean runBackground){

        def payload = script.json(mapPayload)

        script.println "Executing API Post Request [$url] on connection [${conn.getName()}] with payload : "
        mapPayload.each{key, val ->
            script.println "   $key = $val"
        }

        HttpResponse<String> r = conn.post("$url").body(payload).asString()
        if(!(200..299).contains(r.status)) {
            script.throwVetoException("Error ($r.status) : $r.statusText")
        } else {
            Map rMap = ((Map) new JsonSlurper().parseText(r.body))
            if (runBackground){
                script.println "Running Job [${(String)rMap["jobId"]}] in background with current status : ${(String)rMap["jobStatus"]}"
            } else {
                String jobId = (String)rMap["jobId"]
                int rStatus = (int)rMap["status"]
                HttpResponse<String> rNew
                Map rMapNew
                for(long delay = 50; rStatus == -1; delay = Math.min(1000, delay * 2)) {
                    sleep(delay)
                    rMapNew = get("$url/$jobId")
                    rStatus = (int)rMapNew["status"]
                }
                return ["status":rStatus,"jobStatus":(String)rMapNew["jobStatus"],"jobId":jobId]
            }
        }
    }
    Map executeJob(String url, Map mapPayload){
        return executeJob(url,mapPayload,false)
    }

    Map executeDM(Map payload, boolean runBackground){
        if(!payload["endPeriod"]) {payload["endPeriod"] = payload["startPeriod"]}
        if(!payload["importMode"]) { payload["importMode"] = Globals.defImportMode }
        if(!payload["exportMode"]) { payload["exportMode"] = Globals.defExportMode }
        Map ret = executeJob("${baseURLs.DM}/jobs", payload, runBackground)
        script.println "${(String)ret["jobStatus"]} : Data Load [${(String)payload["jobName"]}] executed as job [${(String)ret["jobId"]}]"
        return ret
    }
    Map executeDM(Map payload){
        return executeDM(payload,false)
    }
}
