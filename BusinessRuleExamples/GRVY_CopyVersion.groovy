/* RTPS: {RTP_SOURCE_VERSION} {RTP_TARGET_VERSION} {RTP_PERIOD} {RTP_YEAR} */

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
def sPeriod = mdxParams(rtps.RTP_PERIOD.toString())
def sYear = mdxParams(rtps.RTP_YEAR.toString())
def sTargetVersion = mdxParams(rtps.RTP_TARGET_VERSION.toString())
def sSourceView = "[CALC_WorkingCopy]"
def sSourceVersion = "[Working]"
if (rtps.RTP_SOURCE_VERSION.toString().toUpperCase().equals("PREFINAL")) {
	sSourceView = "[CALC_PreFinalCopy]"
	sSourceVersion = "[PreFinal]"
}

// Other Dimensions 
def sBusUnit = mdxParams("Elkay Manufacturing")
def sBUPF = mdxParams("Elkay Manufacturing (PF)")
def sProduct = mdxParams("All Products")
def sChannel = mdxParams("All Channels")
def sDept = mdxParams("All Departments")
def sScenario = mdxParams("Actual")
def sView = mdxParams("PER")


// Accounts Dim - Ignore Dynamic / Shared Accounts
List<String> ignoreMbrs = ['dynamic calc','shared']
def sAccounts = mdxParams(app.getDimension('Account').getEvaluatedMembers("Lvl0Descendants(Account)" ,cube).findAll{!ignoreMbrs.contains(it.toMap()["Data Storage".toString()])}*.getName(MemberNameType.ESSBASE_NAME))

// BU Get Loop Dimension
List<String> lstBU = app.getDimension('BusinessUnit').getEvaluatedMembers("Lvl0Descendants($sBusUnit)" ,cube)*.getName(MemberNameType.ESSBASE_NAME)
//List<String> lstBU = app.getDimension('BusinessUnit').getEvaluatedMembers("Lvl0Descendants(Elkay Intl)" ,cube)*.getName(MemberNameType.ESSBASE_NAME)


println "CLEARING $sTargetVersion for $sPeriod and $sYear"
def sClearPOV = """
			CROSSJOIN(	{$sAccounts},
			CROSSJOIN(	{$sPeriod},
			CROSSJOIN(	{Descendants($sScenario, ${sScenario}.dimension.Levels(0)),[FinancialActuals]},
			CROSSJOIN(	{Descendants($sBUPF, ${sBUPF}.dimension.Levels(0))},
			CROSSJOIN(	{Descendants($sBusUnit, ${sBusUnit}.dimension.Levels(0))},
			CROSSJOIN(	{Descendants($sDept, ${sDept}.dimension.Levels(0))},
			CROSSJOIN(	{Descendants($sChannel, ${sChannel}.dimension.Levels(0))},
			CROSSJOIN(	{Descendants($sProduct, ${sProduct}.dimension.Levels(0))},
			CROSSJOIN(	{$sYear},
			CROSSJOIN(	{$sView},
						{$sTargetVersion}
			))))))))))
			"""
cube.clearPartialData(sClearPOV,true)
logTimer(startTime,"CLEARING COMPLETE")

def loopStart = currentTimeMillis()
println "COPYING $sSourceVersion to $sTargetVersion for $sPeriod and $sYear"
lstBU.each {BU -> 
	def buStart = currentTimeMillis()
	CustomCalcParameters calcParams = new CustomCalcParameters()
	
	calcParams.pov = """
			CROSSJOIN(	{$sAccounts},
			CROSSJOIN(	{$sPeriod},
			CROSSJOIN(	{Descendants($sScenario, ${sScenario}.dimension.Levels(0)),[FinancialActuals]},
			CROSSJOIN(	{Descendants($sBUPF, ${sBUPF}.dimension.Levels(0))},
			CROSSJOIN(	{[$BU]},
			CROSSJOIN(	{Descendants($sDept, ${sDept}.dimension.Levels(0))},
			CROSSJOIN(	{Descendants($sChannel, ${sChannel}.dimension.Levels(0))},
			CROSSJOIN(	{Descendants($sProduct, ${sProduct}.dimension.Levels(0))},
			CROSSJOIN(	{$sYear},
			CROSSJOIN(	{$sView},
						{$sTargetVersion}
			))))))))))
			"""
	calcParams.sourceRegion = "{($sSourceVersion,$sSourceView)}"
	calcParams.script = "([PER]) := ($sSourceView);"
    calcParams.roundDigits = 4
    cube.executeAsoCustomCalculation(calcParams)
    //logTimer(buStart,"COPIED $BU in")
}
logTimer(loopStart,"COPY COMPLETE")
logTimer(startTime,"FULL PROCESS COMPLETE")

/* 
CALC_Working view member formula : 
NONEMPTYTUPLE ([PER],[Working])

	([PER],[Working])

*/