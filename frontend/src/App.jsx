import { useState, useEffect } from 'react'

function MoviesList() {
  const [movies, setMovies] = useState([])
  const [form, setForm] = useState({ title: '', director: '', releaseYear: '', genre: '', runtimeMinutes: '', posterUrl: '' })
  const [msg, setMsg] = useState('')

  useEffect(() => { fetchMovies() }, [])

  function fetchMovies() {
    fetch('/api/movies')
      .then(r => r.json())
      .then(setMovies)
      .catch(() => setMsg('Failed to load movies'))
  }

  function handleSubmit(e) {
    e.preventDefault()
    fetch('/api/movies', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...form, releaseYear: Number(form.releaseYear), runtimeMinutes: Number(form.runtimeMinutes) })
    })
      .then(r => { if (!r.ok) throw new Error(); return r.json() })
      .then(() => {
        setMsg('Movie added!')
        setForm({ title: '', director: '', releaseYear: '', genre: '', runtimeMinutes: '', posterUrl: '' })
        fetchMovies()
      })
      .catch(() => setMsg('Failed to add movie'))
  }

  return (
    <div>
      <h2>Movies</h2>
      {msg && <p className="msg">{msg}</p>}
      <table>
        <thead><tr><th>ID</th><th>Title</th><th>Director</th><th>Year</th><th>Genre</th><th>Runtime</th></tr></thead>
        <tbody>
          {movies.map(m => (
            <tr key={m.id}>
              <td>{m.id}</td><td>{m.title}</td><td>{m.director}</td>
              <td>{m.releaseYear}</td><td>{m.genre}</td><td>{m.runtimeMinutes} min</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h3>Add Movie</h3>
      <form onSubmit={handleSubmit}>
        <input required placeholder="Title" value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} />
        <input required placeholder="Director" value={form.director} onChange={e => setForm({ ...form, director: e.target.value })} />
        <input required type="number" placeholder="Year" value={form.releaseYear} onChange={e => setForm({ ...form, releaseYear: e.target.value })} />
        <input placeholder="Genre" value={form.genre} onChange={e => setForm({ ...form, genre: e.target.value })} />
        <input type="number" placeholder="Runtime (min)" value={form.runtimeMinutes} onChange={e => setForm({ ...form, runtimeMinutes: e.target.value })} />
        <input placeholder="Poster URL (optional)" value={form.posterUrl} onChange={e => setForm({ ...form, posterUrl: e.target.value })} />
        <button type="submit">Add Movie</button>
      </form>
    </div>
  )
}

function AddTracking() {
  const [movies, setMovies] = useState([])
  const [form, setForm] = useState({ movieId: '', userId: '', status: 'TO_WATCH', rating: '', notes: '' })
  const [msg, setMsg] = useState('')

  useEffect(() => {
    fetch('/api/movies').then(r => r.json()).then(data => {
      setMovies(data)
      if (data.length > 0) setForm(f => ({ ...f, movieId: data[0].id }))
    })
  }, [])

  function handleSubmit(e) {
    e.preventDefault()
    const body = { movieId: Number(form.movieId), userId: form.userId, status: form.status }
    if (form.rating) body.rating = Number(form.rating)
    if (form.notes) body.notes = form.notes
    fetch('/api/tracking', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })
      .then(r => { if (!r.ok) throw new Error(); return r.json() })
      .then(() => {
        setMsg('Tracking entry added!')
        setForm({ movieId: movies[0]?.id || '', userId: '', status: 'TO_WATCH', rating: '', notes: '' })
      })
      .catch(() => setMsg('Failed to add tracking entry'))
  }

  return (
    <div>
      <h2>Add Tracking Entry</h2>
      {msg && <p className="msg">{msg}</p>}
      <form onSubmit={handleSubmit}>
        <label>Movie
          <select value={form.movieId} onChange={e => setForm({ ...form, movieId: e.target.value })}>
            {movies.map(m => <option key={m.id} value={m.id}>{m.title}</option>)}
          </select>
        </label>
        <input required placeholder="User ID" value={form.userId} onChange={e => setForm({ ...form, userId: e.target.value })} />
        <label>Status
          <select value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}>
            <option>TO_WATCH</option>
            <option>WATCHING</option>
            <option>WATCHED</option>
          </select>
        </label>
        <input type="number" min="1" max="10" placeholder="Rating (1-10, optional)" value={form.rating} onChange={e => setForm({ ...form, rating: e.target.value })} />
        <input placeholder="Notes (optional)" value={form.notes} onChange={e => setForm({ ...form, notes: e.target.value })} />
        <button type="submit">Add Entry</button>
      </form>
    </div>
  )
}

function Watchlist() {
  const [entries, setEntries] = useState([])
  const [err, setErr] = useState('')

  useEffect(() => {
    fetch('/api/tracking/full')
      .then(r => r.json())
      .then(setEntries)
      .catch(() => setErr('Failed to load watchlist'))
  }, [])

  return (
    <div>
      <h2>Watchlist</h2>
      {err && <p className="msg">{err}</p>}
      <table>
        <thead><tr><th>Title</th><th>Director</th><th>Status</th><th>Rating</th><th>Notes</th></tr></thead>
        <tbody>
          {entries.map(e => (
            <tr key={e.id}>
              <td>{e.movie?.title}</td>
              <td>{e.movie?.director}</td>
              <td><span className={`status ${e.status?.toLowerCase()}`}>{e.status}</span></td>
              <td>{e.rating ?? '—'}</td>
              <td>{e.notes ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default function App() {
  const [view, setView] = useState('movies')

  return (
    <div className="app">
      <header>
        <h1>MovieTrack</h1>
        <nav>
          <button className={view === 'movies' ? 'active' : ''} onClick={() => setView('movies')}>Movies</button>
          <button className={view === 'track' ? 'active' : ''} onClick={() => setView('track')}>Track</button>
          <button className={view === 'watchlist' ? 'active' : ''} onClick={() => setView('watchlist')}>Watchlist</button>
        </nav>
      </header>
      <main>
        {view === 'movies' && <MoviesList />}
        {view === 'track' && <AddTracking />}
        {view === 'watchlist' && <Watchlist />}
      </main>
    </div>
  )
}
