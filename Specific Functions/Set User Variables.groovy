/* RTPS: {RTP_USER} */

// Globals
Cube cube = rule.getCube()
Application app = cube.getApplication()

Map<String,String> userVarNames = [:]
userVarNames["ExpenseFilter"] = "Account"
userVarNames["RevenueFilter"] = "Account"

userVarNames["CostCodeFilter"] = "CostCode"
userVarNames["CostCodeView"] = "CostCode"

userVarNames["EmployeeFilter"] = "Employee"

userVarNames["FundFilter"] = "Fund"
userVarNames["FundView"] = "Fund"

userVarNames["YearView"] = "Years"


userVarNames.each{ uvName, uvDim ->

	if( app.hasUserVariable(uvName) ){
    	UserVariable uv = app.getUserVariable( uvName )
        if( !uv.value ){
        	Dimension dim = app.getDimension( uvDim, cube )
            List<Member> dimMembers = dim.getEvaluatedMembers( uv.getMemberSelection(), cube )
            println "${uv.name} has no value, setting it to ${dimMembers[0].name}"
            app.setUserVariableValue( uv, dimMembers[0] )
            println "${uv.name} has been set to ${uv.value}"
        } else {
        	println "User Var has Value : ${uv.getName()} = ${uv.getValue()}"
        }
        
    } else {
    	println "User Var not found : ${uvName}"
    }

}
