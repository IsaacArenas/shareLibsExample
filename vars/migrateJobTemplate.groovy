/**
  Author : Isaac Arenas
  Description: Create a copy of a jenkins job and change the template is using
  Pipeline Call Sintax:

  migrateJobTemplate([
    newTemplate: "string containing the new template location",
    jobUrl: "url for the job you want to copy  https://fakejenkins/job/folder/job/fakeJob/ ",
    copySuffix: "suffix that would be added to the job name  example if suffix is "copy" the new job would be on https://fakejenkins/job/folder/job/fakeJob-copy/ "    
  ])

  to use :
  @Library('libraryName') _
  migrateJobTemplate([
    newTemplate: "/folder/FakeTemplate123",
    jobUrl: "https://fakejenkins/job/folder/job/fakeJob/",
    copySuffix: "copy"    
  ])

**/

def call(Map inputs){
  // suffix to be added to the url to get the job configuration xml
  configSuffix ="config.xml"
  // credential to use on the jenkins API to access the jobs
  credential = "fakeCredential"
  //inputs 
  template= inputs.newTemplate
  copySuffix= inputs.copySuffix 
  jobUrl= inputs.jobUrl

  //slave to execute 
  node("fakeNode"){
    //stage to get the xml configuration of the first job using the rest APÎ
    stage("get Configuration"){
      response =getRestExec(jobUrl+configSuffix,credential)
      content = response.content
    }
    stage("replace configuration"){
      //split the url by slashes and get the JobName from there
      splitUrl = jobUrl.split('/')
      jobName= splitUrl[splitUrl.lenght -1]
      /** 
        due to the insertions of alot of special unicode characters on the jobs regex its a cleaner solution to make this change than parseXml
        changing 2 things :
        - model tag: that contains the template name 
          uniqueString "<model>template location</model>" 
        - job name tag: that contains the display name of the job 
          uniqueString "
          <entry>
            <string>name</string>
            <string>jobName</string>
          </entry>"   
      **/
      newContent= content.replaceAll(/\<model\>(.*)\<\/model\>/, "<model>${template}</model>")
        .replaceAll(/\<entry\>\n[ \t]*<string\>name\<\/string\>\n[ \t]*\<string\>[a-zA-Z-]*\<\/string>\n[ \t]*\<\/entry\>/,
        "<entry>\n<string>name</string>\n<string>${jobName}</string>\n</entry>" )
    }
    stage("create a copy of the Job"){
      createItemUrl= jobUrl.replaceAll(/\/job\/[0-9a-zA-Z-]*\/$/), "/createITem?name="]+jobName+"-"+copySuffix)
      postRestExec(createItemUrl, newContent,credential)
    }
  } 
}
/**
method that execute a GET http Method agains the Provided URL
inputs:
getUri : the Url to call
returns:
the complete http response
**/
def getRestExec(getUri){
  //use the httpRequest Jenkins plugin to make the request 
  // by default the only valid response status range is 100:399
  response = httpRequest(
    url: getUri,
    authentication: credential
  )
  return response
}
/**
method that execute a GET http Method agains the Provided URL
inputs:
getUri : the Url to call
credential : credential use for the jenkins API
returns:
the complete http response
**/
def getRestExec(getUri,credential){
  //use the httpRequest Jenkins plugin to make the request 
  // by default the only valid response status range is 100:399
  response = httpRequest(
    url: getUri,
    authentication: credential
  )
  return response
}

/**
method that execute a Post http Method agains the Provided URL
inputs:
uri : String containing the Url to call
body: String containing the xml body to call
credential : credential use for the jenkins API
returns:
the complete http response
**/
def gePostExec(uri,body,credential){
  //use the httpRequest Jenkins plugin to make the request 
  // by default the only valid response status range is 100:399
  response = httpRequest(
    htttpMode: 'POST',
    url: getUri,
    authentication: credential
    requestBody: body,
    customHeaders:[[name:"Content-Type", value:"text/xml"]]
  )
  return response
}