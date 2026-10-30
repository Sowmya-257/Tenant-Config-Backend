import React, { useState, useEffect } from 'react';
import { Layers, Sliders, CheckCircle2, Building, Clock, ArrowRight, RefreshCw, AlertCircle } from 'lucide-react';
import { api, SHIFTS, OFFICE_NAMES } from '../services/store';

export default function ConfigDashboardPage({ user, onNavigateToEdit }) {
  const offices = api.getOffices(user.tenant);
  const [selectedOffice, setSelectedOffice] = useState(offices[0] || 'BLR-01');
  const [selectedShift, setSelectedShift] = useState('NIGHT');

  const [currentVersion, setCurrentVersion] = useState(1);
  const [settings, setSettings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // When tenant changes, default to primary office
  useEffect(() => {
    setSelectedOffice(offices[0] || '');
  }, [user.tenant]);

  // Fetch real data from Spring Boot backend
  const loadLiveSettings = async () => {
    setLoading(true);
    setError(null);
    try {
      const [ver, resolved] = await Promise.all([
        api.getVersion(user.tenant),
        api.resolveAllSettings(user.tenant, selectedOffice || null, selectedShift || null),
      ]);
      setCurrentVersion(ver);
      setSettings(resolved);
    } catch (err) {
      console.error('Failed to load dashboard from backend', err);
      setError(err.message || 'Could not connect to backend server');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLiveSettings();
  }, [user.tenant, selectedOffice, selectedShift]);

  return (
    <div>
      {/* Scope Filter Bar */}
      <div className="card">
        <div className="card-header">
          <div>
            <h2 className="card-title">
              <Layers size={18} color="#3b82f6" />
              Configuration Dashboard ({user.tenant})
            </h2>
            <p className="card-subtitle">
              Hierarchy resolution rule: <code>SHIFT &rarr; OFFICE &rarr; CLIENT &rarr; GLOBAL</code> (Server-side resolved)
            </p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span className="badge badge-green">Active Version: v{currentVersion}</span>
            <button
              className="btn btn-secondary btn-sm"
              onClick={loadLiveSettings}
              title="Refresh live data from backend"
            >
              <RefreshCw size={13} className={loading ? 'spin' : ''} />
              <span>Refresh</span>
            </button>
            {user.role !== 'ROLE_EMPLOYEE' && (
              <button
                className="btn btn-primary btn-sm"
                onClick={() => onNavigateToEdit()}
              >
                <Sliders size={14} />
                <span>Edit Settings</span>
              </button>
            )}
          </div>
        </div>

        {/* Office & Shift Evaluator Filters */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', background: 'var(--bg)', padding: '12px 16px', borderRadius: 'var(--radius)' }}>
          <div>
            <label className="form-label">Evaluate For Office Campus:</label>
            <select
              className="select"
              value={selectedOffice}
              onChange={(e) => setSelectedOffice(e.target.value)}
            >
              <option value="">None (Tenant Baseline)</option>
              {offices.map((o) => (
                <option key={o} value={o}>
                  {o} — {OFFICE_NAMES[o] || 'Campus'}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="form-label">Evaluate For Shift:</label>
            <select
              className="select"
              value={selectedShift}
              onChange={(e) => setSelectedShift(e.target.value)}
            >
              <option value="">None (Daytime General Default)</option>
              {SHIFTS.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {error && (
        <div
          style={{
            background: 'rgba(239, 68, 68, 0.1)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            borderRadius: 'var(--radius)',
            padding: '12px 16px',
            color: '#f87171',
            fontSize: '0.85rem',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            marginBottom: '16px',
          }}
        >
          <AlertCircle size={18} />
          <span>Error loading settings from backend: {error}</span>
        </div>
      )}

      {/* Settings Grid / Table */}
      <div className="card">
        <div className="card-header">
          <h3 className="card-title">Resolved Operational Settings for {user.tenant}</h3>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            Live effective values computed by Spring Boot <code>HierarchyResolutionService</code>
          </span>
        </div>

        {loading ? (
          <div style={{ padding: '36px', textAlign: 'center', color: 'var(--text-muted)' }}>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
              <RefreshCw size={16} className="spin" />
              <span>Fetching live settings from backend server...</span>
            </div>
          </div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Setting Key</th>
                  <th>Type</th>
                  <th>Platform Default</th>
                  <th>Resolved Effective Value</th>
                  <th>Winning Level</th>
                  <th>Author</th>
                </tr>
              </thead>
              <tbody>
                {settings.map((item) => {
                  let badgeColor = 'badge-gray';
                  if (item.level === 'SHIFT') badgeColor = 'badge-purple';
                  if (item.level === 'OFFICE') badgeColor = 'badge-blue';
                  if (item.level === 'CLIENT') badgeColor = 'badge-green';

                  return (
                    <tr key={item.key}>
                      <td>
                        <code style={{ fontWeight: 600, color: '#93c5fd' }}>{item.key}</code>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          {item.description}
                        </div>
                      </td>
                      <td>
                        <span className="badge badge-gray">{item.type}</span>
                      </td>
                      <td style={{ color: 'var(--text-muted)', fontFamily: 'var(--mono)' }}>
                        {item.defaultValue}
                      </td>
                      <td>
                        <span
                          style={{
                            fontFamily: 'var(--mono)',
                            fontWeight: 700,
                            fontSize: '0.95rem',
                            color: '#34d399',
                          }}
                        >
                          {item.value}
                        </span>
                      </td>
                      <td>
                        <span className={`badge ${badgeColor}`}>
                          {item.level}
                        </span>
                        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginLeft: '6px' }}>
                          ({item.scope})
                        </span>
                      </td>
                      <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                        {item.author}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Hierarchy Info Box */}
      <div
        style={{
          background: 'rgba(37, 99, 235, 0.08)',
          border: '1px solid rgba(37, 99, 235, 0.2)',
          borderRadius: 'var(--radius)',
          padding: '12px 16px',
          fontSize: '0.8rem',
          color: 'var(--text-muted)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <span>
          <strong>Precedence Rule:</strong> {user.tenant} specific policies override platform defaults. Shift rules (e.g. night escort) override campus and client levels.
        </span>
        <button
          className="btn btn-secondary btn-sm"
          onClick={() => onNavigateToEdit('booking.cutoff.mins')}
        >
          Change Booking Cutoff &rarr;
        </button>
      </div>
    </div>
  );
}
