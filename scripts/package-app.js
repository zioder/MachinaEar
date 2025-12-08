const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const standaloneDir = path.join('.next', 'standalone');
const publicSrc = 'public';
const publicDest = path.join(standaloneDir, 'public');

console.log('Preparing deployment package...');

try {
    // The standalone build needs the complete .next directory
    // But we can't copy .next to .next/standalone/.next (circular)
    // So we copy the contents of .next excluding standalone
    console.log('Copying .next directory contents...');
    const nextSrc = '.next';
    const nextDest = path.join(standaloneDir, '.next');

    // Remove existing .next in standalone if it exists
    if (fs.existsSync(nextDest)) {
        fs.rmSync(nextDest, { recursive: true, force: true });
    }
    fs.mkdirSync(nextDest, { recursive: true });

    // Copy each item in .next except 'standalone'
    const nextItems = fs.readdirSync(nextSrc);
    for (const item of nextItems) {
        if (item !== 'standalone') {
            const srcPath = path.join(nextSrc, item);
            const destPath = path.join(nextDest, item);
            console.log(`  Copying ${item}...`);
            fs.cpSync(srcPath, destPath, { recursive: true });
        }
    }

    // Copy public
    console.log('Copying public directory...');
    if (fs.existsSync(publicSrc)) {
        if (fs.existsSync(publicDest)) {
            fs.rmSync(publicDest, { recursive: true, force: true });
        }
        fs.cpSync(publicSrc, publicDest, { recursive: true });
    } else {
        console.warn('Warning: public directory not found!');
    }

    // Zip
    console.log('Zipping...');
    execSync(`powershell Compress-Archive -Path ".next\\\\standalone\\\\*" -DestinationPath deploy.zip -Force`, { stdio: 'inherit' });

    console.log('Package created: deploy.zip');
} catch (e) {
    console.error('Packaging failed:', e);
    process.exit(1);
}
