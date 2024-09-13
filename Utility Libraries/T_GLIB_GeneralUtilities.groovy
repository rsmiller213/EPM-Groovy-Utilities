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


def getJobStatus( Connection connection, HttpResponse<String> jsonResponse, String relString, Boolean awaitCompletion ) {
   
    final int IN_PROGRESS = -1
    
    if ( !(200..299).contains(jsonResponse.status) ) {
       def errorBody = new JsonSlurper().parseText(jsonResponse.body) as Map
        println ( "\n" + logPrettyMap(errorBody) )
      throwVetoException("Error occured from Response: $jsonResponse.statusText")
   }
    
    def responseBody = new JsonSlurper().parseText(jsonResponse.body) as Map
    Integer status = responseBody["status"] as Integer
    String fullUrl = responseBody.links.find{ it["rel"] == relString }["href"].toString()
    String jobUrl
    if( fullUrl.contains('interop') ){
      jobUrl = fullUrl.substring(fullUrl.lastIndexOf("/interop"))
    } else if (fullUrl.contains('HyperionPlanning')) {
      jobUrl = fullUrl.substring(fullUrl.lastIndexOf("/HyperionPlanning"))
    } else if (fullUrl.contains('aif')) {
      jobUrl = fullUrl.substring(fullUrl.lastIndexOf("/aif"))
    }
    if(awaitCompletion && jobUrl) {
    
       for( long delay = 50; status == IN_PROGRESS; delay = Math.min(1000, delay * 2) ) {
           Integer oldStatus = status
           sleep(delay)
           HttpResponse<String> statusResponse = connection.get(jobUrl).asString()
           responseBody = new JsonSlurper().parseText(statusResponse.body) as Map
           status = responseBody["status"] as Integer
           println "Old Status : $oldStatus | New Status : $status"
           if( responseBody["subStatus"]  ){ println "SubStatus : ${responseBody["subStatus"]}" }
      }
    
       if(status != 0){
          println logPrettyMap(responseBody)
         throwVetoException("Error occured from Status Ping: ${responseBody.details}")
      }
    } else {
       println "\nRunning in background\n\n"
        println "\n${responseBody}"
    }
}

def getJobStatus( Connection connection, HttpResponse<String> jsonResponse ) {
   getJobStatus(connection,jsonResponse,"self",true)
}

def getJobStatus( Connection connection, HttpResponse<String> jsonResponse, Boolean awaitCompletion ) {
   getJobStatus(connection,jsonResponse,"self",awaitCompletion)
}

def getJobStatus( Connection connection, HttpResponse<String> jsonResponse, String relString ) {
   getJobStatus(connection,jsonResponse,relString,true)
}



class GridUtils {

   DataGrid grid
    Map<String, String> povDimToMemberMap
    List<Map<String, String>> rowsDimToMemberMap
    List<Map<String, String>> rowsDimToEditableMemberMap
    Map<String, Set<String>> colsDimToMemberMap = [:]
    //Map<String, Set<String>> colsDimToEditableMemberMap = [:]
   
    GridUtils( DataGrid grid ) {
       
        this.grid = grid
        Closure<Map<String, String>> dimToMemberMap = { List<DataGrid.HeaderCell> headerCells ->
         headerCells?.collectEntries { [ (it.dimName) : it.mbrName ] }
      }
        
        this.povDimToMemberMap = dimToMemberMap( grid.pov )
        this.rowsDimToMemberMap = grid.rows.collect{ DataGrid.Row row -> dimToMemberMap( row.headers ) }
        this.rowsDimToEditableMemberMap = grid.rows.findAll{ it.data.any{ !it.readOnly } }.collect{ DataGrid.Row row -> dimToMemberMap( row.headers ) }
        grid.columns.each{ List<DataGrid.HeaderCell> hdrs -> colsDimToMemberMap.put( hdrs.get(0).dimName, hdrs*.mbrName as LinkedHashSet ) }
        //FIXME: get this to work 
        //grid.columns.each{ List<DataGrid.HeaderCell> hdrs -> hdrs.findAll{ it.isCalcableForPOV() && !it.isZoomable() }.collect{ DataGrid.HeaderCell hdr -> colsDimToEditableMemberMap.put( hdr.get(0).dimName, hdr*.mbrName as LinkedHashSet ) }
    }
    
    String getPovMember( String dimName ){
       return povDimToMemberMap?.get(dimName)
    }
    
    List<String> getColumnMembers( String dimName ){
       Set<String> columnSet = colsDimToMemberMap?.get(dimName)
      columnSet ? new ArrayList<String>(columnSet) : null
    }
    
    List<String> getRowMembers( String dimName ) {
      List<String> rowMembersWithDupes = rowsDimToMemberMap*.get(dimName)
      rowsDimToMemberMap ? new ArrayList<String>(rowsDimToMemberMap as LinkedHashSet) : null
   }
    
    List<String> getMembers( String dimName ) throws Exception{
       String pov = getPovMember(dimName)
      List<String> members = pov ? [pov] : ( getColumnMembers(dimName) ?: getRowMembers(dimName) )
      if(members) {
         return members
      } else {
         throw new Exception("Dimension: " + dimName + " isn't present in the grid.")
      }
    }
    
    List<DataGrid.Row> getEditedRows () {
      List<DataGrid.Row> res = this.grid.rows.findAll{ it.data.any{ it.edited } }
       if( res && res.size() > 0 ) {
          return res
      } else {
           return null
        }
   }
    
    List<DataGrid.DataCell> getEditedCells () {
      List<DataGrid.DataCell> res = this.grid.rows.findAll{ it.data.any{ it.edited } }.collect{ it.data.findAll{ DataCell cell -> cell.edited } }.flatten() as List<DataGrid.DataCell>
       if( res && res.size() > 0 ) {
          return res
      } else {
           return null
        }
   }
    
    List<DataGrid.Row> getEditableRows () {
      List<DataGrid.Row> res = this.grid.rows.findAll{ it.data.any{ !it.readOnly } }
       if( res && res.size() > 0 ) {
          return res
      } else {
           return null
        }
   }
    
    List<DataGrid.DataCell> getEditableCells () {
      List<DataGrid.DataCell> res = this.grid.rows.findAll{ it.data.any{ !it.readOnly } }.collect{ it.data.findAll{ DataCell cell -> !cell.readOnly } }.flatten() as List<DataGrid.DataCell>
       if( res && res.size() > 0 ) {
          return res
      } else {
           return null
        }
   }    

}


//List<DataGrid.DataCell> gridCells =  operation.grid.rows.findAll{ it.data.any{ !it.isReadOnly() } }.collect{ it.data.findAll{ DataCell cell -> !cell.isReadOnly() } }.flatten() as List<DataGrid.DataCell>




