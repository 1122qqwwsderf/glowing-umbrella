(function () {
    function tick() {
        const d = new Date();
        const p = n => String(n).padStart(2, '0');
        const week = '日一二三四五六'[d.getDay()];
        const dateEl = document.getElementById('date');
        const timeEl = document.getElementById('time');
        if (dateEl) dateEl.textContent = d.getFullYear() + '年' + (d.getMonth() + 1) + '月' + d.getDate() + '日 周' + week;
        if (timeEl) timeEl.textContent = p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds());
    }
    tick();
    setInterval(tick, 1000);
})();
