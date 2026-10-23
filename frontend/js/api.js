// REST + Server-Sent Events client for the Spring Boot backend.
window.CricketAPI = (() => {
  const base = '/api';

  async function req(path, options = {}) {
    const res = await fetch(base + path, { headers: { 'Content-Type': 'application/json' }, ...options });
    if (!res.ok) {
      let message = res.statusText;
      try {
        const body = await res.json();
        message = body.detail || body.title || message;
      } catch (_) { /* not JSON */ }
      throw new Error(message);
    }
    return res.json();
  }

  return {
    mode: 'live',
    listMatches: () => req('/matches'),
    getMatch: id => req(`/matches/${id}`),
    getSummary: id => req(`/matches/${id}/summary`),
    getLeaderboard: (category, limit = 5) => req(`/players/leaderboard?category=${category}&limit=${limit}`),
    recordBall: (id, body) => req(`/matches/${id}/balls`, { method: 'POST', body: JSON.stringify(body) }),
    startMatch: id => req(`/matches/${id}/start`, { method: 'POST' }),
    getSimulator: () => req('/simulator'),
    setSimulator: running => req(`/simulator/${running ? 'resume' : 'pause'}`, { method: 'POST' }),
    subscribe(onMatch, onStatus) {
      const source = new EventSource(base + '/stream');
      source.addEventListener('match', e => onMatch(JSON.parse(e.data)));
      source.onopen = () => onStatus('connected');
      source.onerror = () => onStatus('reconnecting');
      return () => source.close();
    },
  };
})();
