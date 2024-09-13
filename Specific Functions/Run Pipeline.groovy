/* RTPS: */

%Template(name:="T_GLIB_GeneralUtilties")
long ruleStartTime = currentTimeMillis()

Cube cube = rule.getCube()
Application app = cube.getApplication()
String connectionName = "EPM Local"
Connection connection = app.getConnection(connectionName)
String startPeriodName = "Oct#${stripQuotes(app.getSubstitutionVariableValue("PIPE_ACT_YEAR"))}"
String endPeriodName = "${stripQuotes(app.getSubstitutionVariableValue("PIPE_ACT_MONTH"))}#${stripQuotes(app.getSubstitutionVariableValue("PIPE_ACT_YEAR"))}"
String pipelineName = "GLDataImport"

println "EXECUTING ${rule.getName()} (PIPELINE: $pipelineName) for $startPeriodName through $endPeriodName"

Map<String,String> pipeVars = [:]
pipeVars["STARTPERIOD"] = startPeriodName
pipeVars["ENDPERIOD"] = endPeriodName
pipeVars["IMPORTMODE"] = "Replace"
pipeVars["EXPORTMODE"] = "Replace"
pipeVars["ATTACH_LOGS"] = "N"
pipeVars["SEND_MAIL"] = "FAILURE"
pipeVars["SEND_TO"] = app.getSubstitutionVariableValue("PIPE_MAIL_TO")

def payloadBody = new JSONObject()
  .put("jobType","PIPELINE")
  .put("jobName",pipelineName)
  .put("variables",pipeVars)

String payload = payloadBody.toString()

HttpResponse<String> requestResponse = connection.post("/aif/rest/V1/jobs").body(payload).asString()
awaitCompletion(requestResponse, connectionName, "self")

logTimer(ruleStartTime,"Rule Complete")


