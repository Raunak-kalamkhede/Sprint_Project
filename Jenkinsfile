node {
    // 1. Tool Setup
    def mvnHome = tool 'Maven_3.9'
    def jdkHome = tool 'JDK21'
    
    // Inject tools directly into runtime environment paths
    withEnv(["PATH+MAVEN=${mvnHome}/bin", "PATH+JDK=${jdkHome}/bin"]) {
        
        try {
            stage('1. Environment Setup & Checkout') {
                echo 'Cleaning workspace and pulling latest code from repository...'
                cleanWs()
                checkout scm
            }

            stage('2. Artifact Compilation') {
                echo 'Compiling Java classes and verifying dependencies...'
                bat "mvn clean compile -DskipTests"
            }

            stage('3. Functional Automation Suite') {
                echo 'Executing Cucumber UI, API, and Hybrid E2E Test Matrix in Parallel...'
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    bat "mvn test -Dheadless=true"
                }
            }

            stage('4. JMeter Performance Load Test') {
                echo 'Launching Headless JMeter Performance Engine for 100 Concurrent Users...'
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                    dir('performance-testing') {
                        bat "jmeter -n -t Notes_Load_Test.jmx -l results.jtl -e -o html-report"
                    }
                }
            }

            stage('5. Allure HTML Report Compilation') {
                echo 'Converting raw test result JSON binaries into interactive Allure Dashboard...'
                bat "mvn allure:report"
            }

        } finally {
            // This block replaces the 'post { always { ... } }' block cleanly
            stage('6. Publishing Reports & Archiving') {
                echo 'Archiving screenshots, processing logs, and publishing reporting matrices...'
                
                // Publish Allure
                allure includeProperties: false, results: [[path: 'target/allure-results']]
                
                // Publish Cucumber Results
                cucumber fileIncludePattern: '**/cucumber-report.json', jsonReportDirectory: 'target'
                
                // Archive Failures and Logs
                archiveArtifacts artifacts: 'target/screenshots/*.png, logs/*.log, performance-testing/results.jtl', allowEmptyArchive: true
                
                // Publish JMeter Dashboard
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
}