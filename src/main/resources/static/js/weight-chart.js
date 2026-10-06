(function () {
    const canvas = document.getElementById('weightChart');
    if (!canvas || typeof Chart === 'undefined' || EPOCH_DAYS.length === 0) return;

    const DAY_MS = 86400000;
    const points = EPOCH_DAYS.map((day, i) => ({ x: day, y: WEIGHTS[i] }));

    // 7-day trailing average, computed on the full history so range edges aren't skewed.
    const average = points.map(p => {
        const win = points.filter(q => q.x <= p.x && q.x > p.x - 7);
        const mean = win.reduce((sum, q) => sum + q.y, 0) / win.length;
        return { x: p.x, y: Math.round(mean * 10) / 10 };
    });

    const shortDate = day => new Date(day * DAY_MS)
        .toLocaleDateString(undefined, { month: 'short', day: 'numeric', timeZone: 'UTC' });
    const longDate = day => new Date(day * DAY_MS)
        .toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric', timeZone: 'UTC' });
    const fmt = value => (Math.round(value * 10) / 10).toString();

    Chart.defaults.color = '#c9ccd3';
    Chart.defaults.borderColor = 'rgba(255, 255, 255, 0.08)';

    const chart = new Chart(canvas, {
        type: 'line',
        data: {
            datasets: [
                { label: 'Weight (lbs)', data: [], borderColor: '#8ab4ff', backgroundColor: '#8ab4ff',
                  borderWidth: 2, pointRadius: 3, tension: 0.15 },
                { label: '7-day average', data: [], borderColor: '#f0c24b', borderDash: [6, 4],
                  borderWidth: 2, pointRadius: 0, tension: 0.3 }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: { type: 'linear', ticks: { callback: v => shortDate(v), maxTicksLimit: 7 } },
                y: { title: { display: true, text: 'lbs' }, grace: '5%' }
            },
            plugins: {
                tooltip: { callbacks: { title: items => longDate(items[0].parsed.x) } }
            }
        }
    });

    const el = id => document.getElementById(id);

    function apply(rangeDays) {
        const from = rangeDays === 0 ? -Infinity : TODAY - rangeDays;
        const shown = points.filter(p => p.x >= from);
        const shownAvg = average.filter(p => p.x >= from);

        chart.data.datasets[0].data = shown;
        chart.data.datasets[1].data = shownAvg;
        chart.update();

        el('rangeEmpty').hidden = shown.length > 0;
        el('statCurrent').textContent = fmt(points[points.length - 1].y) + ' lbs';

        if (shown.length === 0) {
            ['statChange', 'statMin', 'statMax'].forEach(id => el(id).textContent = '-');
            return;
        }
        const values = shown.map(p => p.y);
        const change = shown[shown.length - 1].y - shown[0].y;
        el('statChange').textContent = (change > 0 ? '+' : '') + fmt(change) + ' lbs';
        el('statMin').textContent = fmt(Math.min(...values)) + ' lbs';
        el('statMax').textContent = fmt(Math.max(...values)) + ' lbs';
    }

    document.querySelectorAll('.range-buttons button').forEach(button => {
        button.addEventListener('click', () => {
            document.querySelectorAll('.range-buttons button').forEach(b => b.classList.remove('active'));
            button.classList.add('active');
            apply(parseInt(button.dataset.range, 10));
        });
    });

    apply(90);
})();