/*RTPS:*/
%Template(name:="TMP_GROOVY_LIBRARY")

long ruleStart = currentTimeMillis()
Cube cube = rule.getCube()
Application app = cube.getApplication()
Connection conn = rule.cube.application.getConnection(Globals.connLocalEPM)
println "App : $app | Cube : $cube | Conn : ${conn.getName()}"
epmLogUser()

try{
	API api = new API(this)
	println "Script ${api.baseURLs}"

/*Map payload = [
		"jobType":"DATARULE",
		"jobName":"LR_OP_TEST_NUM",
		"startPeriod":"Oct-15",
		"endPeriod":"Mar-16",
		"fileName":"TEST_DM_NUMERIC.txt"
		]
	def result = api.executeDM(payload)
    epmLogPrettyMap(result)*/
} catch (Exception e) {
	println e.toString()
    println e.getMessage()
    println e.getStackTrace()
}