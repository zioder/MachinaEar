const { execSync } = require('child_process');
const fs = require('fs');
const DETAILS = JSON.parse(fs.readFileSync('ec2-details.json', 'utf8'));
const IP = DETAILS.PublicIp;
const KEY = DETAILS.KeyFile;
const SSH = `ssh -o StrictHostKeyChecking=no -i ${KEY} ubuntu@${IP}`;

console.log(`Re-deploying and fixing structure on ${IP}...`);

const fixScript = `
set -e

echo "Cleaning..."
rm -rf ~/temp_app_fix
mkdir -p ~/temp_app_fix

echo "Unzipping..."
unzip -q ~/app.zip -d ~/temp_app_fix

echo "Inspecting structure..."
find ~/temp_app_fix -maxdepth 5 -name "server.js"

# Identify the deeply nested folder containing server.js
SERVER_PATH=$(find ~/temp_app_fix -not -path '*/node_modules/*' -name "server.js" | head -n 1)
echo "Found server.js at: $SERVER_PATH"

APP_ROOT=$(dirname "$SERVER_PATH")
echo "App Root is: $APP_ROOT"

# Check what's in App Root
ls -F "$APP_ROOT"

# We want to move contents of APP_ROOT to /var/www/machinaear
# BUT we also need 'static' and 'public' if they are outside APP_ROOT?
# Standalone build usually puts .next/static inside .next of the APP_ROOT...
# Wait, if Next.js inferred workspace root, the standalone output structure is:
# .next/standalone/
#    package.json
#    server.js
#    .next/server/...
# OR
# .next/standalone/
#    [project root]/
#       server.js
#    node_modules/ (shared?)

# Let's see where node_modules is.
NM_PATH=$(find ~/temp_app_fix -maxdepth 5 -type d -name "node_modules" | head -n 1)
echo "Found node_modules at: $NM_PATH"

# If node_modules is outside APP_ROOT, we need to move it into /var/www/machinaear along with contents of APP_ROOT?
# OR does server.js expect ../node_modules?
# Usually server.js expects node_modules in PWD.

# Let's reset /var/www/machinaear
sudo mkdir -p /var/www/machinaear
sudo chown -R ubuntu:ubuntu /var/www/machinaear
sudo rm -rf /var/www/machinaear/*

# Move APP_ROOT contents to destination
echo "Moving APP_ROOT contents..."
sudo cp -r "$APP_ROOT"/* /var/www/machinaear/

# If node_modules was not inside APP_ROOT, copy it too
if [[ "$NM_PATH" != *"$APP_ROOT"* ]]; then
    echo "node_modules is outside app root. Copying/Linking..."
    # If it's a monorepo structure, node_modules might be up.
    # We should merge it?
    # cp -r $NM_PATH /var/www/machinaear/node_modules
    # Be careful not to overwrite if app root has its own.
    sudo cp -rn "$NM_PATH" /var/www/machinaear/
fi

# Also static and public?
# In my package-app.js I put .next and public at the ROOT of the zip.
# So they are at ~/temp_app_fix/.next and ~/temp_app_fix/public
# They need to be in /var/www/machinaear/.next and /var/www/machinaear/public
# BUT wait, server.js (if nested) might imply a different structure.
# Standalone server.js usually looks for .next in the same directory.
# So we should copy .next and public from temp_app_fix root to /var/www/machinaear

echo "Copying .next and public from zip root..."
if [ -d ~/temp_app_fix/public ]; then
    sudo cp -r ~/temp_app_fix/public /var/www/machinaear/
fi
if [ -d ~/temp_app_fix/.next ]; then
    sudo cp -r ~/temp_app_fix/.next /var/www/machinaear/
fi

# Fix permissions
sudo chown -R ubuntu:ubuntu /var/www/machinaear

# Verify 'server.js' is at root of logic
if [ ! -f /var/www/machinaear/server.js ]; then
    echo "WARNING: server.js not found at /var/www/machinaear/server.js"
    # It might be fine if we moved it.
fi

echo "Restarting PM2..."
cd /var/www/machinaear
pm2 delete machinaear || true
PORT=3000 pm2 start server.js --name machinaear --update-env
pm2 save

sleep 5
pm2 logs machinaear --lines 20 --nostream
`;

fs.writeFileSync('redeploy-fix.sh', fixScript);

try {
    execSync(`scp -o StrictHostKeyChecking=no -i ${KEY} redeploy-fix.sh ubuntu@${IP}:~/redeploy.sh`, { stdio: 'inherit' });
    execSync(`${SSH} "chmod +x redeploy.sh && ./redeploy.sh"`, { stdio: 'inherit' });
} catch (e) {
    console.log("Redeploy script failed cleanly (might process exit)");
}
