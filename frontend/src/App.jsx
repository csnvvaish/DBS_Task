import { createContext, useContext, useEffect, useState } from 'react';
import { Link, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import { api } from './api.js';

const roleRoutes = {
  ROLE_USER: '/user',
  ROLE_MANAGER: '/manager',
  ROLE_ADMIN: '/admin',
};

const AuthContext = createContext(null);
const useAuth = () => useContext(AuthContext);

function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    api.me()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setReady(true));
  }, []);

  const value = {
    user,
    ready,
    async login(values) {
      const loggedIn = await api.login(values);
      setUser(loggedIn);
      return loggedIn;
    },
    async logout() {
      await api.logout();
      setUser(null);
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

function AuthShell({ children, eyebrow, title, detail }) {
  return (
    <main className="auth-layout">
      <aside className="auth-aside">
        <Link className="wordmark" to="/login"><span className="brand-mark">N</span> Northstar</Link>
        <div className="aside-copy">
          <p className="eyebrow">ACCESS CONTROL / 01</p>
          <h1>One account.<br />The right access.</h1>
          <p>Identity and permissions, grounded in your organization's directory.</p>
        </div>
        <div className="aside-footer"><span className="status-dot" /> Oracle-backed identity</div>
      </aside>
      <section className="auth-main">
        <div className="auth-form-wrap">
          <p className="eyebrow form-eyebrow">{eyebrow}</p>
          <h2>{title}</h2>
          <p className="form-detail">{detail}</p>
          {children}
        </div>
      </section>
    </main>
  );
}

function PasswordField({ label, name, autoComplete, required, minLength, maxLength }) {
  const [visible, setVisible] = useState(false);

  return (
    <label>{label}<span className="password-control">
      <input
        name={name}
        type={visible ? 'text' : 'password'}
        autoComplete={autoComplete}
        required={required}
        minLength={minLength}
        maxLength={maxLength}
      />
      <button
        className="password-toggle"
        type="button"
        aria-label={visible ? 'Hide password' : 'Show password'}
        aria-pressed={visible}
        title={visible ? 'Hide password' : 'Show password'}
        onClick={() => setVisible((current) => !current)}
      >
        <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
          {visible ? (
            <>
              <path d="M3 3l18 18M10.6 10.7a2 2 0 002.7 2.7" />
              <path d="M9.9 5.2A10.8 10.8 0 0112 5c5 0 8.5 4.3 9.5 7-.4 1.1-1.2 2.3-2.3 3.4M6.2 6.2C3.9 7.7 2.8 9.9 2.5 12c.5 1.5 2.2 4 5.2 5.4 1.3.6 2.7.9 4.3.9 1 0 1.9-.1 2.8-.4" />
            </>
          ) : (
            <>
              <path d="M2.5 12S6 5 12 5s9.5 7 9.5 7-3.5 7-9.5 7-9.5-7-9.5-7z" />
              <circle cx="12" cy="12" r="3" />
            </>
          )}
        </svg>
      </button>
    </span></label>
  );
}

function LoginPage() {
  const { user, login } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to={roleRoutes[user.role] || '/login'} replace />;

  async function submit(event) {
    event.preventDefault();
    setError('');
    setBusy(true);
    const values = Object.fromEntries(new FormData(event.currentTarget));
    values.username = values.username.trim();
    try {
      const loggedIn = await login(values);
      navigate(roleRoutes[loggedIn.role] || '/login', { replace: true });
    } catch (exception) {
      setError(exception.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthShell eyebrow="WELCOME BACK" title="Sign in" detail="Use your registered account to continue.">
      {location.state?.notice && <p className="notice" role="status">{location.state.notice}</p>}
      <form className="form-stack" onSubmit={submit}>
        <label>Username<input name="username" autoComplete="username" required maxLength="50" /></label>
        <PasswordField label="Password" name="password" autoComplete="current-password" required />
        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="primary-button" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'} <span aria-hidden="true">↗</span></button>
      </form>
      <p className="form-switch">New here? <Link to="/register">Create an account</Link></p>
    </AuthShell>
  );
}

function RegisterPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to={roleRoutes[user.role] || '/login'} replace />;

  async function submit(event) {
    event.preventDefault();
    setError('');
    setBusy(true);
    const values = Object.fromEntries(new FormData(event.currentTarget));
    try {
      await api.register(values);
      navigate('/login', { replace: true, state: { notice: 'Account created. Sign in to continue.' } });
    } catch (exception) {
      setError(exception.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <AuthShell eyebrow="NEW ACCOUNT" title="Create your account" detail="Your account starts with standard user access.">
      <form className="form-stack" onSubmit={submit}>
        <label>Username<input name="username" autoComplete="username" required minLength="3" maxLength="50" pattern="[A-Za-z0-9._]+-?" /></label>
        <label>Email<input name="email" type="email" autoComplete="email" required maxLength="255" /></label>
        <PasswordField label="Password" name="password" autoComplete="new-password" required minLength="12" maxLength="72" />
        {error && <p className="form-error" role="alert">{error}</p>}
        <button className="primary-button" disabled={busy}>{busy ? 'Creating account…' : 'Create account'} <span aria-hidden="true">↗</span></button>
      </form>
      <p className="form-switch">Already registered? <Link to="/login">Sign in</Link></p>
    </AuthShell>
  );
}

function ProtectedRoute({ role, children }) {
  const { user, ready } = useAuth();
  if (!ready) return <main className="loading-screen"><span className="loader" />Checking session</main>;
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== role) return <Navigate to={roleRoutes[user.role] || '/login'} replace />;
  return children;
}

const pageInfo = {
  ROLE_USER: { title: 'Your workspace', label: 'MEMBER SPACE', summary: 'Your account and personal workspace are ready.', tone: 'user', sections: ['Personal overview', 'Your account', 'Recent activity'] },
  ROLE_MANAGER: { title: 'Team operations', label: 'MANAGEMENT', summary: "A clear view of your team's operational workspace.", tone: 'manager', sections: ['Team overview', 'People and access', 'Operational updates'] },
  ROLE_ADMIN: { title: 'Access control', label: 'ADMINISTRATION', summary: 'Manage directory access and role assignments.', tone: 'admin', sections: ['Directory', 'Role assignments', 'Security controls'] },
};

function DashboardPage({ role }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [dashboard, setDashboard] = useState(null);
  const [users, setUsers] = useState([]);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);
  const info = pageInfo[role];

  useEffect(() => {
    api.dashboard(role)
      .then(setDashboard)
      .catch((exception) => setError(exception.message));
    if (role === 'ROLE_ADMIN') {
      api.users().then(setUsers).catch((exception) => setError(exception.message));
    }
  }, [role]);

  async function changeRole(id, nextRole) {
    setBusyId(id);
    setError('');
    try {
      const updated = await api.updateRole(id, nextRole);
      setUsers((current) => current.map((item) => item.id === id ? { ...item, role: updated.role } : item));
    } catch (exception) {
      setError(exception.message);
    } finally {
      setBusyId(null);
    }
  }

  async function signOut() {
    try {
      await logout();
      navigate('/login', { replace: true });
    } catch (exception) {
      setError(exception.message);
    }
  }

  return (
    <main className={`dashboard-shell ${info.tone}`}>
      <header className="topbar">
        <Link className="wordmark dark-wordmark" to={roleRoutes[role]}><span className="brand-mark">N</span> Northstar</Link>
        <div className="topbar-right"><span className="role-chip">{user.role}</span><span className="topbar-user">{user.username}</span><button className="signout-button" onClick={signOut} aria-label="Logout" title="Logout"><span aria-hidden="true">↗</span>Logout</button></div>
      </header>
      <div className="dashboard-content">
        <div className="dashboard-heading">
          <div><p className="eyebrow">{info.label}</p><h1>{info.title}</h1><p className="dashboard-summary">{dashboard?.message || info.summary}</p></div>
          <div className="identity-stamp"><span className="stamp-label">SIGNED IN AS</span><strong>{user.username}</strong><span>{user.role}</span></div>
        </div>

        {error && <p className="dashboard-error" role="alert">{error}</p>}

        {role === 'ROLE_ADMIN' ? (
          <section className="directory-section">
            <div className="section-heading"><div><p className="eyebrow">DIRECTORY</p><h2>People and roles</h2></div><span className="count-label">{users.length} accounts</span></div>
            <div className="user-table-wrap">
              <table className="user-table">
                <thead><tr><th>Account</th><th>Email</th><th>Role</th><th>Created</th></tr></thead>
                <tbody>{users.map((item) => (
                  <tr key={item.id}>
                    <td><strong>{item.username}</strong></td>
                    <td>{item.email}</td>
                    <td><select aria-label={`Role for ${item.username}`} value={item.role} disabled={busyId === item.id} onChange={(event) => changeRole(item.id, event.target.value)}>
                      <option value="ROLE_USER">ROLE_USER</option><option value="ROLE_MANAGER">ROLE_MANAGER</option><option value="ROLE_ADMIN">ROLE_ADMIN</option>
                    </select></td>
                    <td>{item.createdAt ? new Date(item.createdAt).toLocaleDateString() : '—'}</td>
                  </tr>
                ))}</tbody>
              </table>
              {users.length === 0 && <p className="empty-state">No accounts in the directory.</p>}
            </div>
          </section>
        ) : (
          <section className="workspace-sections" aria-label={`${info.title} sections`}>
            {info.sections.map((section, index) => <article className={`workspace-row row-${index + 1}`} key={section}>
              <span className="row-number">0{index + 1}</span><h2>{section}</h2><span className="row-mark" aria-hidden="true">↗</span>
            </article>)}
          </section>
        )}
        <footer className="dashboard-footer"><span>Authenticated via Oracle directory</span><span>Session protected</span></footer>
      </div>
    </main>
  );
}

function AppRoutes() {
  const { user } = useAuth();
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/user" element={<ProtectedRoute role="ROLE_USER"><DashboardPage role="ROLE_USER" /></ProtectedRoute>} />
      <Route path="/manager" element={<ProtectedRoute role="ROLE_MANAGER"><DashboardPage role="ROLE_MANAGER" /></ProtectedRoute>} />
      <Route path="/admin" element={<ProtectedRoute role="ROLE_ADMIN"><DashboardPage role="ROLE_ADMIN" /></ProtectedRoute>} />
      <Route path="*" element={<Navigate to={user ? (roleRoutes[user.role] || '/login') : '/login'} replace />} />
    </Routes>
  );
}

export default function App() {
  return <AuthProvider><AppRoutes /></AuthProvider>;
}