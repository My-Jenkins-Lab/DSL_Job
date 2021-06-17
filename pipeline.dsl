import jenkins.model.*


job('From_dsl') {
  steps {
    shell('echo Hello World!')
  }
}

pipelineJob('example_pipeline') {
    definition {
        cps {
            script(readFileFromWorkspace('project-a-workflow.groovy'))
            sandbox()
        }
    }
}
