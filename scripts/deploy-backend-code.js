const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const DETAILS = JSON.parse(fs.readFileSync('backend-details.json', 'utf8'));
const IP = DETAILS.PublicIp;
const KEY = DETAILS.KeyFile;
const SSH = `ssh -o StrictHostKeyChecking=no -i ${KEY} ubuntu@${IP}`;

const PROJECT_ROOT = path.resolve(__dirname, '../MachinaEar');
const WAR_PATH = path.join(PROJECT_ROOT, 'target', 'iam-0.1.0.war');

console.log('Building Backend...');
try {
    // Check if mvn exists
    execSync('mvn -version', { stdio: 'ignore' });
} catch (e) {
    console.error("Maven not found in PATH! Please install Maven.");
    process.exit(1);
}

try {
    // Build
    execSync('mvn clean package -DskipTests', { cwd: PROJECT_ROOT, stdio: 'inherit' });

    if (!fs.existsSync(WAR_PATH)) {
        throw new Error("WAR file not found after build!");
    }

    console.log(`Uploading WAR to ${IP}...`);
    execSync(`scp -o StrictHostKeyChecking=no -i ${KEY} "${WAR_PATH}" ubuntu@${IP}:~/ROOT.war`, { stdio: 'inherit' });

    console.log("Deploying to WildFly...");
    // Move to deployments folder. This triggers auto-deploy.
    const deployCmd = `
    sudo mv ~/ROOT.war /opt/wildfly/standalone/deployments/ROOT.war
    # Ensure ownership
    sudo chown ubuntu:ubuntu /opt/wildfly/standalone/deployments/ROOT.war
    `;

    execSync(`${SSH} "${deployCmd}"`, { stdio: 'inherit' });

    console.log("Deployment triggered. Watching logs...");
    // Tail logs briefy
    // execSync(`${SSH} "tail -f /opt/wildfly/standalone/log/server.log"`, { stdio: 'inherit', timeout: 10000 }).catch(() => {});

} catch (e) {
    console.error("Build/Deploy Failed:", e.message);
}
