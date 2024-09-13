/* RTPS: {RTP_VERSIONS} {RTP_SCENARIOS} {RTP_PERIODS} {RTP_YEAR} */

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

long startTime = currentTimeMillis()
Cube cube = rule.getCube()
Application app = cube.getApplication()

// Run Time Prompts
def sPrograms = mdxParams(rtps.RTP_PROGRAM_SELECT.getMembers())

// Other Dimensions
def sPeriod = mdxParams("YearTotal")
def sYear = mdxParams("&PLN_Year1")
def sVersions = mdxParams("Final")
def sScenarios = mdxParams("Budget")
def sJob = mdxParams("TOTAL_JOBS")
def sDataElement = mdxParams("Staff")
def sFund = mdxParams("ALL_FUNDS")
def sLineItem = mdxParams("ALL_LINEITEMS")
def sFunction = mdxParams("ALL_FUNCTIONS")
def sCostCenter = mdxParams("ALL_COSTCENTERS")
def sProject = mdxParams("ALL_PROJECTS")
def sEmployee = mdxParams("TOTAL_EMPLOYEES")
def sFuture = mdxParams("Future1")
def sView = mdxParams("Periodic")


// Accounts Dim - Ignore Dynamic / Shared Accounts
List<String> ignoreMbrs = ['dynamic calc','shared']
def sAccounts = mdxParams(app.getDimension('Account').getEvaluatedMembers("Lvl0Descendants(IS)" ,cube).findAll{!ignoreMbrs.contains(it.toMap()["Data Storage".toString()])}*.getName(MemberNameType.ESSBASE_NAME))

println "CLEARING $sVersions AND $sScenarios for $sPeriod and $sYear"
def sClearPOV = """
			CROSSJOIN(	{$sAccounts},
			CROSSJOIN(	{Descendants($sPeriod, ${sPeriod}.dimension.Levels(0))},
			CROSSJOIN(	{$sScenarios},
			CROSSJOIN(	{$sPrograms},
			CROSSJOIN(	{Descendants($sJob, ${sJob}.dimension.Levels(0))},
			CROSSJOIN(	{$sDataElement},
			CROSSJOIN(	{Descendants($sFund, ${sFund}.dimension.Levels(0))},
			CROSSJOIN(	{Descendants($sLineItem, ${sLineItem}.dimension.Levels(0))},
            CROSSJOIN(	{Descendants($sFunction, ${sFunction}.dimension.Levels(0))},
            CROSSJOIN(	{Descendants($sCostCenter, ${sCostCenter}.dimension.Levels(0))},
            CROSSJOIN(	{Descendants($sProject, ${sProject}.dimension.Levels(0))},
            CROSSJOIN(	{Descendants($sEmployee, ${sEmployee}.dimension.Levels(0))},
			CROSSJOIN(	{$sFuture},
            CROSSJOIN(	{$sYear},
			CROSSJOIN(	{$sView},
						{$sVersions}
			)))))))))))))))
			"""
cube.clearPartialData(sClearPOV,true)
logTimer(startTime,"CLEARING COMPLETE")
