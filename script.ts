import fs from 'fs';

async function fetchLog() {
    const res = await fetch('https://api.github.com/repos/saadddd1/twoledger/actions/jobs/72638223369/logs', {
        headers: { 'User-Agent': 'fetch-script' }
    });
    if (res.status === 302 || res.status === 301) {
        const redirected = await fetch(res.headers.get('location'));
        const text = await redirected.text();
        fs.writeFileSync('github_log.txt', text);
    } else {
        const text = await res.text();
        fs.writeFileSync('github_log.txt', text);
    }
}
fetchLog().catch(console.error);
