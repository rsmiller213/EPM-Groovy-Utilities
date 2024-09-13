/* RTPS: */

//Cube cube = operation.getCube()
Application app = operation.getApplication()
Cube cube = app.getCube("RevExp")

println "App : ${app.name} | Cube : ${cube.name}"

UserVariable uvCostCenter = app.getUserVariable("UV_CostCenter")

println "Name : ${uvCostCenter.name} | Value : ${uvCostCenter.value}"

if ( !uvCostCenter.value ) {
	// Get No_CostCenter Member
    Dimension dimCC = app.getDimension("Cost Center",cube)
	app.setUserVariableValue(uvCostCenter, dimCC.getMember("No_CostCenter",cube))
    println "Setting UV Name : ${uvCostCenter.name} | Value : ${uvCostCenter.value}"
}

