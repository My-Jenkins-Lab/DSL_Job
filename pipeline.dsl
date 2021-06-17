import jenkins.model.*


job('From_dsl') {
  steps {
    shell('echo Hello World!')
  }
}

for(int i =0 ; i < 3; i++){
  pipelineJob('example_pipeline'+ i ) {
      definition {
          cps {
              script(readFileFromWorkspace('project-a-workflow.groovy'))
              sandbox()
          }
      }
  }
}
