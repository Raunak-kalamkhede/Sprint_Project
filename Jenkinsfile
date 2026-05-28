pipeline {
    agent any

    tools {
        maven 'Maven_3.9'
        jdk 'JDK21'
    }

    options {
        timeout(time: 15, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    stages {
        stage('1. Environment Setup & Checkout') {
            steps {
                echo 'Cleaning workspace and pulling latest code from repository...'
                cleanWs()
                checkout scm
            }
        }

        stage('2. Artifact Compilation') {
            steps {
                echo 'Compiling Java classes and verifying dependencies...'
                sh 'mvn clean compile -DskipTests'
            }
        }

        stage('3. Functional Automation Suite') {
            steps {
                echo 'Executing Cucumber UI, API, and Hybrid E2E Test Matrix in Parallel...'
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    sh 'mvn test -Dheadless=true'
                }
            }
        }

        stage('4. JMeter Performance Load Test') {
            steps {
                echo 'Launching Headless JMeter Performance Engine for 100 Concurrent Users...'
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    dir('performance-testing') {
                        sh 'jmeter -n -t Notes_Load_Test.jmx -l results.jtl -e -o html-report'
                    }
                }
            }
        }

        stage('5. Allure HTML Report Compilation') {
            steps {
                echo 'Converting raw test result JSON binaries into interactive Allure Dashboard...'
                sh 'mvn allure:report'
            }
        }
    }

    post {
        always {
            echo 'Archiving screenshots, processing logs, and publishing reporting matrices...'
            
            allure([
                includeProperties: false,
                jdk: '',
                properties: [],
                reportBuildPolicy: 'ALWAYS',
                results: [[path: 'target/allure-results']]
            ])
            
            cucumber fileIncludePattern: '**/cucumber-report.json', jsonReportDirectory: 'target'
            
            archiveArtifacts artifacts: 'target/screenshots/*.png, logs/*.log, performance-testing/results.jtl', allowEmptyArchive: true
            
            publishHTML(target: [
                allowMissing: false,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'performance-testing/html-report',
                reportFiles: 'index.html',
                reportName: 'JMeter Performance Load Report'
            ])
        }
    }
}