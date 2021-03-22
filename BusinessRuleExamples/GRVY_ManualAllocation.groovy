/* RTPS: {RTP_ACCOUNT} {RTP_CUSTOMER} {RTP_ALLOC_AMT} */

def sAccount = mdxParams(rtps.RTP_ACCOUNT.toString())
def sPeriod = mdxParams("&CUR_MONTH")
def sYears = mdxParams("&CUR_YEAR")
def sScenario = mdxParams("ManualAlloc")
def sVersion = mdxParams("Working")
def sDept = mdxParams("No_Dept")
def sBusUnit = mdxParams("Elkay Manufacturing")
def sBUPF = mdxParams("Elkay Manufacturing (PF)")
def sProduct = mdxParams("All Products")
def sCustomer = mdxParams(rtps.RTP_CUSTOMER.toString())
def sView = mdxParams("PER")
def sAllocAmount = rtps.RTP_ALLOC_AMT.toString()

println "Allocating $sAllocAmount to Account : $sAccount for Base Level Customers under $sCustomer"

AllocationParameters alcParams = new AllocationParameters()

// Allocation Options

//   General Options
alcParams.zeroAmountOption = ZeroAmountOption.ABORT
alcParams.allocationMethod = AllocationMethod.SHARE
alcParams.roundMethod = RoundingMethod.ERRORS_TO_HIGHEST
alcParams.roundDigits = 4

//   Basis Options
alcParams.zeroBasisOption = ZeroBasisOption.NEXT_AMOUNT
alcParams.negativeBasisOption = NegativeBasisOption.DEFAULT


// Point of View
alcParams.pov = """
					CROSSJOIN(	{$sAccount},
					CROSSJOIN(	{$sPeriod},
                    CROSSJOIN(	{$sYears},
                    CROSSJOIN(	{$sScenario},
                    CROSSJOIN(	{$sVersion},
					CROSSJOIN(	{$sView},
                    			{$sDept}
                	))))))
                """
                
// Amount to Allocate
alcParams.amount = sAllocAmount

// Target Range
alcParams.range = """
					CROSSJOIN(	{Descendants($sBusUnit, ${sBusUnit}.dimension.Levels(0))},
                    CROSSJOIN(	{Descendants($sBUPF, ${sBUPF}.dimension.Levels(0))},
                    CROSSJOIN(	{Descendants($sProduct, ${sProduct}.dimension.Levels(0))},
                    			{Descendants($sCustomer, ${sCustomer}.dimension.Levels(0))}
                    )))
				"""

// Basis / Pattern
alcParams.basis = "([Account].[GL_40015],[Scenario].[CalculatedActuals],[View].[Calc_WorkingCopy])"


Cube cube = rule.getCube()
cube.executeAsoAllocation(alcParams)


/* 
CALC_Working view member formula : 
NONEMPTYTUPLE ([PER],[Working])

	([PER],[Working])

*/