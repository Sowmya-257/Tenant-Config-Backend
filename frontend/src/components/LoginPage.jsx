import React, { useState } from 'react';
import { Shield, LogIn } from 'lucide-react';
import { TENANTS } from '../services/store';

export default function LoginPage({ onLogin }) {
  const [tenant, setTenant] = useState('MICROSOFT');
  const [username, setUsername] = useState('bob_ops');
  const [role, setRole] = useState('ROLE_OPERATOR');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!username.trim()) return;
    onLogin({
      username: username.trim(),
      tenant,
      role,
    });
  };

  const handleQuickLogin = (user, tRole) => {
    onLogin({
      username: user,
      tenant,
      role: tRole,
    });
  };

  return (
    <div
      style={{
        minHeight: '85vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '20px',
      }}
    >
      <div
        className="card"
        style={{
          maxWidth: 440,
          width: '100%',
          padding: '32px 28px',
          boxShadow: '0 8px 32px rgba(0,0,0,0.4)',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <div
            style={{
              width: 44,
              height: 44,
              borderRadius: 10,
              background: 'var(--primary)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '12px',
            }}
          >
            <Shield size={22} color="#fff" />
          </div>
          <h2 style={{ fontSize: '1.3rem', fontWeight: 700, marginBottom: '4px' }}>
            MoveInSync Configuration
          </h2>
          <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
            Sign in to manage client settings &amp; policies
          </p>
        </div>

        {/* Quick Persona Selectors */}
        <div style={{ marginBottom: '20px' }}>
          <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '8px' }}>
            QUICK LOGIN:
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => handleQuickLogin('alice_admin', 'ROLE_ADMIN')}
              style={{ fontSize: '0.75rem' }}
            >
              Admin
            </button>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => handleQuickLogin('bob_ops', 'ROLE_OPERATOR')}
              style={{ fontSize: '0.75rem', borderColor: 'var(--primary)', color: '#60a5fa' }}
            >
              Operator
            </button>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => handleQuickLogin('charlie_emp', 'ROLE_EMPLOYEE')}
              style={{ fontSize: '0.75rem' }}
            >
              Employee
            </button>
          </div>
        </div>

        <div style={{ position: 'relative', textAlign: 'center', margin: '20px 0' }}>
          <hr style={{ border: 'none', borderTop: '1px solid var(--border)' }} />
          <span
            style={{
              position: 'absolute',
              top: '-10px',
              left: '50%',
              transform: 'translateX(-50%)',
              background: 'var(--panel)',
              padding: '0 8px',
              fontSize: '0.7rem',
              color: 'var(--text-muted)',
            }}
          >
            OR CUSTOM LOGIN
          </span>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Client / Tenant</label>
            <select
              className="select"
              value={tenant}
              onChange={(e) => setTenant(e.target.value)}
            >
              {TENANTS.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name} ({t.id})
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label className="form-label">Username</label>
            <input
              type="text"
              className="input"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Username"
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Role</label>
            <select
              className="select"
              value={role}
              onChange={(e) => setRole(e.target.value)}
            >
              <option value="ROLE_ADMIN">Admin (Full Control)</option>
              <option value="ROLE_OPERATOR">Operator (Edit Configuration)</option>
              <option value="ROLE_EMPLOYEE">Employee (Read-Only)</option>
            </select>
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '8px' }}
          >
            <LogIn size={16} />
            <span>Sign In</span>
          </button>
        </form>
      </div>
    </div>
  );
}
