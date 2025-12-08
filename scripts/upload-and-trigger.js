const { execSync } = require('child_process');
const fs = require('fs');

const DETAILS = JSON.parse(fs.readFileSync('ec2-details.json', 'utf8'));
const IP = DETAILS.PublicIp;
const KEY = DETAILS.KeyFile;

console.log(`Uploading deploy.zip to ${IP} as app.zip...`);

try {
    execSync(`scp -o StrictHostKeyChecking=no -i ${KEY} deploy.zip ubuntu@${IP}:~/app.zip`, { stdio: 'inherit' });
    console.log("Upload Complete. Running redeploy script...");
    execSync('node redeploy-fix.js', { stdio: 'inherit' });
    console.log("Done.");
} catch (e) {
    console.error("Failed:", e.message);
}
