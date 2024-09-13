/* RTPS: */

/* 11 Dimensions
DENSE : Account, Period, View | SPARSE : Scenario, Version, Years,  Award, Element, CostCode, Employee, Project
*/

%Template(name:="T_GLIB_GeneralUtilities",plantype:="STAFF")
%Template(name:="T_GLIB_EPMAutomateUtilities",plantype:="STAFF")
long ruleStartTime = currentTimeMillis()

// Globals
Cube cube = rule.getCube()
Application app = cube.getApplication()
Connection localHost = app.getConnection("EPMLocal")
EpmAutomate epmAuto = getEpmAutomate()


String baseDirectory = "/u03/lcm" 
String zipInputFileName = "ValidIntersections.zip"
String zipOutputFileName = zipInputFileName.replace(".zip", "_New.zip")
String xlInputFileName = zipInputFileName.replace(".zip", "-1.xlsx") 
String xlOutputFileName = zipInputFileName.replace(".zip", "_New.xlsx")
String errorFileName = zipInputFileName.replace(".zip", "_Log.txt")
List viWorksheets = ["Rules","Sub Rules"]

// DEBUG - Print XL Rules/SubRules to Log to help build the input files
boolean debug = false
Map sheetRowsList = [:]


// As we cannot access the base classes in EPM Groovy (due to sandboxing) we need to extend
class XLFactory extends org.apache.poi.ss.usermodel.WorkbookFactory {
  public XLFactory(){
    super()
  }
}

class XLInputStream extends java.io.FileInputStream{
  public XLInputStream(String path){
    super(path)
  }
}

class XLOutputStream extends java.io.FileOutputStream{
  public XLOutputStream(String path){
    super(path)
  }
}

class XLZipOutputStream extends java.util.zip.ZipOutputStream{
  XLZipOutputStream(XLOutputStream outputStream){
    super(outputStream)
  }
}

class XLZipInputStream extends java.util.zip.ZipInputStream {
  XLZipInputStream(XLInputStream inputStream){
    super(inputStream)
  }
  // Due to static typing in EPM Groovy, need this to return XLZipEntry, not ZipEntry
  public XLZipEntry getNextEntry() {
    java.util.zip.ZipEntry entry = super.getNextEntry()
    if (entry != null){
      return new XLZipEntry(entry.getName())
    }
    return null
  }
}

class XLZipEntry extends java.util.zip.ZipEntry {
  public XLZipEntry(String filename){
    super(filename);
  }
}



// Zip Buffer Helper
def processZipWithBuffer( InputStream is, OutputStream os ){
  byte[] buffer = new byte[1024]
  int length
  while ((length = is.read(buffer)) > 0) {
    os.write(buffer, 0, length)
  }
}

// Need our own Zip Utility
def zipFile( String inputFileName, String zipFileName, String directory ){
  // Create Buffer
  byte[] buffer = new byte[1024]
  //Create the output stream for zip file
  XLOutputStream fos = new XLOutputStream("$directory/$zipFileName")
  XLZipOutputStream zos = new XLZipOutputStream(fos)
  //Open XL File for Reading
  XLInputStream fis = new XLInputStream("$directory/$inputFileName")
  //Create Zip Entry
  XLZipEntry zipEntry = new XLZipEntry(inputFileName)
  zos.putNextEntry(zipEntry)

  processZipWithBuffer(fis, zos)

  //Close Process
  zos.closeEntry()
  fis.close()
  zos.close()

  println "File [$inputFileName] zipped as [$zipFileName]"
}

// Need Our Own Unzip Utility - Could have used pipeline File Operations, but choose to keep it all in one package (this script)
def unzipFile(String zipFileName, String directory){
  // Create Buffer
  byte[] buffer = new byte[1024]


  // Open the zip file for reading
  XLInputStream fis = new XLInputStream("$directory/$zipFileName")
  XLZipInputStream zis = new XLZipInputStream(fis)

  // Process each entry in the zip file
  XLZipEntry zipEntry
  while ((zipEntry = zis.getNextEntry()) != null) {
    String outFileName = directory + "/" + zipEntry.getName()
    println "Extracting file: ${outFileName}"

    // Create output stream for the extracted file
    XLOutputStream fos = new XLOutputStream(outFileName)

    processZipWithBuffer(zis, fos)

    // Close the file output stream
    fos.close()
    zis.closeEntry()
  }

  // Close the zip input stream
  zis.close()
  fis.close()

  println "Unzipping completed for: $zipFileName"
}

