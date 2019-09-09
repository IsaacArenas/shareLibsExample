/**
  Author : Isaac Arenas
  Description: Function to Execute a cloudFormationScript
  Pipeline Call Sintax:
  ExecuteCloudFormation([
    gitRepo: "string containing the git ssh URL ",
    gitBranch: "string containing the name of the Git repo Branch ",
    cfTemplate: "path to the cfTemplate To execute",
    stackName: "string containing the name of the stack to be created"
    parameters: "list of parameters"   
  ])

assumptions : 
the node already has a .aws folder containing the configuration and credentials files 
the credential files have 2 profiles one for the user and one for the role to be assumed by the user
credentials file example:
-----
 [default]
aws_access_key_id = AKIAV6CZYRPSBGFSLSJ
aws_secret_access_key = 9GapUYMeTG13131ADADDA3tZrgSegIxdfNgO1
[devops]
role_arn = arn:aws:iam::11111:role/devopsRole
source_profile = default
role_session_name = devops-session
                                      
**/

def call(Map inputs){
  //setting up some initial configuration
  profileName= "devops"
  credentials= "fakeCredentials"
  defaultNode="fakeNode"

  node(defaultNode){
    //cleaning Up the Workspace
    stage("cleaning workspace"){
      cleanWs()
    }
    //stage to clone the git Repository that contains the cloudformation template
    stage("clone git repository"){
      checkout([$class: 'gitSCM', branches: [[name: inputs.gitBranch]]],
      doGenerateSubModuleConfigurations: false,
      gitTool: 'GIT', submoduleCfg:[],
      userRemoteConfigs[[credentialsId: fakeCredentials , url: gitRepo ]])
    }
    //stage to generate the configuration that is going to be used by the AWS CLI
    stage ("generating Configuation"){
      env.CFN_PROFILE= (profileName!= "")? "--profile ${profileName} " : ""
      env.CFN_PARAMETERS= (inpust.parameters!="")? "--parameters " +inputs.parameters.replaceAll("\n", ","): ""
      env.CFN_STACK_NAME= inputs.stackName
      env.CFN_TEMPLATE= inputs.cfTemplate
      inputs.parameters 

    }
    //stage to Execute the AWS CLI for create or update the cfn stack
    stage("Execute Template"){
      sh '''
      if aws ${CFN_PROFILE}cloudformation list-stacks | grep -q \"${CFN_STACK_NAME}\" ; then 
        echo "update existing stack"
          aws ${CFN_PROFILE}cloudformation deploy --template-file ${CFN_TEMPLATE} --stack-name ${CFN_STACK_NAME} ${CFN_PARAMETERS}
          aws ${CFN_PROFILE}cloudformation wait stack-update-complete --stack-name ${CFN_STACK_NAME}
      else
        echo "create new stack"
        aws ${CFN_PROFILE}cloudformation deploy --template-file ${CFN_TEMPLATE} --stack-name ${CFN_STACK_NAME} ${CFN_PARAMETERS}
        aws ${CFN_PROFILE}cloudformation wait stack-create-complete --stack-name ${CFN_STACK_NAME}  
      fi
      '''
    }
  
  }
}