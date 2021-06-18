//  (c) Copyright 2019-2021 Micro Focus or one of its affiliates.
//
//  The only warranties for products and services of Micro Focus and
//  its affiliates and licensors ("Micro Focus") are as may be set
//  forth in the express warranty statements accompanying such products
//  and services. Nothing herein should be construed as constituting an
//  additional warranty. Micro Focus shall not be liable for technical
//  or editorial errors or omissions contained herein. The information
//  contained herein is subject to change without notice.
//
//  Except as specifically indicated otherwise, this document contains
//  confidential information and a valid license is required for
//  possession, use or copying. If this work is provided to the U.S.
//  Government, consistent with FAR 12.211 and 12.212, Commercial
//  Computer Software, Computer Software Documentation, and Technical
//  Data for Commercial Items are licensed to the U.S. Government under
//  vendor's standard commercial license.

// Properties for the build, selecting SUTs to run test ...
properties(
    [parameters ([
 //     booleanParam(name: 'run_aix64', defaultValue: true, description: 'AIX 7.1 64b'),
 //     booleanParam(name: 'run_aix32', defaultValue: true, description: 'AIX 7.1 32b'),
 //     booleanParam(name: 'run_darwin64', defaultValue: true, description: 'Apple MAC (darwin) 64b'),
 //     booleanParam(name: 'run_hpguard32', defaultValue: true, description: 'HP NONSTOP Guardian 32b'),
 //     booleanParam(name: 'run_hposs32', defaultValue: true, description: 'HP NONSTOP OSS 32b'),
 //     booleanParam(name: 'run_hpux32', defaultValue: true, description: 'HP UX 32b'),
 //     booleanParam(name: 'run_hpux64', defaultValue: true, description: 'HP UX 64b'),
	    	booleanParam(name: 'run_linux64', defaultValue: true, description: 'Linux 64b'),
 //     booleanParam(name: 'run_sparc32', defaultValue: true, description: 'Solaris SPARC 32b'),
 //     booleanParam(name: 'run_sparc64', defaultValue: true, description: 'Solaris SPARC 64b'),
 //     booleanParam(name: 'run_win32', defaultValue: true, description: 'Windows 32b'),
 //     booleanParam(name: 'run_win64', defaultValue: true, description: 'Windows 64b'),
 //     booleanParam(name: 'run_winmd32', defaultValue: true, description: 'Windows 32b'),
 //		booleanParam(name: 'run_winmd64', defaultValue: true, description: 'Windows 64b')
    ])]
)

// Set the upstream source package name, so we don't wind up using the very long repo name
def sourcePackage = "rfclient"
def scriptsPackage = "scripts"

def rftestRepo = "https://github.houston.softwaregrp.net/Voltage-Core-Crypto/RF-Tests-Payments.git"
def rftestRepoUrl = "https://github.houston.softwaregrp.net/Voltage-Core-Crypto/RF-Tests-Payments"

def version = "master"
// Some settings for github
def scmCreds = "microfocus-github"
def remote_server_port = "8444"

// The sdshared repo (NA for now)
//def sdsharedRepo = "https://github.houston.softwaregrp.net/Voltage-Core-Crypto/Sdshared.git"
//def sdsharedRepoUrl = "https://github.houston.softwaregrp.net/Voltage-Core-Crypto/Sdshared"
//def sdsharedRepoBranch = "*/master"

def labels = []

// Add a small description to this build
currentBuild.description = "#${BUILD_NUMBER}, ${version}"

