import jenkins.model.*


job('From_dsl') {
  steps {
    shell('echo Hello World!')
  }
  triggers {
  }
}
