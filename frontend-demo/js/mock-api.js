// Offline stand-in for the Spring Boot API: same method names and same JSON shapes,
// but the scoring engine and live feed run in the browser.
window.CricketAPI = (() => {
  const rnd = n => Math.floor(Math.random() * n);
  const pick = a => a[rnd(a.length)];
  const surname = n => n.split(' ').pop();
  const oversOf = b => `${Math.floor(b / 6)}.${b % 6}`;
  const r2 = v => Math.round(v * 100) / 100;
  const rate = (r, b) => b ? r2(r * 6 / b) : 0;
  const sr = (r, b) => b ? r2(r * 100 / b) : 0;

  // ---------- teams ----------
  const B = 'BATTER', WK = 'WICKET_KEEPER', AR = 'ALL_ROUNDER', BW = 'BOWLER';
  const teams = {};
  const players = {};
  function team(id, name, shortName, color, squad) {
    const t = { id, name, shortName, color, players: [] };
    squad.forEach(([n, role], i) => {
      const p = { id: `${id}-${i + 1}`, name: n, teamId: id, role, matches: 0, runs: 0, ballsFaced: 0, fours: 0, sixes: 0, highestScore: 0, dismissals: 0, ballsBowled: 0, runsConceded: 0, wickets: 0 };
      t.players.push(p); players[p.id] = p;
    });
    t.bowlers = t.players.filter(p => p.role === BW || p.role === AR).reverse();
    t.keeper = t.players.find(p => p.role === WK) || t.players[0];
    teams[id] = t;
    return t;
  }
  const ind = team('ind', 'India', 'IND', '#1f6feb', [['Rohit Sharma', B], ['Shubman Gill', B], ['Virat Kohli', B], ['Suryakumar Yadav', B], ['Rishabh Pant', WK], ['Hardik Pandya', AR], ['Ravindra Jadeja', AR], ['Axar Patel', AR], ['Kuldeep Yadav', BW], ['Jasprit Bumrah', BW], ['Mohammed Siraj', BW]]);
  const aus = team('aus', 'Australia', 'AUS', '#f5b700', [['Travis Head', B], ['David Warner', B], ['Mitchell Marsh', AR], ['Glenn Maxwell', AR], ['Marcus Stoinis', AR], ['Tim David', B], ['Matthew Wade', WK], ['Pat Cummins', BW], ['Mitchell Starc', BW], ['Adam Zampa', BW], ['Josh Hazlewood', BW]]);
  const eng = team('eng', 'England', 'ENG', '#e5484d', [['Jos Buttler', WK], ['Phil Salt', B], ['Will Jacks', AR], ['Jonny Bairstow', B], ['Harry Brook', B], ['Liam Livingstone', AR], ['Moeen Ali', AR], ['Chris Jordan', BW], ['Jofra Archer', BW], ['Adil Rashid', BW], ['Mark Wood', BW]]);
  const nz = team('nz', 'New Zealand', 'NZ', '#8b95a5', [['Devon Conway', WK], ['Finn Allen', B], ['Kane Williamson', B], ['Daryl Mitchell', B], ['Glenn Phillips', AR], ['Mark Chapman', B], ['Mitchell Santner', AR], ['Tim Southee', BW], ['Trent Boult', BW], ['Lockie Ferguson', BW], ['Ish Sodhi', BW]]);

  // ---------- innings engine (mirrors Innings.java) ----------
  function newInnings(number, bat, bowl, maxOvers, target) {
    const inn = { number, bat, bowl, maxOvers, target, batting: new Map(), bowling: new Map(), fow: [], runsPerOver: [], thisOver: [],
      runs: 0, wickets: 0, legal: 0, wides: 0, noBalls: 0, byes: 0, legByes: 0, next: 0, rot: 0, striker: null, nonStriker: null, bowler: null, overDone: false, completed: false };
    inn.striker = nextBatter(inn); inn.nonStriker = nextBatter(inn); inn.bowler = nextBowler(inn, null);
    return inn;
  }
  function nextBatter(inn) {
    const p = inn.bat.players[inn.next++];
    if (!p) return null;
    inn.batting.set(p.id, { p, runs: 0, balls: 0, fours: 0, sixes: 0, out: false, dismissal: 'not out' });
    return p.id;
  }
  function nextBowler(inn, prev) {
    const opts = inn.bowl.bowlers, maxBalls = Math.max(1, Math.ceil(inn.maxOvers / 5)) * 6;
    for (let i = 0; i < opts.length; i++) {
      const c = opts[(inn.rot + i) % opts.length], f = inn.bowling.get(c.id);
      if (c.id === prev || (f && f.balls >= maxBalls)) continue;
      inn.rot = (inn.rot + i + 1) % opts.length;
      if (!f) inn.bowling.set(c.id, { p: c, balls: 0, runs: 0, wickets: 0, maidens: 0, overRuns: 0 });
      return c.id;
    }
    const any = inn.bowl.players.find(p => p.id !== prev);
    if (!inn.bowling.has(any.id)) inn.bowling.set(any.id, { p: any, balls: 0, runs: 0, wickets: 0, maidens: 0, overRuns: 0 });
    return any.id;
  }
  function deliver(inn, extra, ran, dismissal) {
    if (inn.overDone) { inn.thisOver = []; inn.overDone = false; }
    const st = inn.batting.get(inn.striker), bw = inn.bowling.get(inn.bowler);
    const over = `${Math.floor(inn.legal / 6)}.${inn.legal % 6 + 1}`, oi = Math.floor(inn.legal / 6);
    const legal = extra !== 'WIDE' && extra !== 'NO_BALL';
    let bat = 0, ext = 0, charged = 0;
    if (extra === 'NONE') { bat = ran; charged = ran; }
    else if (extra === 'WIDE') { ext = 1 + ran; inn.wides += ext; charged = ext; }
    else if (extra === 'NO_BALL') { ext = 1; inn.noBalls++; bat = ran; charged = 1 + ran; }
    else if (extra === 'BYE') { ext = ran; inn.byes += ran; }
    else { ext = ran; inn.legByes += ran; }
    const total = bat + ext;
    inn.runs += total;
    while (inn.runsPerOver.length <= oi) inn.runsPerOver.push(0);
    inn.runsPerOver[oi] += total;
    const faced = extra !== 'WIDE', p = st.p;
    st.runs += bat; p.runs += bat;
    if (faced) { st.balls++; p.ballsFaced++; }
    if (bat === 4) { st.fours++; p.fours++; } else if (bat === 6) { st.sixes++; p.sixes++; }
    const wicket = !!dismissal, bwk = wicket && dismissal !== 'RUN_OUT';
    if (legal) { bw.balls++; bw.p.ballsBowled++; inn.legal++; }
    bw.runs += charged; bw.overRuns += charged; bw.p.runsConceded += charged;
    if (bwk) { bw.wickets++; bw.p.wickets++; }
    const label = extra === 'NONE' ? (wicket ? (ran ? ran + 'W' : 'W') : String(ran))
      : extra === 'WIDE' ? (ext === 1 ? 'wd' : ext + 'wd') : extra === 'NO_BALL' ? (bat ? bat + 'nb' : 'nb') : extra === 'BYE' ? ran + 'b' : ran + 'lb';
    inn.thisOver.push(label);
    p.highestScore = Math.max(p.highestScore, st.runs);
    let dtext = null;
    if (wicket) {
      inn.wickets++;
      const f = pick(inn.bowl.players), b = surname(bw.p.name);
      dtext = { BOWLED: `b ${b}`, LBW: `lbw b ${b}`, CAUGHT: f === bw.p ? `c & b ${b}` : `c ${surname(f.name)} b ${b}`,
        STUMPED: `st ${surname(inn.bowl.keeper.name)} b ${b}`, RUN_OUT: `run out (${surname(f.name)})` }[dismissal];
      st.out = true; st.dismissal = dtext; p.dismissals++;
      inn.fow.push(`${inn.runs}-${inn.wickets} (${p.name}, ${oversOf(inn.legal)} ov)`);
      inn.striker = nextBatter(inn);
    } else if (ran % 2 === 1) [inn.striker, inn.nonStriker] = [inn.nonStriker, inn.striker];
    const overComplete = legal && inn.legal % 6 === 0;
    if (overComplete) { if (bw.overRuns === 0) bw.maidens++; bw.overRuns = 0; }
    if (inn.wickets >= 10 || !inn.striker || inn.legal >= inn.maxOvers * 6 || (inn.target && inn.runs >= inn.target)) inn.completed = true;
    else if (overComplete) { [inn.striker, inn.nonStriker] = [inn.nonStriker, inn.striker]; inn.bowler = nextBowler(inn, inn.bowler); inn.overDone = true; }
    return { over, batter: p, bowler: bw.p, extra, bat, total, label, wicket, dtext };
  }
  function describe(d) {
    const pre = `${d.over} ${surname(d.bowler.name)} to ${surname(d.batter.name)}, `;
    if (d.wicket) return pre + `OUT! ${d.dtext}. ` + pick(['Big breakthrough!', 'The crowd goes silent.', "That's a huge wicket.", 'Got him!']);
    if (d.extra === 'WIDE') return pre + 'wide, ' + pick(['drifting down leg', 'too far outside off']);
    if (d.extra === 'NO_BALL') return pre + 'NO BALL! Overstepped' + (d.bat ? `, and ${d.bat} off the bat` : '');
    if (d.extra === 'BYE') return pre + `${d.total} bye${d.total > 1 ? 's' : ''}, beats the keeper`;
    if (d.extra === 'LEG_BYE') return pre + `${d.total} leg bye${d.total > 1 ? 's' : ''}, off the pads`;
    return pre + ({ 0: pick(['no run, defended solidly', 'beaten outside off!', 'dot ball, good length', 'straight to the fielder']),
      1: '1 run, ' + pick(['worked to square leg', 'pushed to long-off', 'dabbed down to third man']),
      2: '2 runs, ' + pick(['placed into the gap, they come back for two', 'good running between the wickets']),
      3: '3 runs, chased down just inside the rope', 5: '5 runs',
      4: 'FOUR! ' + pick(['driven beautifully through the covers', 'pulled hard, races away to the rope', 'cut past point, no stopping that']),
      6: 'SIX! ' + pick(['launched over long-on', "that's gone miles into the stands", 'lofted cleanly over extra cover']) })[d.bat];
  }

  // ---------- matches (mirrors ScoringService.java) ----------
  const matches = [];
  let ids = 0;
  function createMatch(a, b, venue, start) {
    const m = { id: 'm' + (++ids), a, b, venue, overs: 20, start, status: 'UPCOMING', toss: null, innings: [], comm: [], result: null, potm: null, seq: 0 };
    matches.push(m);
    return m;
  }
  const sys = (m, inn, text) => m.comm.unshift({ sequence: ++m.seq, innings: inn, over: '', batter: '', bowler: '', runs: 0, label: '', wicket: false, boundary: false, text, timestamp: new Date().toISOString() });
  function startMatch(m) {
    if (m.status !== 'UPCOMING') throw new Error(`Match ${m.id} has already started`);
    const toss = Math.random() < .5 ? m.a : m.b, batFirst = Math.random() < .5;
    const first = batFirst ? toss : (toss === m.a ? m.b : m.a);
    m.toss = `${toss.name} won the toss and elected to ${batFirst ? 'bat' : 'bowl'}`;
    m.innings.push(newInnings(1, first, first === m.a ? m.b : m.a, m.overs, null));
    m.status = 'LIVE';
    if (m.start > Date.now()) m.start = Date.now();
    [...m.a.players, ...m.b.players].forEach(p => p.matches++);
    sys(m, 1, `Toss: ${m.toss}.`);
  }
  function recordBall(m, extra, runs, dismissal) {
    if (m.status !== 'LIVE') throw new Error(`Match ${m.id} is not live (status: ${m.status})`);
    if (dismissal && extra !== 'NONE') throw new Error('A wicket can only be recorded on a delivery without extras');
    if (dismissal && dismissal !== 'RUN_OUT' && runs > 0) throw new Error('Only a run out can have runs on the same delivery');
    if (extra === 'WIDE' && runs > 4) throw new Error('A wide can carry at most 4 extra runs');
    if ((extra === 'BYE' || extra === 'LEG_BYE') && (runs < 1 || runs > 4)) throw new Error('Byes and leg byes must be between 1 and 4 runs');
    const inn = m.innings[m.innings.length - 1];
    const d = deliver(inn, extra, runs, dismissal);
    m.comm.unshift({ sequence: ++m.seq, innings: inn.number, over: d.over, batter: d.batter.name, bowler: d.bowler.name, runs: d.total,
      label: d.label, wicket: d.wicket, boundary: extra !== 'WIDE' && (d.bat === 4 || d.bat === 6), text: describe(d), timestamp: new Date().toISOString() });
    if (m.comm.length > 400) m.comm.length = 400;
    if (!inn.completed) return;
    if (inn.number === 1) {
      const chase = newInnings(2, inn.bowl, inn.bat, m.overs, inn.runs + 1);
      m.innings.push(chase);
      sys(m, 1, `Innings break: ${inn.bat.shortName} ${inn.runs}/${inn.wickets} (${oversOf(inn.legal)} ov). ${chase.bat.name} need ${chase.target} to win.`);
    } else finish(m);
  }
  function finish(m) {
    const [f, s] = m.innings;
    let winner = null;
    if (s.runs >= s.target) {
      winner = s.bat;
      const w = 10 - s.wickets, left = m.overs * 6 - s.legal;
      m.result = `${winner.name} won by ${w} wicket${w === 1 ? '' : 's'}${left > 0 ? ` (${left} balls left)` : ''}`;
    } else if (s.runs === f.runs) m.result = 'Match tied';
    else { winner = f.bat; const d = f.runs - s.runs; m.result = `${winner.name} won by ${d} run${d === 1 ? '' : 's'}`; }
    const impact = new Map();
    m.innings.forEach(i => {
      i.batting.forEach(b => impact.set(b.p, (impact.get(b.p) || 0) + b.runs + (b.balls ? (b.runs - b.balls) * .5 : 0)));
      i.bowling.forEach(b => impact.set(b.p, (impact.get(b.p) || 0) + b.wickets * 25 - b.runs * .3));
    });
    m.potm = [...impact].filter(([p]) => !winner || p.teamId === winner.id).sort((x, y) => y[1] - x[1])[0]?.[0].name ?? null;
    m.status = 'COMPLETED';
    sys(m, 2, `Result: ${m.result}.`);
  }

  // ---------- views (same JSON as the backend) ----------
  const teamView = t => ({ id: t.id, name: t.name, shortName: t.shortName, color: t.color });
  function batterView(inn, id) {
    const b = id && inn.batting.get(id);
    if (!b) return null;
    return { playerId: id, name: b.p.name, runs: b.runs, balls: b.balls, fours: b.fours, sixes: b.sixes, strikeRate: sr(b.runs, b.balls),
      dismissal: b.dismissal, out: b.out, onStrike: !inn.completed && id === inn.striker };
  }
  const bowlerView = b => b && ({ playerId: b.p.id, name: b.p.name, overs: oversOf(b.balls), maidens: b.maidens, runs: b.runs, wickets: b.wickets, economy: rate(b.runs, b.balls) });
  function view(m) {
    const inn = m.innings[m.innings.length - 1];
    let live = null, statusText = m.status === 'UPCOMING' ? 'Match yet to begin' : m.result;
    if (m.status === 'LIVE') {
      const left = m.overs * 6 - inn.legal, need = inn.target ? Math.max(0, inn.target - inn.runs) : null;
      live = { striker: batterView(inn, inn.striker), nonStriker: batterView(inn, inn.nonStriker), bowler: bowlerView(inn.bowling.get(inn.bowler)),
        currentRunRate: rate(inn.runs, inn.legal), requiredRunRate: inn.target && left ? r2(need * 6 / left) : null,
        target: inn.target, runsNeeded: need, ballsRemaining: left, thisOver: [...inn.thisOver] };
      statusText = inn.target ? `${inn.bat.shortName} need ${need} run${need === 1 ? '' : 's'} in ${left} ball${left === 1 ? '' : 's'}` : m.toss;
    }
    return {
      id: m.id, title: `${m.a.shortName} vs ${m.b.shortName}`, matchType: 'T20', venue: m.venue, status: m.status, oversPerInnings: m.overs,
      startTime: new Date(m.start).toISOString(), teamA: teamView(m.a), teamB: teamView(m.b), tossInfo: m.toss, statusText, result: m.result,
      playerOfTheMatch: m.potm, currentInnings: m.innings.length - 1,
      innings: m.innings.map(i => ({ number: i.number, battingTeam: teamView(i.bat), bowlingTeam: teamView(i.bowl), runs: i.runs, wickets: i.wickets,
        overs: oversOf(i.legal), runRate: rate(i.runs, i.legal),
        extras: { total: i.wides + i.noBalls + i.byes + i.legByes, wides: i.wides, noBalls: i.noBalls, byes: i.byes, legByes: i.legByes },
        batting: [...i.batting.keys()].map(id => batterView(i, id)), bowling: [...i.bowling.values()].map(bowlerView),
        fallOfWickets: [...i.fow], runsPerOver: [...i.runsPerOver], target: i.target, completed: i.completed })),
      live, commentary: m.comm.slice(0, 30),
    };
  }
  function summary(m) {
    const bats = [], bowls = [];
    m.innings.forEach(i => {
      i.batting.forEach(b => bats.push({ name: b.p.name, team: i.bat.shortName, figures: `${b.runs}${b.out ? '' : '*'} (${b.balls})`, value: b.runs }));
      i.bowling.forEach(b => b.balls && bowls.push({ name: b.p.name, team: i.bowl.shortName, figures: `${b.wickets}/${b.runs} (${oversOf(b.balls)})`, value: b.wickets * 1000 - b.runs }));
    });
    const top = l => l.sort((x, y) => y.value - x.value).slice(0, 3);
    return { matchId: m.id, title: `${m.a.shortName} vs ${m.b.shortName}`, status: m.status, result: m.result, playerOfTheMatch: m.potm,
      scores: m.innings.map(i => `${i.bat.shortName} ${i.runs}/${i.wickets} (${oversOf(i.legal)} ov)`), topBatters: top(bats), topBowlers: top(bowls) };
  }
  function statsView(p) {
    const t = teams[p.teamId];
    return { id: p.id, name: p.name, team: t.shortName, teamColor: t.color, role: p.role, matches: p.matches, runs: p.runs, ballsFaced: p.ballsFaced,
      highestScore: p.highestScore, fours: p.fours, sixes: p.sixes, battingAverage: p.dismissals ? r2(p.runs / p.dismissals) : null,
      strikeRate: sr(p.runs, p.ballsFaced), wickets: p.wickets, overs: oversOf(p.ballsBowled), runsConceded: p.runsConceded,
      economy: p.ballsBowled ? rate(p.runsConceded, p.ballsBowled) : null };
  }

  // ---------- simulator (mirrors MatchSimulator.java) ----------
  function randomRuns() { const r = rnd(1000); return r < 330 ? 0 : r < 670 ? 1 : r < 760 ? 2 : r < 770 ? 3 : r < 920 ? 4 : 6; }
  function randomBall() {
    const r = rnd(1000);
    if (r < 40) {
      const x = rnd(100), how = x < 55 ? 'CAUGHT' : x < 75 ? 'BOWLED' : x < 90 ? 'LBW' : x < 96 ? 'RUN_OUT' : 'STUMPED';
      return ['NONE', how === 'RUN_OUT' && Math.random() < .5 ? 1 : 0, how];
    }
    if (r < 70) return ['WIDE', 0, null];
    if (r < 80) return ['NO_BALL', randomRuns(), null];
    if (r < 90) return ['LEG_BYE', 1, null];
    return ['NONE', randomRuns(), null];
  }
  const play = m => { const [e, r, d] = randomBall(); recordBall(m, e, r, d); };
  function playUntil(m, done) { for (let g = 0; g < 1000 && m.status === 'LIVE' && !done(m); g++) play(m); }

  // ---------- seed (mirrors DataSeeder.java) ----------
  const now = Date.now();
  const done = createMatch(ind, eng, 'Eden Gardens, Kolkata', now - 864e5);
  startMatch(done); playUntil(done, m => m.status === 'COMPLETED'); done.start = now - 864e5;
  const m1 = createMatch(ind, aus, 'Wankhede Stadium, Mumbai', now - 40 * 6e4);
  startMatch(m1); m1.start = now - 40 * 6e4; playUntil(m1, m => m.innings[m.innings.length - 1].legal >= 52);
  const m2 = createMatch(eng, nz, "Lord's, London", now - 110 * 6e4);
  startMatch(m2); m2.start = now - 110 * 6e4; playUntil(m2, m => m.innings.length === 2 && m.innings[1].legal >= 46);
  createMatch(aus, nz, 'Melbourne Cricket Ground', now + 2 * 36e5);

  // ---------- public API ----------
  let running = true;
  const listeners = [];
  const find = id => { const m = matches.find(x => x.id === id); if (!m) throw new Error('Match not found: ' + id); return m; };
  const emit = m => { const v = view(m); listeners.forEach(fn => fn(v)); return v; };
  setInterval(() => {
    if (!running) return;
    matches.filter(m => m.status === 'LIVE').forEach(m => { play(m); emit(m); });
  }, 2500);
  const order = { LIVE: 0, UPCOMING: 1, COMPLETED: 2 };
  const ok = v => Promise.resolve(JSON.parse(JSON.stringify(v)));
  const attempt = fn => { try { return ok(fn()); } catch (e) { return Promise.reject(e); } };

  return {
    mode: 'demo',
    listMatches: () => ok([...matches].sort((a, b) => order[a.status] - order[b.status] || a.start - b.start).map(view)),
    getMatch: id => attempt(() => view(find(id))),
    getSummary: id => attempt(() => summary(find(id))),
    getLeaderboard: (category, limit = 5) => ok(Object.values(players)
      .sort(category === 'wickets' ? (a, b) => b.wickets - a.wickets || a.runsConceded - b.runsConceded : (a, b) => b.runs - a.runs)
      .slice(0, limit).map(statsView)),
    recordBall: (id, body) => attempt(() => { const m = find(id); recordBall(m, body.extra || 'NONE', body.runs, body.dismissal); return emit(m); }),
    startMatch: id => attempt(() => { const m = find(id); startMatch(m); return emit(m); }),
    getSimulator: () => ok({ running, intervalMs: 2500 }),
    setSimulator: r => { running = r; return ok({ running, intervalMs: 2500 }); },
    subscribe(onMatch, onStatus) { listeners.push(onMatch); setTimeout(() => onStatus('demo'), 0); return () => {}; },
  };
})();
