node { ansiColor('xterm') { timestamps {

    stage('checkout') {
checkout([$class: 'GitSCM', branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[credentialsId: '6833d37d-c1ff-47fd-9bd9-ce1f4cb2d763', url: 'https://github.com/vipulsonar26/DSL_Job.git']]])
    }

    stage('Create branch build Pipeline') {

    }

    stage('Create Github Jobs') {
        jobDsl failOnSeedCollision: true, removedConfigFilesAction: 'DELETE', removedJobAction: 'DELETE', removedViewAction: 'DELETE', targets: 'Pipeline.dsl'
    }

} } }