// Build Excel Workbook Object
org.apache.poi.ss.usermodel.Workbook getXLWorkbook( String fileName, String directory ){
  return new XLFactory().create(new XLInputStream("$directory/$fileName"))
}

// These are your new VI lines and can be created anyway. Just have to make sure they align properly... 
// Run this script with debug=true (set up top) to get a csv written in the log of what it can be
// For the RULES rows, leave the XX_ROWNUM as that gets replaced with the correct row number as its building. It will be added on to the end
Map<String, List> newValidIntersections = [
  "Rules":[
    ["Payroll Lines2", "XX_ROWNUM", '', "true", "Invalid Intersection", "Account", "true", "Element", "false", "View", "false", "Award", "false"]
  ],
  "Sub Rules":[
    ["Payroll Lines2", "ILvl0Descendants(OFFSET_DIST)", '', '', "ILvl0Descendants(TOTAL_PAYROLL)", '', '', "ProjectDistribution", '', '', "ILvl0Descendants(ALL_AWARDS)"],
    ["Payroll Lines2", "ILvl0Descendants(OFFSET_DIST)", '', '', "ILvl0Descendants(TOTAL_OTL)", '', '', "ProjectDistribution", '', '', "ILvl0Descendants(ALL_AWARDS)"]
  ]
]



try{

  // Custom Login Function
  epmAutoLogin(epmAuto,localHost)

  // Ensure old files do not exist
  EpmAutomateStatus epmStatus = epmAuto.execute('listFiles')
  epmAutoCheckStatus(epmStatus, "   File List")
    
  // Filter List File Results and Delete Old Files
  List fileSearch = [zipInputFileName.replace('.zip', '')]
  List<String> fileList = epmAutoGetFilteredFileList( epmStatus, fileSearch, [] )
  fileList.each{ file ->
    epmStatus = epmAuto.execute('deleteFile', file)
    epmAutoCheckStatus(epmStatus, "   Deleting File in EPM Inbox: $file", false)
  }

  // Export Existing Valid Intersections
  epmStatus = epmAuto.execute('exportValidIntersections', zipInputFileName)
  epmAutoCheckStatus(epmStatus, "   Export VI")
    
  // Unzip VI File
  unzipFile(zipInputFileName, baseDirectory)
    
  // Get XL Workbook Object
  def wb = getXLWorkbook(xlInputFileName, baseDirectory)
  
  // Build Sheets with New Rows
  viWorksheets.each{ String sheetName ->
    def ws = wb.getSheet(sheetName)
    def firstCol = ws.getRow(ws.getFirstRowNum()).getFirstCellNum()
    def lastCol = ws.getRow(ws.getFirstRowNum()).getLastCellNum() - 1
      
    // ADD NEW ROWS 
    def currentRow = ws.getLastRowNum() + 1
    List<List> newRowList = newValidIntersections[sheetName] as List
    
    newRowList.each{ rule ->
      def newRow = ws.createRow(currentRow)
      (firstCol..lastCol).each{ cellNum ->
        String cellValue = rule[cellNum] ?: ''
        if (cellValue == "XX_ROWNUM") cellValue = currentRow.toString() 
        def newCell = newRow.createCell(cellNum)
        newCell.setCellValue(cellValue)
      }
      currentRow ++        
    }
      
    // DEBUG ONLY
    if(debug){
      // Read All Rows
      sheetRowsList[sheetName] = []
      ws.rowIterator().each{ org.apache.poi.ss.usermodel.Row row ->
        List rowList = []
        (firstCol..lastCol).each{ cellNum ->
          def currentCell = row.getCell(cellNum) ?: row.createCell(cellNum)
          rowList << currentCell
        }
        ((List)sheetRowsList[sheetName]).add(rowList)
      }
    }
  }
    
  XLOutputStream xlOut = new XLOutputStream("$baseDirectory/$xlOutputFileName")
  wb.write( xlOut )
  xlOut.close()
  wb.close()

  // Zip File
  zipFile(xlOutputFileName, zipOutputFileName, baseDirectory)

  // Import New VI
  epmStatus = epmAuto.execute('importValidIntersections', zipOutputFileName, "ErrorFile=$errorFileName")
  epmAutoCheckStatus(epmStatus, "   Import VI")


} catch (Exception ex){
  println "Error: ${ex.toString()}"
  println sheetRowsList
  throwVetoException("Error: ${ex.toString()}")
} finally{
  epmStatus = epmAuto.execute('logout')
}

if(debug) println sheetRowsList


