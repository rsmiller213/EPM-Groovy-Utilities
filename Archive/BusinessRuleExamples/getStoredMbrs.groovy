/*RTPS:*/
%Template(name:="TMP_GROOVY_LIBRARY")

long startTime = currentTimeMillis() // get current time in milliseconds


try {
	// Grab the Cube & Application
	Cube cube = rule.getCube()
	Application app = cube.getApplication()

	//Get the Period Dimension
	Dimension dimPeriod = app.getDimension("Period")

	//Get Descendants of YearTotal
	List<Member> mbrsPeriod = dimPeriod.getEvaluatedMembers("@DESCENDANTS(YearTotal)",cube)
	println "All Members : ${mbrsPeriod*.getName()}"

	// Get Stored Members from a List<Member>
	List<Member> mbrsStoredPeriods = getStoredMbrs(mbrsPeriod)
	println "Stored Members : $mbrsStoredPeriods"
	println "Stored Members Quoted & Delim'd : ${cscParams(mbrsStoredPeriods)}"


	// Build List of Member Names as String
	List<String> lstPeriod = ["Jan","Feb","Mar","Q1","Apr","May","Jun","Q2"]
	println "All Members : $lstPeriod"

	// Get Stored Members from a List<String>
	List<Member> mbrsStoredPeriods = getStoredMbrs(lstPeriod,dimPeriod)
	println "Stored Members : $mbrsStoredPeriods"
	println "Stored Members Quoted & Delim'd : ${cscParams(mbrsStoredPeriods)}"
	
} catch (Exception e) {
	println e
}

logTimer(startTime, "Member Example")
