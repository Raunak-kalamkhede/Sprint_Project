pipeline {
    agent any

    // Define tools globally based on your project prerequisites
    tools {
        maven 'Maven_3.9'
        jdk 'Java_11'
    }

    // Pipeline behavior constraints
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
                // Verifies compile-time safety before spinning up browser instances
                sh 'mvn clean compile -DskipTests'
            }
        }

        stage('3. Functional Automation Suite') {
            steps {
                echo 'Executing Cucumber UI, API, and Hybrid E2E Test Matrix in Parallel...'
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    // Executes headlessly on Jenkins agents. TestNG XML controls threads.
                    // AnnotationTransformer dynamically manages reruns for flaky anomalies.
                    sh 'mvn test -Dheadless=true'
                }
            }
        }

        stage('4. JMeter Performance Load Test') {
            steps {
                echo 'Launching Headless JMeter Performance Engine for 100 Concurrent Users...'
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    // Navigates to performance folder, runs JMeter in non-GUI mode, and outputs an HTML report
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

    // Post-Execution reporting pipeline block
    post {
        always {
            echo 'Archiving screenshots, processing logs, and publishing reporting matrices...'
            
            // 1. Publish interactive Allure Report dashboard inside Jenkins UI
            allure includeProperties: false, jdk: '', results: [[path: 'target/allure-results']]
            
            // 2. Publish native Cucumber plugin summary tracking charts
            cucumber buildStatusUrl: '', fileIncludePattern: '**/cucumber-report.json', jsonReportDirectory: 'target'
            
            // 3. Backup and archive failed browser screenshots and framework runtime log files
            archiveArtifacts artifacts: 'target/screenshots/*.png, logs/*.log, performance-testing/results.jtl', allowEmptyArchive: true
            
            // 4. Publish standalone JMeter HTML Dashboard directly to the build menu sidebar
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