stage("Skipped Platforms") {
	def labelsOffline = []
	def labelsSkipped = []

	// Parse the parameters to determine which builds to do.. make sure node are online.
	for (p in params) {
		if (p.key.substring(0,4) == 'run_') {
			// If we we are not marked to build, add it to labelsSkipped.
			if(params."$p.key" == false) {
				echo "Skipped - $p.key"
				labelsSkipped << p.key
				continue
			}

			def thisLabel = Jenkins.instance.getLabel(p.key)
			def isOnline = false
			if(thisLabel.isAssignable()) {
				// Is this a provisionable resource?
				for (aCloud in thisLabel.getClouds()) {
					if(aCloud.canProvision(thisLabel)) {
						echo "Provisionable - $p.key"
						labels << p.key
						isOnline = true
						break
					}
				}
				// Is this a computer node?
				for (aNode in thisLabel.getNodes()) {
					if(aNode.toComputer().isOnline())  {
						echo "System Online - $p.key"
						labels << p.key
						isOnline = true
						break
					}
				}
			}
			// If we weren't marked as online, add it to labelsOffline.
			if (!isOnline) {
				echo "System Offline - $p.key"
				labelsOffline << p.key
			}
		}
	}

	// This defines the parallel jobs
	def builders = [:]
	// create a stub build node for labelsOffline and mark them not built.
	for (x in labelsSkipped) {
		def label = x  // needs to happen this way
		def platform = label.substring(4)
		builders[label] = {
			catchError(buildResult: "SUCCESS", message: 'Skipped', stageResult: "NOT_BUILT") {
				error "Skipped - $platform"
			}
		}
	}
	// create a stub build node for labelsOffline and mark them failed.
	for (x in labelsOffline) {
		def label = x  // needs to happen this way
		def platform = label.substring(4)
		builders[label] = {
			catchError(buildResult: "UNSTABLE", message: 'Offline', stageResult: "FAILURE") {
				error "Offline - $platform"
			}
		}
	}
	// This executes the parallel jobs for our non-building stub nodes.
	try {
		parallel builders
	} catch (err) {
		echo "One or more systems are offline. - $err"
	}
}

// On the master node, check out all rf client source.  Then stash the source..
node('edge_master') {
	stage('Checkout ' + version) {
		deleteDir()
		echo "Complete checkout"
	}
}

stage("Start Remote Server") {
	// This defines the parallel jobs
	def builders = [:]
	for (x in labels) {
		def label = x  // needs to happen this way
		def platform = label.substring(4)

		// Create a map to pass in to the 'parallel' step so we can fire all the remote server at once
		builders[label] = {
			// This is what we want to do for each node -> clean, download remote server binaries,  call the ci script to run remote server 
			node(label) {
				stage('run ' + platform) {
					
					echo "Remote server is run"
				}
			}
		}
	}

	// This executes the parallel jobs.   Wrap in try catch so one failure does not mark the whole build as failure.
	try {
		parallel builders
	} catch (err) {
		echo "One or more builds failed - $err"
		currentBuild.result = "UNSTABLE"
	}
}

stage('Wait for Remote Server to be Ready') {
    sleep(time:5,unit:"SECONDS")
}


stage('Run Host RF Test') {
	node('rf_client') {
		try {
			unstash name: sourcePackage + '-source-' + version 
		} catch (err) {
			echo "no stash for rf_client"
		} finally {}

		for (x in labels) {
			def label = x  // needs to happen this way
			def platform = label.substring(4)
			if (platform == "linux64") {
				remote_server_ip = '172.16.31.34'
			}

			def remote_server_ip = "none"
			if (platform == "linux64") {
				remote_server_ip = '172.16.31.34'
			}

			sh 'echo "test....."'
		}

        stage('Publish Robot results') {
          echo "published"
        }
	}
}



stage("Stop Remote Server") {
	// This defines the parallel jobs
	def builders = [:]
	for (x in labels) {
		def label = x  // needs to happen this way
		def platform = label.substring(4)

		// Create a map to pass in to the 'parallel' step so we can stop all the remote server at once
		builders[label] = {
			// This is what we want to do for each node -> call the ci script to stop remote server 
			node(label) {
				stage('stop ' + platform) {
					try {
						
						echo "Remote server is stopped"
					} catch (err) {
						currentBuild.result = "FAILED"
						// Remove this from further steps
						labels.remove(label)
						error "Stop remote server for $label failed - $err"
					}
				}
			}
		}
	}

	// This executes the parallel jobs.   Wrap in try catch so one failure does not mark the whole build as failure.
	try {
		parallel builders
	} catch (err) {
		echo "One or more builds failed - $err"
		currentBuild.result = "UNSTABLE"
	}
}

