/* **********************************************************
*
*    EPM Automate Functions
*
* **********************************************************/

def epmAutoCheckStatus(EpmAutomateStatus epmAutoStatus, String message, boolean killProcessOnFailure = true) {
   if ( epmAutoStatus.status != 0 )   {
      println "${message} Output : ${epmAutoStatus.output}"
      if ( killProcessOnFailure ) {
         throwVetoException("${message} Failed : ${epmAutoStatus.status}")
        } else {
         println "${message} Failed : ${epmAutoStatus.status}"
      }
    } else {
      println "${message} Status : ${epmAutoStatus.status}"
   }
}

String getConnectionPassword (Connection conn) {
   HttpRequest r = conn.get().getHttpRequest()
   def passwordField = HttpRequest.getDeclaredField('password')
   passwordField.setAccessible(true)
   return passwordField.get(r)
}

String generateEncryptionKey() {
   String characters = (('A'..'Z') + ('0'..'9')).join('')
   new SecureRandom().with {
      (1..10).collect { characters[ nextInt( characters.length() ) ] }.join('')
   }
}

def epmAutoLogin( EpmAutomate epmAuto, Connection conn ) {
   EpmAutomateStatus loginStatus = epmAuto.execute('login', conn.getUserName(), 'password.epw', conn.getUrl() )
   println "Initial Login Status [${loginStatus.status}] | Output: ${loginStatus.output}"
   if ( loginStatus.status != 0 ) {
      loginStatus = epmAuto.execute('encrypt', getConnectionPassword(conn), generateEncryptionKey(), 'password.epw')
      println "Encryption Status [${loginStatus.status}] | Output: ${loginStatus.output}"
      if ( loginStatus.status != 0 ) {
         throwVetoException("Encryption Failed [${loginStatus.status}]: ${loginStatus.output}")
      }
      loginStatus = epmAuto.execute('login', conn.getUserName(), 'password.epw', conn.getUrl() )
      println "Second Login Status [${loginStatus.status}] | Output: ${loginStatus.output}"
      if ( loginStatus.status != 0 ) {
         throwVetoException("Second Login Failed [${loginStatus.status}]: ${loginStatus.output}")
      }
   }
}

List epmAutoGetFilteredFileList( EpmAutomateStatus epmStatus, List<String> include = [], List<String> exclude = [] ) {
   List res = []
   epmStatus.getItemsList().each { file ->
      String fileName = file as String
      boolean write = true

      if ( include.size() > 0 ) {
            include.each { check ->
            if ( !fileName.contains(check) && check != '' ) { write = false }
            }
      }

      if ( exclude.size() > 0 && write ) {
            exclude.each { check ->
            if ( fileName.contains(check) ) { write = false }
            }
      }

      if (write) { res << fileName }
   }
   return res
}
