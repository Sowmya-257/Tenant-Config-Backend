import React, { useState, useEffect } from 'react';
import { History, RotateCcw, RefreshCw, AlertCircle } from 'lucide-react';
import { api } from '../services/store';

export default function HistoryPage({ user, onRollbackComplete, showToast }) {
  const [history, setHistory] = useState([]);
  const [currentVersion, setCurrentVersion] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [rollingBackVer, setRollingBackVer] = useState(null);

  const loadHistoryData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [ver, hist] = await Promise.all([
        api.getVersion(user.tenant),
        api.getHistory(user.tenant),
      ]);
      setCurrentVersion(ver);
      setHistory(hist);
    } catch (err) {
      console.error('Failed to load history from backend', err);
      setError(err.message || 'Could not fetch history from backend');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadHistoryData();
  }, [user.tenant]);

  const handleRollback = async (targetVer) => {
    if (user.role === 'ROLE_EMPLOYEE') {
      showToast('Employees have read-only access. Only Admin and Operator can execute rollbacks.', 'error');
      return;
    }

    if (!window.confirm(`Are you sure you want to rollback to version v${targetVer}?`)) {
      return;
    }

    setRollingBackVer(targetVer);
    try {
      const nextVer = await api.rollback(user.tenant, targetVer, user.username);
      showToast(`Rolled back to settings of v${targetVer}! Committed as forward version v${nextVer}.`, 'success');
      await loadHistoryData();
      onRollbackComplete();
    } catch (err) {
      showToast('Rollback failed on backend: ' + err.message, 'error');
    } finally {
      setRollingBackVer(null);
    }
  };

  return (
    <div>
      <div className="card">
        <div className="card-header">
          <div>
            <h2 className="card-title">
              <History size={18} color="#3b82f6" />
              Version History &amp; Rollback ({user.tenant})
            </h2>
            <p className="card-subtitle">
              Live audit trail from Spring Boot database. Each row represents 1 version and all settings committed under it.
            </p>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span className="badge badge-green">Current: v{currentVersion}</span>
            <button
              className="btn btn-secondary btn-sm"
              onClick={loadHistoryData}
              title="Refresh history from backend"
            >
              <RefreshCw size={13} className={loading ? 'spin' : ''} />
              <span>Refresh</span>
            </button>
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
              margin: '0 0 16px 0',
            }}
          >
            <AlertCircle size={18} />
            <span>Error loading history: {error}</span>
          </div>
        )}

        {loading ? (
          <div style={{ padding: '36px', textAlign: 'center', color: 'var(--text-muted)' }}>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
              <RefreshCw size={16} className="spin" />
              <span>Fetching audit history from backend server...</span>
            </div>
          </div>
        ) : history.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '32px', color: 'var(--text-muted)' }}>
            No history records found for {user.tenant}.
          </div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th style={{ width: '100px' }}>Version</th>
                  <th>Changes in this Version</th>
                  <th>Reason</th>
                  <th>Author</th>
                  <th>Timestamp</th>
                  <th style={{ width: '100px', textAlign: 'center' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {history.map((h, idx) => {
                  const isCurrent = h.version === currentVersion;
                  const items = h.items || [];

                  return (
                    <tr
                      key={h.version || idx}
                      style={{ background: isCurrent ? 'rgba(16, 185, 129, 0.05)' : 'transparent' }}
                    >
                      {/* Version column */}
                      <td style={{ verticalAlign: 'top' }}>
                        <span
                          style={{
                            fontFamily: 'var(--mono)',
                            fontWeight: 700,
                            fontSize: '0.95rem',
                            color: isCurrent ? '#34d399' : '#60a5fa',
                          }}
                        >
                          v{h.version}
                        </span>
                        {isCurrent && (
                          <div style={{ marginTop: '4px' }}>
                            <span className="badge badge-green" style={{ fontSize: '0.65rem' }}>
                              Active
                            </span>
                          </div>
                        )}
                      </td>

                      {/* Changes in this version (grouped in 1 line/row) */}
                      <td style={{ verticalAlign: 'top' }}>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '5px' }}>
                          {items.length === 0 ? (
                            <span style={{ color: 'var(--text-muted)' }}>Baseline initialization</span>
                          ) : (
                            items.map((item, itemIdx) => (
                              <div
                                key={itemIdx}
                                style={{
                                  display: 'inline-flex',
                                  alignItems: 'center',
                                  gap: '6px',
                                  fontSize: '0.8rem',
                                  flexWrap: 'wrap',
                                }}
                              >
                                <code style={{ color: '#93c5fd', fontWeight: 600 }}>{item.settingKey}</code>
                                <span className="badge badge-gray" style={{ fontSize: '0.7rem' }}>
                                  {item.level}
                                  {item.officeId ? ` (${item.officeId})` : ''}
                                  {item.shiftId ? ` [${item.shiftId}]` : ''}
                                </span>
                                <span>&rarr;</span>
                                <strong
                                  style={{
                                    fontFamily: 'var(--mono)',
                                    color: item.value === '(DELETED)' ? '#f87171' : '#34d399',
                                  }}
                                >
                                  {item.value}
                                </strong>
                              </div>
                            ))
                          )}
                        </div>
                      </td>

                      {/* Reason */}
                      <td style={{ verticalAlign: 'top', maxWidth: 220, fontSize: '0.8rem', color: 'var(--text)' }}>
                        {h.reason}
                      </td>

                      {/* Author */}
                      <td style={{ verticalAlign: 'top', fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                        {h.author}
                      </td>

                      {/* Timestamp */}
                      <td style={{ verticalAlign: 'top', fontSize: '0.8rem', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                        {new Date(h.timestamp).toLocaleString()}
                      </td>

                      {/* Rollback action */}
                      <td style={{ verticalAlign: 'top', textAlign: 'center' }}>
                        {!isCurrent && user.role !== 'ROLE_EMPLOYEE' && (
                          <button
                            className="btn btn-secondary btn-sm"
                            onClick={() => handleRollback(h.version)}
                            disabled={rollingBackVer !== null}
                            title={`Rollback configuration to state of v${h.version}`}
                          >
                            <RotateCcw size={12} className={rollingBackVer === h.version ? 'spin' : ''} />
                            <span>{rollingBackVer === h.version ? 'Rolling back...' : 'Rollback'}</span>
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
