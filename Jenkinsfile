post {
        always {
            echo 'Archiving screenshots, processing logs, and publishing reporting matrices...'
            
            // 1. Fixed Syntax: Cleanest, universally accepted parameter block for the Allure step
            allure includeProperties: false, results: [[path: 'target/allure-results']]
            
            // 2. Cucumber report tracking block
            cucumber fileIncludePattern: '**/cucumber-report.json', jsonReportDirectory: 'target'
            
            // 3. Backup and archive files
            archiveArtifacts artifacts: 'target/screenshots/*.png, logs/*.log, performance-testing/results.jtl', allowEmptyArchive: true
            
            // 4. Publish standalone JMeter HTML Dashboard
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
