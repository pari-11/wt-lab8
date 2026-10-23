// Dashboard UI. All data comes from the backend through window.CricketAPI (api.js).
(() => {
  const api = window.CricketAPI;
  const state = {
    matches: new Map(), selectedId: null, tab: 'scorecard', inningsTab: {}, filter: 'ALL',
    lastSeq: new Map(), simRunning: true, lb: 'runs', formRuns: 0,
  };
  const $ = s => document.querySelector(s);
  const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const current = () => state.matches.get(state.selectedId);

  // ---------- helpers ----------
  function scoreFor(m, teamId) {
    const inn = m.innings.find(i => i.battingTeam.id === teamId);
    if (!inn) return null;
    return { score: inn.wickets >= 10 ? `${inn.runs}` : `${inn.runs}/${inn.wickets}`, overs: inn.overs };
  }
  function ballClass(label) {
    if (!label) return '';
    if (label.endsWith('W')) return 'wkt';
    if (label === '4') return 'four';
    if (label === '6') return 'six';
    if (/wd|nb|lb|b/.test(label)) return 'extra';
    if (label === '0') return 'dot';
    return '';
  }
  const ballChip = l => `<span class="ball ${ballClass(l)}">${esc(l === '0' ? '•' : l)}</span>`;
  const badge = (t, lg) => `<span class="badge ${lg ? 'lg' : ''}" style="--c:${esc(t.color)}">${esc(t.shortName)}</span>`;
  function countdown(iso) {
    const ms = new Date(iso) - Date.now();
    if (ms <= 0) return 'Starting soon';
    const h = Math.floor(ms / 3.6e6), m = Math.floor(ms % 3.6e6 / 6e4);
    return `Starts in ${h ? h + 'h ' : ''}${m}m`;
  }
  const statusLine = m => m.status === 'UPCOMING' ? countdown(m.startTime) : m.statusText;

  // ---------- match strip ----------
  function renderStrip() {
    const list = [...state.matches.values()].filter(m => state.filter === 'ALL' || m.status === state.filter);
    const order = { LIVE: 0, UPCOMING: 1, COMPLETED: 2 };
    list.sort((a, b) => order[a.status] - order[b.status]);
    $('#matchStrip').innerHTML = list.length ? list.map(m => {
      const row = t => {
        const s = scoreFor(m, t.id);
        return `<div class="mc-team">${badge(t)}<span class="mc-name">${esc(t.name)}</span>
          <span class="mc-score">${s ? `${s.score}<small>(${s.overs})</small>` : ''}</span></div>`;
      };
      return `<button class="match-card ${m.id === state.selectedId ? 'selected' : ''}" data-id="${m.id}">
        <div class="mc-head"><span class="chip ${m.status}">${m.status}</span><span>${esc(m.matchType)} · ${esc(m.venue.split(',').pop().trim())}</span></div>
        ${row(m.teamA)}${row(m.teamB)}
        <p class="mc-status">${esc(statusLine(m))}</p></button>`;
    }).join('') : '<p class="empty">No matches</p>';
  }

  // ---------- hero ----------
  function renderHero() {
    const m = current();
    if (!m) { $('#hero').innerHTML = '<p class="empty">Select a match</p>'; return; }
    const battingId = m.status === 'LIVE' ? m.innings[m.currentInnings]?.battingTeam.id : null;
    const teamRow = t => {
      const s = scoreFor(m, t.id);
      return `<div class="hero-team ${t.id === battingId ? 'batting' : ''}">${badge(t, true)}
        <div class="ht-name">${esc(t.name)}${t.id === battingId ? '<span class="bat-dot" title="Batting"></span>' : ''}</div>
        <div class="ht-score">${s ? `${s.score}<small>(${s.overs} ov)</small>` : '<small>Yet to bat</small>'}</div></div>`;
    };
    let metrics = '', livePanel = '';
    const L = m.live;
    if (L) {
      const metric = (k, v) => `<div class="metric"><span>${k}</span><b>${v}</b></div>`;
      metrics = `<div class="metrics">${metric('CRR', L.currentRunRate.toFixed(2))}
        ${L.target ? metric('Target', L.target) + metric('Need', `${L.runsNeeded} <small style="font-size:12px;color:var(--muted)">off ${L.ballsRemaining}</small>`) + metric('RRR', L.requiredRunRate != null ? L.requiredRunRate.toFixed(2) : '-') : metric('Balls left', L.ballsRemaining)}</div>`;
      const bat = b => b ? `<tr class="${b.onStrike ? 'striker' : ''}"><td class="name">${esc(b.name)}</td><td><b>${b.runs}</b></td><td>${b.balls}</td><td>${b.fours}</td><td>${b.sixes}</td><td>${b.strikeRate.toFixed(1)}</td></tr>` : '';
      const bw = L.bowler;
      livePanel = `<div class="live-panel">
        <div class="table-scroll"><table><thead><tr><th>Batter</th><th>R</th><th>B</th><th>4s</th><th>6s</th><th>SR</th></tr></thead>
          <tbody>${bat(L.striker)}${bat(L.nonStriker)}</tbody></table></div>
        <div><div class="table-scroll"><table><thead><tr><th>Bowler</th><th>O</th><th>R</th><th>W</th><th>Econ</th></tr></thead>
          <tbody>${bw ? `<tr><td class="name">${esc(bw.name)}</td><td>${bw.overs}</td><td>${bw.runs}</td><td><b>${bw.wickets}</b></td><td>${bw.economy.toFixed(2)}</td></tr>` : ''}</tbody></table></div>
          <p class="sub-title" style="margin-top:12px">This over</p><div class="this-over">${L.thisOver.map(ballChip).join('') || '<span class="hint">—</span>'}</div></div>
      </div>`;
    } else if (m.status === 'COMPLETED' && m.playerOfTheMatch) {
      metrics = `<div class="metrics"><div class="metric"><span>Player of the match</span><b>${esc(m.playerOfTheMatch)}</b></div></div>`;
    } else if (m.status === 'UPCOMING') {
      metrics = `<div class="metrics"><div class="metric"><span>Start time</span><b>${new Date(m.startTime).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' })}</b></div></div>
        <p style="margin-top:14px"><button class="btn primary" id="startBtn">Start match now</button></p>`;
    }
    $('#hero').innerHTML = `<div class="hero-head"><span class="chip ${m.status}">${m.status}</span>
        <span>${esc(m.matchType)} · ${esc(m.venue)}</span></div>
      <div class="hero-teams">${teamRow(m.teamA)}${teamRow(m.teamB)}</div>
      <p class="status-text">${esc(statusLine(m))}</p>${metrics}${livePanel}`;
    $('#startBtn')?.addEventListener('click', async () => {
      try { onMatch(await api.startMatch(m.id)); } catch (e) { toast(e.message, 'wkt'); }
    });
  }

  // ---------- tabs ----------
  function renderTab() {
    const m = current();
    const el = $('#tabContent');
    if (!m) { el.innerHTML = ''; return; }
    if (state.tab === 'scorecard') el.innerHTML = scorecard(m);
    else if (state.tab === 'commentary') el.innerHTML = commentary(m);
    else if (state.tab === 'summary') renderSummary(m);
    else if (state.tab === 'update') renderUpdate(m);
  }

  function scorecard(m) {
    if (!m.innings.length) return '<p class="empty">Scorecard will appear once the match starts.</p>';
    const idx = Math.min(state.inningsTab[m.id] ?? m.currentInnings, m.innings.length - 1);
    const inn = m.innings[idx];
    const sw = m.innings.map((i, n) => `<button data-inn="${n}" class="${n === idx ? 'active' : ''}">${esc(i.battingTeam.shortName)} innings</button>`).join('');
    const bat = inn.batting.map(b => `<tr class="${b.onStrike ? 'striker' : ''}"><td class="name">${esc(b.name)}<small>${esc(b.dismissal)}</small></td>
      <td><b>${b.runs}</b></td><td>${b.balls}</td><td>${b.fours}</td><td>${b.sixes}</td><td>${b.strikeRate.toFixed(1)}</td></tr>`).join('');
    const bowl = inn.bowling.filter(b => b.overs !== '0.0').map(b => `<tr><td class="name">${esc(b.name)}</td><td>${b.overs}</td><td>${b.maidens}</td>
      <td>${b.runs}</td><td><b>${b.wickets}</b></td><td>${b.economy.toFixed(2)}</td></tr>`).join('');
    const x = inn.extras;
    return `<div class="inn-switch">${sw}</div>
      <div class="table-scroll"><table><thead><tr><th>Batter</th><th>R</th><th>B</th><th>4s</th><th>6s</th><th>SR</th></tr></thead><tbody>${bat}</tbody></table></div>
      <div class="sc-total"><span>Extras <small style="color:var(--muted);font-weight:400">(wd ${x.wides}, nb ${x.noBalls}, b ${x.byes}, lb ${x.legByes})</small></span><span>${x.total}</span></div>
      <div class="sc-total" style="border-top:1px solid var(--border)"><span>Total</span><span>${inn.runs}/${inn.wickets} (${inn.overs} ov, RR ${inn.runRate.toFixed(2)})</span></div>
      <p class="fow"><b>Fall of wickets:</b> ${inn.fallOfWickets.length ? esc(inn.fallOfWickets.join(', ')) : '—'}</p>
      <div class="table-scroll"><table><thead><tr><th>Bowler</th><th>O</th><th>M</th><th>R</th><th>W</th><th>Econ</th></tr></thead><tbody>${bowl}</tbody></table></div>`;
  }

  function commentary(m) {
    if (!m.commentary.length) return '<p class="empty">No commentary yet.</p>';
    return `<ul class="comm-list">${m.commentary.map(c => {
      if (!c.label) return `<li class="comm system"><p>${esc(c.text)}</p></li>`;
      const cls = ballClass(c.label);
      return `<li class="comm ${cls === 'wkt' || cls === 'six' || cls === 'four' ? 'hl-' + cls : ''}">${ballChip(c.label)}
        <div><div class="comm-over">Over ${esc(c.over)}</div><p>${esc(c.text)}</p></div></li>`;
    }).join('')}</ul>`;
  }

  async function renderSummary(m) {
    const el = $('#tabContent');
    try {
      const s = await api.getSummary(m.id);
      if (state.tab !== 'summary' || state.selectedId !== m.id) return;
      const perf = list => list.length ? list.map(p => `<div class="perf"><span>${esc(p.name)}<small>${esc(p.team)}</small></span><b>${esc(p.figures)}</b></div>`).join('') : '<p class="hint">—</p>';
      el.innerHTML = `${s.result ? `<div class="result-banner">🏆 ${esc(s.result)}${s.playerOfTheMatch ? ` · Player of the match: ${esc(s.playerOfTheMatch)}` : ''}</div>` : ''}
        <p class="sub-title">Scores</p><p>${s.scores.length ? s.scores.map(esc).join(' &nbsp;·&nbsp; ') : 'Match yet to begin'}</p>
        <div class="sum-grid"><div><p class="sub-title">Top batters</p>${perf(s.topBatters)}</div>
        <div><p class="sub-title">Top bowlers</p>${perf(s.topBowlers)}</div></div>`;
    } catch (e) { el.innerHTML = `<p class="empty">${esc(e.message)}</p>`; }
  }

  function renderUpdate(m) {
    const el = $('#tabContent');
    if (m.status !== 'LIVE') { el.innerHTML = '<p class="empty">Score updates can only be posted for live matches.</p>'; return; }
    if (el.querySelector('#ballForm')?.dataset.match === m.id) { // keep form state, refresh header only
      el.querySelector('#updateMeta').textContent = updateMeta(m);
      return;
    }
    el.innerHTML = `<form id="ballForm" class="ball-form" data-match="${m.id}">
      <p class="hint" id="updateMeta">${esc(updateMeta(m))}</p>
      <div class="field"><label>Runs</label><div class="seg" id="runsSeg">${[0, 1, 2, 3, 4, 5, 6].map(r => `<button type="button" data-runs="${r}" class="${r === state.formRuns ? 'active' : ''}">${r}</button>`).join('')}</div></div>
      <div class="field-row">
        <label>Extra<select name="extra"><option value="NONE">None</option><option value="WIDE">Wide</option><option value="NO_BALL">No ball</option><option value="BYE">Bye</option><option value="LEG_BYE">Leg bye</option></select></label>
        <label>Wicket<select name="dismissal"><option value="">Not out</option><option>BOWLED</option><option>CAUGHT</option><option>LBW</option><option>RUN_OUT</option><option>STUMPED</option></select></label>
      </div>
      <div><button class="btn primary" type="submit">Record delivery</button></div>
      <p class="form-msg" id="formMsg"></p>
      <p class="hint">Sends <code>POST /api/matches/${esc(m.id)}/balls</code>; every connected dashboard updates instantly.</p></form>`;
  }
  function updateMeta(m) {
    const L = m.live, inn = m.innings[m.currentInnings];
    return `${inn.battingTeam.shortName} ${inn.runs}/${inn.wickets} (${inn.overs}) · ${L.striker?.name ?? '-'} facing ${L.bowler?.name ?? '-'}`;
  }

  // ---------- side panels ----------
  function renderManhattan() {
    const m = current(), el = $('#manhattan');
    if (!m || !m.innings.length) { el.innerHTML = '<p class="empty">No overs bowled yet.</p>'; return; }
    const W = 340, H = 170, padL = 22, padB = 18, overs = m.oversPerInnings;
    const max = Math.max(12, ...m.innings.flatMap(i => i.runsPerOver));
    const gw = (W - padL) / overs, bw = gw / m.innings.length * 0.78;
    const y = v => (H - padB) - v / max * (H - padB - 6);
    let svg = '';
    for (let g = 6; g <= max; g += 6) svg += `<line x1="${padL}" x2="${W}" y1="${y(g)}" y2="${y(g)}" stroke="#22304a"/><text x="0" y="${y(g) + 4}" fill="#8a96ad" font-size="9">${g}</text>`;
    m.innings.forEach((inn, s) => inn.runsPerOver.forEach((r, o) => {
      svg += `<rect x="${padL + o * gw + s * bw + gw * 0.11}" y="${y(r)}" width="${bw}" height="${Math.max(0, (H - padB) - y(r))}" rx="1.5" fill="${inn.battingTeam.color}"><title>Over ${o + 1}: ${r}</title></rect>`;
    }));
    for (let o = 5; o <= overs; o += 5) svg += `<text x="${padL + (o - 0.5) * gw}" y="${H - 4}" fill="#8a96ad" font-size="9" text-anchor="middle">${o}</text>`;
    el.innerHTML = `<div class="chart"><svg viewBox="0 0 ${W} ${H}">${svg}</svg></div>
      <div class="legend">${m.innings.map(i => `<span><i style="background:${i.battingTeam.color}"></i>${esc(i.battingTeam.shortName)}</span>`).join('')}</div>`;
  }

  async function refreshLeaders() {
    try {
      const rows = await api.getLeaderboard(state.lb, 5);
      const isRuns = state.lb === 'runs';
      $('#leaders').innerHTML = `<div class="lb-tabs"><button data-lb="runs" class="${isRuns ? 'active' : ''}">Most runs</button>
        <button data-lb="wickets" class="${!isRuns ? 'active' : ''}">Most wickets</button></div>
        ${rows.map((p, i) => `<div class="lb-row"><span class="lb-rank">${i + 1}</span>
          <span class="lb-name">${esc(p.name)} <span class="badge" style="--c:${esc(p.teamColor)};height:18px;font-size:10px;min-width:28px">${esc(p.team)}</span>
          <small>${isRuns ? `${p.ballsFaced} balls · SR ${p.strikeRate.toFixed(1)} · HS ${p.highestScore}` : `${p.overs} ov · Econ ${p.economy != null ? p.economy.toFixed(2) : '-'}`}</small></span>
          <span class="lb-val">${isRuns ? p.runs : p.wickets}</span></div>`).join('')}`;
    } catch (_) { /* keep last render */ }
  }

  // ---------- live updates ----------
  function toast(text, cls) {
    const t = $('#toast');
    t.textContent = text;
    t.className = `toast show ${cls}`;
    clearTimeout(toast.timer);
    toast.timer = setTimeout(() => t.classList.remove('show'), 1600);
  }

  function onMatch(view) {
    const prev = state.lastSeq.get(view.id);
    const latest = view.commentary[0];
    state.matches.set(view.id, view);
    if (latest) {
      state.lastSeq.set(view.id, latest.sequence);
      if (prev != null && latest.sequence > prev && view.id === state.selectedId) {
        const c = ballClass(latest.label);
        if (c === 'wkt') toast('WICKET!', 'wkt');
        else if (c === 'six') toast('SIX!', 'six');
        else if (c === 'four') toast('FOUR!', 'four');
      }
    }
    renderStrip();
    if (view.id === state.selectedId) { renderHero(); renderTab(); renderManhattan(); }
  }

  function setConnection(status) {
    const el = $('#connection');
    const map = { connected: ['ok', 'Live feed connected'], reconnecting: ['warn', 'Reconnecting…'] };
    const [cls, text] = map[status] || ['', status];
    el.className = `conn ${cls}`;
    el.textContent = text;
  }

  async function refreshSimulator() {
    try {
      state.simRunning = (await api.getSimulator()).running;
      $('#simToggle').textContent = state.simRunning ? 'Pause feed' : 'Resume feed';
    } catch (_) { /* ignore */ }
  }

  // ---------- events ----------
  function bind() {
    $('#matchStrip').addEventListener('click', e => {
      const card = e.target.closest('.match-card');
      if (!card) return;
      state.selectedId = card.dataset.id;
      renderStrip(); renderHero(); renderTab(); renderManhattan();
    });
    $('#filters').addEventListener('click', e => {
      const b = e.target.closest('button');
      if (!b) return;
      state.filter = b.dataset.filter;
      document.querySelectorAll('#filters button').forEach(x => x.classList.toggle('active', x === b));
      renderStrip();
    });
    $('#tabs').addEventListener('click', e => {
      const b = e.target.closest('button');
      if (!b) return;
      state.tab = b.dataset.tab;
      document.querySelectorAll('#tabs button').forEach(x => x.classList.toggle('active', x === b));
      $('#tabContent').innerHTML = '';
      renderTab();
    });
    $('#tabContent').addEventListener('click', e => {
      const inn = e.target.closest('[data-inn]');
      if (inn) { state.inningsTab[state.selectedId] = +inn.dataset.inn; renderTab(); }
      const r = e.target.closest('[data-runs]');
      if (r) {
        state.formRuns = +r.dataset.runs;
        document.querySelectorAll('#runsSeg button').forEach(x => x.classList.toggle('active', x === r));
      }
    });
    $('#tabContent').addEventListener('submit', async e => {
      e.preventDefault();
      const f = e.target, msg = $('#formMsg');
      const body = { runs: state.formRuns, extra: f.extra.value, dismissal: f.dismissal.value || null };
      try {
        const view = await api.recordBall(f.dataset.match, body);
        msg.className = 'form-msg ok';
        msg.textContent = `Recorded: ${view.commentary[0]?.text ?? 'ok'}`;
        onMatch(view);
      } catch (err) {
        msg.className = 'form-msg err';
        msg.textContent = err.message;
      }
    });
    $('#leaders').addEventListener('click', e => {
      const b = e.target.closest('[data-lb]');
      if (b) { state.lb = b.dataset.lb; refreshLeaders(); }
    });
    $('#simToggle').addEventListener('click', async () => {
      try { await api.setSimulator(!state.simRunning); } catch (_) { /* ignore */ }
      refreshSimulator();
    });
  }

  async function init() {
    bind();
    const tick = () => { $('#clock').textContent = new Date().toLocaleTimeString(); };
    tick(); setInterval(tick, 1000);
    try {
      const list = await api.listMatches();
      list.forEach(m => { state.matches.set(m.id, m); if (m.commentary[0]) state.lastSeq.set(m.id, m.commentary[0].sequence); });
      state.selectedId = (list.find(m => m.status === 'LIVE') || list[0])?.id ?? null;
    } catch (e) {
      $('#hero').innerHTML = `<p class="empty">Could not load matches: ${esc(e.message)}</p>`;
    }
    renderStrip(); renderHero(); renderTab(); renderManhattan();
    api.subscribe(onMatch, setConnection);
    refreshLeaders(); setInterval(refreshLeaders, 5000);
    refreshSimulator();
  }

  init();
})();
