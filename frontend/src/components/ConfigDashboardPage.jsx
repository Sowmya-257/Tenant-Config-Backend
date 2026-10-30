import React, { useState, useEffect } from 'react';
import {
  Layers,
  Sliders,
  CheckCircle2,
  Building,
  Clock,
  ArrowRight,
  RefreshCw,
  AlertCircle,
  HelpCircle,
  ShieldCheck,
  Radio,
  Zap,
  Check,
  X,
  Play,
  Share2,
} from 'lucide-react';
import { api, SHIFTS, OFFICE_NAMES } from '../services/store';

export default function ConfigDashboardPage({ user, onNavigateToEdit, showToast, sseConnected, lastPropagationMs }) {
  const offices = api.getOffices(user.tenant);
  const [selectedOffice, setSelectedOffice] = useState(offices[0] || 'BLR-01');
  const [selectedShift, setSelectedShift] = useState('NIGHT');

  const [currentVersion, setCurrentVersion] = useState(1);
  const [settings, setSettings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Explain mode state
  const [explainItem, setExplainItem] = useState(null);
  const [explainData, setExplainData] = useState(null);
  const [explainLoading, setExplainLoading] = useState(false);

  // Active rollouts (canary) state
  const [activeRollouts, setActiveRollouts] = useState([]);
  const [rolloutActingId, setRolloutActingId] = useState(null);

  // When tenant changes, default to primary office
  useEffect(() => {
    setSelectedOffice(offices[0] || '');
  }, [user.tenant]);

  // Fetch real data from Spring Boot backend
  const loadLiveSettings = async () => {
    setLoading(true);
    setError(null);
    try {
      const [ver, resolved, rollouts] = await Promise.all([
        api.getVersion(user.tenant),
        api.resolveAllSettings(user.tenant, selectedOffice || null, selectedShift || null),
        api.listRollouts(user.tenant),
      ]);
      setCurrentVersion(ver);
      setSettings(resolved);
      setActiveRollouts(rollouts || []);
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

  // Handle Explain Mode lookup
  const handleOpenExplain = async (item) => {
    setExplainItem(item);
    setExplainData(null);
    setExplainLoading(true);
    try {
      const data = await api.explainSetting(user.tenant, item.key, selectedOffice || null, selectedShift || null);
      setExplainData(data);
    } catch (err) {
      showToast('Failed to load explain diagnostics: ' + err.message, 'error');
    } finally {
      setExplainLoading(false);
    }
  };

  // Promote active canary rollout to all offices
  const handlePromoteRollout = async (rolloutId) => {
    if (user.role === 'ROLE_EMPLOYEE') {
      showToast('Employees cannot promote rollouts.', 'error');
      return;
    }
    setRolloutActingId(rolloutId);
    try {
      const res = await api.promoteRollout(rolloutId, user.username, 'Promoted via dashboard');
      showToast(`Canary rollout promoted to ALL offices at version v${res.promotedVersion}!`, 'success');
      await loadLiveSettings();
    } catch (err) {
      showToast('Promotion failed: ' + err.message, 'error');
    } finally {
      setRolloutActingId(null);
    }
  };

  // Abort active canary rollout
  const handleAbortRollout = async (rolloutId) => {
    if (user.role === 'ROLE_EMPLOYEE') {
      showToast('Employees cannot abort rollouts.', 'error');
      return;
    }
    setRolloutActingId(rolloutId);
    try {
      await api.abortRollout(rolloutId, user.username, 'Aborted via dashboard');
      showToast('Canary rollout aborted. Pilot offices reverted to baseline.', 'info');
      await loadLiveSettings();
    } catch (err) {
      showToast('Abort failed: ' + err.message, 'error');
    } finally {
      setRolloutActingId(null);
    }
  };

  return (
    <div>
      {/* Live Push Streaming & Propagation Banner */}
      <div
        style={{
          background: sseConnected ? 'rgba(16, 185, 129, 0.08)' : 'rgba(239, 68, 68, 0.08)',
          border: `1px solid ${sseConnected ? 'rgba(16, 185, 129, 0.25)' : 'rgba(239, 68, 68, 0.25)'}`,
          borderRadius: 'var(--radius)',
          padding: '10px 16px',
          marginBottom: '16px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          fontSize: '0.825rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div
            style={{
              width: 8,
              height: 8,
              borderRadius: '50%',
              background: sseConnected ? '#34d399' : '#f87171',
              boxShadow: sseConnected ? '0 0 8px #34d399' : 'none',
            }}
          />
          <span>
            <strong>Push-Based Live Propagation:</strong>{' '}
            {sseConnected ? (
              <span style={{ color: '#34d399' }}>Connected via SSE Stream (/api/v1/stream/{user.tenant})</span>
            ) : (
              <span style={{ color: '#f87171' }}>Reconnecting with Jitter...</span>
            )}
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <span style={{ color: 'var(--text-muted)' }}>
            Propagation SLA: <strong style={{ color: '#60a5fa' }}>{lastPropagationMs || '< 50'} ms</strong> (Target: &lt; 5,000 ms guarantee)
          </span>
        </div>
      </div>

      {/* Active Canary Staged Rollout Banner (if active) */}
      {activeRollouts.map((rollout) => (
        <div
          key={rollout.id}
          style={{
            background: 'rgba(245, 158, 11, 0.1)',
            border: '1px solid rgba(245, 158, 11, 0.35)',
            borderRadius: 'var(--radius)',
            padding: '14px 18px',
            marginBottom: '16px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '16px',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
              <span className="badge badge-yellow" style={{ fontSize: '0.7rem' }}>
                CANARY PILOT ACTIVE
              </span>
              <strong style={{ fontSize: '0.9rem', color: '#fbbf24' }}>
                Staged Rollout #{rollout.id}: {rollout.settingKey} &rarr; {rollout.canaryValue}
              </strong>
            </div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              Target Pilot Offices: <strong style={{ color: '#93c5fd' }}>{rollout.targetOfficesCsv}</strong> (all other offices remain on client baseline). Reason: <em>{rollout.reason}</em>
            </div>
          </div>

          {user.role !== 'ROLE_EMPLOYEE' && (
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                className="btn btn-primary btn-sm"
                onClick={() => handlePromoteRollout(rollout.id)}
                disabled={rolloutActingId === rollout.id}
                style={{ background: '#059669', borderColor: '#059669' }}
              >
                <Share2 size={13} />
                <span>{rolloutActingId === rollout.id ? 'Promoting...' : 'Promote to All Offices'}</span>
              </button>
              <button
                className="btn btn-danger btn-sm"
                onClick={() => handleAbortRollout(rollout.id)}
                disabled={rolloutActingId === rollout.id}
              >
                <X size={13} />
                <span>Abort</span>
              </button>
            </div>
          )}
        </div>
      ))}

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
                  <th>Setting Key &amp; Validation Schema</th>
                  <th>Type</th>
                  <th>Platform Default</th>
                  <th>Resolved Effective Value</th>
                  <th>Winning Level</th>
                  <th>Author</th>
                  <th style={{ textAlign: 'center', width: '100px' }}>Diagnostics</th>
                </tr>
              </thead>
              <tbody>
                {settings.map((item) => {
                  let badgeColor = 'badge-gray';
                  if (item.level === 'SHIFT') badgeColor = 'badge-purple';
                  if (item.level === 'OFFICE') badgeColor = 'badge-blue';
                  if (item.level === 'CLIENT') badgeColor = 'badge-green';

                  let validationBrief = '';
                  try {
                    const rules = JSON.parse(item.validationRulesJson || '{}');
                    if (rules.minMinutes !== undefined) validationBrief = `Bounds: ${rules.minMinutes}m - ${rules.maxMinutes}m`;
                    else if (rules.allowOvernight !== undefined) validationBrief = `Overnight Allowed: ${rules.allowOvernight}`;
                    else if (rules.allowedValues) validationBrief = `Allowed: ${rules.allowedValues.slice(0, 3).join(', ')}...`;
                    else if (rules.allowedItems) validationBrief = `Allowed: ${rules.allowedItems.slice(0, 3).join(', ')}...`;
                  } catch {}

                  return (
                    <tr key={item.key}>
                      <td>
                        <code style={{ fontWeight: 600, color: '#93c5fd' }}>{item.key}</code>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          {item.description}
                        </div>
                        {validationBrief && (
                          <div style={{ fontSize: '0.7rem', color: '#a78bfa', marginTop: '2px', fontFamily: 'var(--mono)' }}>
                            Rule: {validationBrief}
                          </div>
                        )}
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
                      <td style={{ textAlign: 'center' }}>
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          onClick={() => handleOpenExplain(item)}
                          title="Explain why this value won across the 4 hierarchy levels"
                          style={{ padding: '4px 8px', fontSize: '0.75rem' }}
                        >
                          <HelpCircle size={13} color="#60a5fa" />
                          <span>Explain</span>
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Explain Mode Modal */}
      {explainItem && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0, 0, 0, 0.75)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '20px',
          }}
          onClick={() => setExplainItem(null)}
        >
          <div
            className="card"
            style={{
              maxWidth: 620,
              width: '100%',
              maxHeight: '90vh',
              overflowY: 'auto',
              border: '1px solid #3b82f6',
              boxShadow: '0 20px 40px rgba(0,0,0,0.6)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="card-header" style={{ borderBottom: '1px solid var(--border)', paddingBottom: '12px' }}>
              <div>
                <h3 className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <HelpCircle size={18} color="#3b82f6" />
                  Explain Mode: Why Value Won
                </h3>
                <p className="card-subtitle">
                  Setting: <code>{explainItem.key}</code> evaluated for{' '}
                  <strong>{selectedOffice || 'Tenant Baseline'}</strong> / <strong>{selectedShift || 'General Shift'}</strong>
                </p>
              </div>
              <button
                type="button"
                onClick={() => setExplainItem(null)}
                style={{ background: 'transparent', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}
              >
                <X size={18} />
              </button>
            </div>

            {explainLoading ? (
              <div style={{ padding: '36px', textAlign: 'center' }}>
                <RefreshCw size={20} className="spin" />
                <p style={{ marginTop: '8px', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                  Evaluating 4-level hierarchy precedence from backend...
                </p>
              </div>
            ) : explainData ? (
              <div style={{ padding: '16px 0' }}>
                {/* Winner Card */}
                <div
                  style={{
                    background: 'rgba(16, 185, 129, 0.1)',
                    border: '1px solid #059669',
                    borderRadius: 'var(--radius)',
                    padding: '14px 16px',
                    marginBottom: '20px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <div>
                    <div style={{ fontSize: '0.75rem', fontWeight: 600, color: '#34d399', textTransform: 'uppercase' }}>
                      Winning Level: {explainData.resolvedLevel}
                    </div>
                    <div style={{ fontSize: '1.2rem', fontWeight: 700, fontFamily: 'var(--mono)', color: '#34d399', marginTop: '2px' }}>
                      {explainData.resolvedValue}
                    </div>
                  </div>
                  <div style={{ textAlign: 'right', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                    <div>Author: <strong>{explainData.author}</strong></div>
                    <div>Version: <strong>v{explainData.version}</strong></div>
                  </div>
                </div>

                {/* Precedence Breakdown Ladder */}
                <h4 style={{ fontSize: '0.85rem', fontWeight: 600, marginBottom: '12px', color: 'var(--text-muted)' }}>
                  EVALUATION BREAKDOWN (Most specific level wins):
                </h4>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  {explainData.evaluatedLevels && explainData.evaluatedLevels.map((lvl, idx) => {
                    const isWinner = lvl.winning;
                    return (
                      <div
                        key={idx}
                        style={{
                          background: isWinner ? 'rgba(59, 130, 246, 0.15)' : 'var(--bg)',
                          border: `1px solid ${isWinner ? '#3b82f6' : 'var(--border)'}`,
                          borderRadius: 'var(--radius)',
                          padding: '12px 14px',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                          <span
                            className={`badge ${
                              lvl.level === 'SHIFT'
                                ? 'badge-purple'
                                : lvl.level === 'OFFICE'
                                ? 'badge-blue'
                                : lvl.level === 'CLIENT'
                                ? 'badge-green'
                                : 'badge-gray'
                            }`}
                          >
                            Level {lvl.level}
                          </span>
                          <span style={{ fontSize: '0.8rem', color: 'var(--text)' }}>
                            Scope: <code>{lvl.scopeIdentifier}</code>
                          </span>
                        </div>

                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                          <span style={{ fontFamily: 'var(--mono)', fontWeight: 600, color: isWinner ? '#34d399' : 'var(--text-muted)' }}>
                            {lvl.value || 'None'}
                          </span>
                          {isWinner ? (
                            <span className="badge badge-green" style={{ fontSize: '0.7rem' }}>
                              WINNER &check;
                            </span>
                          ) : (
                            <span className="badge badge-gray" style={{ fontSize: '0.7rem' }}>
                              Overridden
                            </span>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>

                <div style={{ marginTop: '20px', padding: '12px 14px', background: 'var(--bg)', borderRadius: 'var(--radius)', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                  <strong>Rule Applied:</strong> When evaluating <code>{explainItem.key}</code>, the engine checked <code>SHIFT</code>, then <code>OFFICE</code> ({selectedOffice || 'None'}), then <code>CLIENT</code> ({user.tenant}), and fell back to <code>GLOBAL</code> default. <strong>Level {explainData.resolvedLevel}</strong> took precedence.
                </div>
              </div>
            ) : null}
          </div>
        </div>
      )}
    </div>
  );
}
