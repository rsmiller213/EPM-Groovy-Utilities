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


/* 11 Dimensions
Account, Period, Scenario, View, BUPF, BusinessUnit, Department, Channel(Cust), Product, Version, Years
*/
long startTime = currentTimeMillis()
Cube cube = rule.getCube()
Application app = cube.getApplication()

// Run Time Prompts
def sPeriod = mdxParams(rtps.RTP_PERIODS.getMembers())
def sYear = mdxParams(rtps.RTP_YEAR.toString())
def sVersions = mdxParams(rtps.RTP_VERSIONS.getMembers())
def sScenarios = mdxParams(rtps.RTP_SCENARIOS.getMembers())

// Other Dimensions
def sBusUnit = mdxParams("Elkay Manufacturing")
def sBUPF = mdxParams("Elkay Manufacturing (PF)")
def sProduct = mdxParams("All Products")
def sChannel = mdxParams("All Channels")
def sDept = mdxParams("All Departments")
def sView = mdxParams("PER")


// Accounts Dim - Ignore Dynamic / Shared Accounts
List<String> ignoreMbrs = ['dynamic calc','shared']
def sAccounts = mdxParams(app.getDimension('Account').getEvaluatedMembers("Lvl0Descendants(Account)" ,cube).findAll{!ignoreMbrs.contains(it.toMap()["Data Storage".toString()])}*.getName(MemberNameType.ESSBASE_NAME))

println "CLEARING $sVersions AND $sScenarios for $sPeriod and $sYear"
def sClearPOV = """
			CROSSJOIN(	{$sAccounts},
			CROSSJOIN(	{$sPeriod},
			CROSSJOIN(	{$sScenarios},
			CROSSJOIN(	{Descendants($sBUPF, ${sBUPF}.dimension.Levels(0))},
			CROSSJOIN(	{Descendants($sBusUnit, ${sBusUnit}.dimension.Levels(0))},
			CROSSJOIN(	{Descendants($sDept, ${sDept}.dimension.Levels(0))},
			CROSSJOIN(	{Descendants($sChannel, ${sChannel}.dimension.Levels(0))},
			CROSSJOIN(	{Descendants($sProduct, ${sProduct}.dimension.Levels(0))},
			CROSSJOIN(	{$sYear},
			CROSSJOIN(	{$sView},
						{$sVersions}
			))))))))))
			"""
cube.clearPartialData(sClearPOV,true)
logTimer(startTime,"CLEARING COMPLETE")
