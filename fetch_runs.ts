async function fetchRuns() {
    const res = await fetch('https://api.github.com/repos/saadddd1/twoledger/actions/runs');
    const d = await res.json();
    d.workflow_runs.forEach(r => console.log(r.id, r.path, r.conclusion, r.created_at));
}
fetchRuns();
