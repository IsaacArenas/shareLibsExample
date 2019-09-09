/**
  Author : Isaac Arenas
  Description: Execute an Application Process Request on Udeploy using the API
  Pipeline Call Sintax:

  executeApplicationProcess([
    componentName: "string containing the component Name",
    udeployAppName: "string containing the applicationName ",
    appProcess: "string containing the Application ProcessName "
    env: "string contianing the application environment"
    version:"string containing the componennt version"    
  ])

  to use :
  @Library('libraryName') _
  executeApplicationProcess([
    componentName: "COSA_SERVICE",
    udeployAppName: "COSA_APP ",
    appProcess: "COSA_SERVICE_INSTALL "
    env: "dev"
    version:"1.0.0"    
  ])

**/
import groovy.json.JsonSlurper;
import groovy.json.JsonOuput;

def call(Map inputs){
  //setting possible results status of the execution status
  POSITIVE_RESULTS=["SUCCEDED"]
  NEGATIVE_RESULTS=["APPROVAL REJECTED","CANCELLED","FAILED TO START", "FAULTED"]
  udeployCred="fakeCredential"

  //setting up some initial configuration for the API urls
  udeployBaseUrl="https://fakeUdeploy.com/"
  applicactionProcessReqAPI= "cli/applicationProcessRequest/request"
  applicactionProcessStaAPI= "cli/applicationProcessRequest/requestStatus"
  applicationRequestReview= "#applicationProcessRequest/"
  applicationProcess= inputs.appProcess

  description "executing app Process Request"


  stage ("Run Application Process"){
    //generate the body use for execution
    jsonBody=appProcessReqBody(inputs)
    //executing The app Process
    response=executeAppProcess(jsonBody)
    //track the status of the request
    trackAppProcess(response)
  }

}

//method to look for the status of the execution 
def getRequestStatus(requestId){
  //creates the Url to get the status
  apiUrl= udeployBaseUrl+ applicactionProcessStaAPI+ "?request=" + requestId
  //execute Get
  resp= getRestExec(apiUrl)
  //parse the response
  def String versionJson= resp.content
  def slurper= new JsonSlurper()
  @NonCPS
  statusResp=slurper.parseText(versionJson)
  //return the status of the request
  return [status: statusResp.status result: statusResp.result]
}
//method to create the body of the httpRequest based on the inputs
def appProcessReqBody(inputs){
  appProcess= [application:inputs.udeployAppName,
  description: description,
  applicationProcess: applicationProcess,
  environment: inputs.env,
  onlyChanged:"false",
  versions:[[version: inputs.version], component: inputs.componentName]]]
  return JsonOuput.toJson(appProcess)
  ]
}
//method to execute the request itself
def executeAppProcess(jsonBody){
  apiUrl= udeployBaseUrl+ applicactionProcessReqAPI
  resp= putRestExec(apiUrl,jsonBody)
  def String versionJson= resp.content
  def slurper= new JsonSlurper()
  @NonCPS
  processResp=slurper.parseText(versionJson)
  return processResp.requestId
}
//http GET Method execution
def getRestExec(uri){
  response = httpRequest( 
    url:uri,
    authentication: udeployCred 
  )
  return response
}
//http PUT Method execution
def putRestExec(uri, body){
  response = httpRequest( 
    httpMode: 'PUT',
    url:uri,
    authentication: udeployCred 
    requestBody: body
  )
  return response
}
//method to track the status of the request
def trackAppProcess(requestId){
  results=true
    while(results){
      sleep(10)
      status= getRequestStatus(requestId)
      println("status: " + status.status + " result "+ status.result)
      results = status.result=='NONE'
    }
    if (status.result!='SUCCEDED'){
      throw new Exception("Execution Failed please Review: "+ udeployBaseUrl + applicationRequestReview + requestId)
    }
}