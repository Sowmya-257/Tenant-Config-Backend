import React, { useState } from 'react';
import {
  Layers,
  Sliders,
  History,
  LogOut,
  User,
  Shield,
  X,
  CheckCircle2,
  AlertCircle,
  Info,
} from 'lucide-react';
import LoginPage from './components/LoginPage';
import ConfigDashboardPage from './components/ConfigDashboardPage';
import EditConfigPage from './components/EditConfigPage';
import HistoryPage from './components/HistoryPage';
import { TENANTS } from './services/store';

export default function App() {
  // Authentication state
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('mis_user');
    return saved ? JSON.parse(saved) : null;
  });

  // Navigation tab state: 'dashboard' | 'edit' | 'history'
  const [tab, setTab] = useState('dashboard');
  const [editSettingKey, setEditSettingKey] = useState('booking.cutoff.mins');
  const [toasts, setToasts] = useState([]);

  const showToast = (message, type = 'info') => {
    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4000);
  };

  const removeToast = (id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  const handleLogin = (userData) => {
    setUser(userData);
    localStorage.setItem('mis_user', JSON.stringify(userData));
    setTab('dashboard');
    showToast(`Logged in as ${userData.username} (${userData.role})`, 'success');
  };

  const handleLogout = () => {
    setUser(null);
    localStorage.removeItem('mis_user');
    setTab('dashboard');
  };

  const handleSwitchTenant = (tenantId) => {
    if (!user) return;
    const updated = { ...user, tenant: tenantId };
    setUser(updated);
    localStorage.setItem('mis_user', JSON.stringify(updated));
    showToast(`Switched context to ${tenantId}`, 'info');
  };

  const handleNavigateToEdit = (key = 'booking.cutoff.mins') => {
    setEditSettingKey(key);
    setTab('edit');
  };

  // If not logged in, render LoginPage
  if (!user) {
    return (
      <>
        <LoginPage onLogin={handleLogin} />
        {/* Toast alerts */}
        <div className="toast-box">
          {toasts.map((t) => (
            <div key={t.id} className={`toast toast-${t.type}`}>
              {t.type === 'success' && <CheckCircle2 size={16} />}
              {t.type === 'error' && <AlertCircle size={16} />}
              {t.type === 'info' && <Info size={16} />}
              <span>{t.message}</span>
              <button
                onClick={() => removeToast(t.id)}
                style={{ background: 'transparent', border: 'none', color: 'inherit', cursor: 'pointer', marginLeft: 8 }}
              >
                <X size={14} />
              </button>
            </div>
          ))}
        </div>
      </>
    );
  }

  const roleLabel =
    user.role === 'ROLE_ADMIN'
      ? 'Admin'
      : user.role === 'ROLE_OPERATOR'
      ? 'Operator'
      : 'Employee';

  const roleBadge =
    user.role === 'ROLE_ADMIN'
      ? 'badge-purple'
      : user.role === 'ROLE_OPERATOR'
      ? 'badge-blue'
      : 'badge-yellow';

  return (
    <div>
      {/* Top Navigation Bar */}
      <header className="nav">
        <div className="brand">
          <div className="brand-dot" />
          <span>MoveInSync</span>
          
          {/* Quick Tenant Switcher */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px', marginLeft: '6px' }}>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>/</span>
            <select
              className="select"
              value={user.tenant}
              onChange={(e) => handleSwitchTenant(e.target.value)}
              style={{
                padding: '3px 8px',
                fontSize: '0.8rem',
                fontWeight: 600,
                color: '#60a5fa',
                background: 'var(--panel-hover)',
                borderColor: 'var(--border)',
                width: 'auto',
                cursor: 'pointer',
              }}
              title="Switch company to view its custom configurations"
            >
              {TENANTS.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        <nav className="nav-links">
          <button
            className={`nav-btn ${tab === 'dashboard' ? 'active' : ''}`}
            onClick={() => setTab('dashboard')}
          >
            <Layers size={15} />
            <span>Dashboard</span>
          </button>

          <button
            className={`nav-btn ${tab === 'edit' ? 'active' : ''}`}
            onClick={() => handleNavigateToEdit('booking.cutoff.mins')}
          >
            <Sliders size={15} />
            <span>Edit Config</span>
          </button>

          <button
            className={`nav-btn ${tab === 'history' ? 'active' : ''}`}
            onClick={() => setTab('history')}
          >
            <History size={15} />
            <span>History</span>
          </button>
        </nav>

        {/* User Info & Logout */}
        <div className="user-badge">
          <span className={`badge ${roleBadge}`}>{roleLabel}</span>
          <span style={{ color: 'var(--text)', fontWeight: 500 }}>{user.username}</span>
          <button
            className="btn btn-secondary btn-sm"
            onClick={handleLogout}
            title="Sign out"
            style={{ padding: '4px 8px', fontSize: '0.75rem' }}
          >
            <LogOut size={13} />
            <span>Logout</span>
          </button>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="container">
        {tab === 'dashboard' && (
          <ConfigDashboardPage
            user={user}
            onNavigateToEdit={handleNavigateToEdit}
          />
        )}

        {tab === 'edit' && (
          <EditConfigPage
            user={user}
            preselectedKey={editSettingKey}
            onSaved={() => setTab('history')}
            onCancel={() => setTab('dashboard')}
            showToast={showToast}
          />
        )}

        {tab === 'history' && (
          <HistoryPage
            user={user}
            onRollbackComplete={() => setTab('dashboard')}
            showToast={showToast}
          />
        )}
      </main>

      {/* Toast Alerts */}
      <div className="toast-box">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast-${t.type}`}>
            {t.type === 'success' && <CheckCircle2 size={16} />}
            {t.type === 'error' && <AlertCircle size={16} />}
            {t.type === 'info' && <Info size={16} />}
            <span>{t.message}</span>
            <button
              onClick={() => removeToast(t.id)}
              style={{ background: 'transparent', border: 'none', color: 'inherit', cursor: 'pointer', marginLeft: 8 }}
            >
              <X size={14} />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
