import React, { useState, useEffect } from 'react';
import { Sliders, Save, AlertCircle, CheckCircle2, ArrowLeft, Plus, Trash2, Building, Check, X, RefreshCw } from 'lucide-react';
import { api, SHIFTS, OFFICE_NAMES } from '../services/store';

export default function EditConfigPage({ user, preselectedKey, onSaved, onCancel, showToast }) {
  const offices = api.getOffices(user.tenant);
  const [definitions, setDefinitions] = useState([]);
  const [currentVersion, setCurrentVersion] = useState(1);
  const [loading, setLoading] = useState(true);

  // Array of change blocks under 1 version
  const [changes, setChanges] = useState([
    {
      id: Date.now(),
      settingKey: preselectedKey || 'booking.cutoff.mins',
      level: 'OFFICE',
      selectedOffices: [offices[0] || 'BLR-01'],
      shiftOfficeId: offices[0] || 'BLR-01',
      shiftId: 'NIGHT',
      value: '25m',
    },
  ]);

  const [author, setAuthor] = useState(user.username);
  const [reason, setReason] = useState('Optimize cutoff times and fleet parameters');
  const [submitting, setSubmitting] = useState(false);

  const isReadOnly = user.role === 'ROLE_EMPLOYEE';

  useEffect(() => {
    async function loadBackendData() {
      setLoading(true);
      try {
        const [defs, ver] = await Promise.all([
          api.getDefinitions(),
          api.getVersion(user.tenant),
        ]);
        setDefinitions(defs);
        setCurrentVersion(ver);

        // Adjust default value for preselectedKey if found
        if (defs && defs.length > 0) {
          const targetKey = preselectedKey || defs[0].key;
          const matchedDef = defs.find((d) => d.key === targetKey) || defs[0];
          setChanges([
            {
              id: Date.now(),
              settingKey: matchedDef.key,
              level: 'OFFICE',
              selectedOffices: [offices[0] || 'BLR-01'],
              shiftOfficeId: offices[0] || 'BLR-01',
              shiftId: 'NIGHT',
              value: matchedDef.defaultValue || '30m',
            },
          ]);
        }
      } catch (err) {
        console.error('Failed to load edit config metadata from backend', err);
        showToast('Failed to load definitions from backend: ' + err.message, 'error');
      } finally {
        setLoading(false);
      }
    }

    loadBackendData();
  }, [user.tenant]);

  // Add another change block
  const handleAddChange = () => {
    const defaultDef = definitions[0] || { key: 'booking.cutoff.mins', defaultValue: '30m' };
    setChanges([
      ...changes,
      {
        id: Date.now() + Math.random(),
        settingKey: defaultDef.key,
        level: 'OFFICE',
        selectedOffices: [offices[0] || 'BLR-01'],
        shiftOfficeId: offices[0] || 'BLR-01',
        shiftId: 'NIGHT',
        value: defaultDef.defaultValue || '30m',
      },
    ]);
  };

  // Remove a change block
  const handleRemoveChange = (id) => {
    if (changes.length === 1) {
      showToast('You must have at least one change in this version.', 'error');
      return;
    }
    setChanges(changes.filter((c) => c.id !== id));
  };

  // Update a field in a change block
  const updateChange = (id, field, val) => {
    setChanges(
      changes.map((c) => {
        if (c.id !== id) return c;
        const updated = { ...c, [field]: val };

        // Auto-fill default value when settingKey changes
        if (field === 'settingKey') {
          const def = definitions.find((d) => d.key === val);
          if (def) updated.value = def.defaultValue;
        }

        return updated;
      })
    );
  };

  // Add an office to a change block
  const addOfficeForChange = (changeId, officeCode) => {
    if (!officeCode) return;
    setChanges(
      changes.map((c) => {
        if (c.id !== changeId) return c;
        const currentList = c.selectedOffices || [];
        if (!currentList.includes(officeCode)) {
          return { ...c, selectedOffices: [...currentList, officeCode] };
        }
        return c;
      })
    );
  };

  // Remove an office from a change block
  const removeOfficeFromChange = (changeId, officeCode) => {
    setChanges(
      changes.map((c) => {
        if (c.id !== changeId) return c;
        const currentList = c.selectedOffices || [];
        return { ...c, selectedOffices: currentList.filter((o) => o !== officeCode) };
      })
    );
  };

  // Quick select all offices for a change
  const selectAllOffices = (changeId) => {
    setChanges(
      changes.map((c) => {
        if (c.id !== changeId) return c;
        return { ...c, selectedOffices: [...offices] };
      })
    );
  };

  // Clear all selected offices for a change
  const clearAllOffices = (changeId) => {
    setChanges(
      changes.map((c) => {
        if (c.id !== changeId) return c;
        return { ...c, selectedOffices: [] };
      })
    );
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (isReadOnly) {
      showToast('Employees have read-only access. Only Admin and Operator can modify configurations.', 'error');
      return;
    }

    if (!reason.trim()) {
      showToast('Please provide an audit reason for this version.', 'error');
      return;
    }

    // Build the flat list of atomic change items
    const batchItems = [];

    for (let i = 0; i < changes.length; i++) {
      const c = changes[i];
      if (!c.value.trim()) {
        showToast(`Please enter a value for change #${i + 1} (${c.settingKey}).`, 'error');
        return;
      }

      if (c.level === 'CLIENT') {
        batchItems.push({
          settingKey: c.settingKey,
          level: 'CLIENT',
          officeId: null,
          shiftId: null,
          value: c.value.trim(),
        });
      } else if (c.level === 'OFFICE') {
        if (!c.selectedOffices || c.selectedOffices.length === 0) {
          showToast(`Select at least one office for change #${i + 1}.`, 'error');
          return;
        }
        // Expand selected offices into individual atomic items under this single version
        for (const off of c.selectedOffices) {
          batchItems.push({
            settingKey: c.settingKey,
            level: 'OFFICE',
            officeId: off,
            shiftId: null,
            value: c.value.trim(),
          });
        }
      } else if (c.level === 'SHIFT') {
        batchItems.push({
          settingKey: c.settingKey,
          level: 'SHIFT',
          officeId: c.shiftOfficeId || offices[0],
          shiftId: c.shiftId || 'NIGHT',
          value: c.value.trim(),
        });
      }
    }

    setSubmitting(true);
    try {
      const nextVer = await api.applyBatchChanges(user.tenant, batchItems, author, reason);
      showToast(
        `Committed ${batchItems.length} changes to Spring Boot backend under Version v${nextVer}!`,
        'success'
      );
      onSaved();
    } catch (err) {
      showToast('Failed to apply changes on backend: ' + err.message, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="card" style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
          <RefreshCw size={16} className="spin" />
          <span>Loading configuration definitions from backend...</span>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="card">
        <div className="card-header">
          <div>
            <h2 className="card-title">
              <Sliders size={18} color="#3b82f6" />
              Edit Configuration ({user.tenant})
            </h2>
            <p className="card-subtitle">
              Add one or more changes to commit together under <strong>Version v{currentVersion + 1}</strong>.
            </p>
          </div>
          <button className="btn btn-secondary btn-sm" onClick={onCancel}>
            <ArrowLeft size={14} />
            <span>Back to Dashboard</span>
          </button>
        </div>

        {isReadOnly && (
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
              marginBottom: '20px',
            }}
          >
            <AlertCircle size={18} />
            <span>You are logged in as an Employee (Read-Only). You cannot modify configurations.</span>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          {/* Multiple Change Blocks */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '20px' }}>
            {changes.map((change, index) => {
              const def = definitions.find((d) => d.key === change.settingKey);

              return (
                <div
                  key={change.id}
                  style={{
                    background: 'var(--bg)',
                    border: '1px solid var(--border)',
                    borderRadius: 'var(--radius)',
                    padding: '16px',
                    position: 'relative',
                  }}
                >
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      marginBottom: '12px',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span className="badge badge-blue">Change #{index + 1}</span>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                        Type: {def?.type || 'STRING'}
                      </span>
                    </div>

                    {changes.length > 1 && (
                      <button
                        type="button"
                        className="btn btn-danger btn-sm"
                        onClick={() => handleRemoveChange(change.id)}
                        disabled={isReadOnly}
                        title="Remove this change"
                        style={{ padding: '3px 8px' }}
                      >
                        <Trash2 size={13} />
                        <span>Remove</span>
                      </button>
                    )}
                  </div>

                  {/* Setting Key & Hierarchy Level */}
                  <div style={{ display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: '12px', marginBottom: '12px' }}>
                    <div>
                      <label className="form-label">Setting</label>
                      <select
                        className="select"
                        value={change.settingKey}
                        onChange={(e) => updateChange(change.id, 'settingKey', e.target.value)}
                        disabled={isReadOnly}
                      >
                        {definitions.map((d) => (
                          <option key={d.key} value={d.key}>
                            {d.key} ({d.type}) — Default: {d.defaultValue}
                          </option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label className="form-label">Hierarchy Target Level</label>
                      <select
                        className="select"
                        value={change.level}
                        onChange={(e) => updateChange(change.id, 'level', e.target.value)}
                        disabled={isReadOnly}
                      >
                        <option value="OFFICE">Office Specific</option>
                        <option value="CLIENT">Client-Wide (All offices)</option>
                        <option value="SHIFT">Shift Specific (Select office & shift)</option>
                      </select>
                    </div>
                  </div>

                  {/* If Level is OFFICE: Dropdown with Option to Add More & Chips Below */}
                  {change.level === 'OFFICE' && (
                    <div style={{ marginBottom: '14px' }}>
                      <div
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          marginBottom: '6px',
                        }}
                      >
                        <label className="form-label" style={{ marginBottom: 0 }}>
                          Select Offices
                        </label>
                        <div style={{ display: 'flex', gap: '10px' }}>
                          <button
                            type="button"
                            onClick={() => selectAllOffices(change.id)}
                            disabled={isReadOnly}
                            style={{
                              background: 'transparent',
                              border: 'none',
                              color: '#60a5fa',
                              fontSize: '0.75rem',
                              cursor: 'pointer',
                              textDecoration: 'underline',
                            }}
                          >
                            Select All
                          </button>
                          {(change.selectedOffices || []).length > 0 && (
                            <button
                              type="button"
                              onClick={() => clearAllOffices(change.id)}
                              disabled={isReadOnly}
                              style={{
                                background: 'transparent',
                                border: 'none',
                                color: 'var(--text-muted)',
                                fontSize: '0.75rem',
                                cursor: 'pointer',
                                textDecoration: 'underline',
                              }}
                            >
                              Clear
                            </button>
                          )}
                        </div>
                      </div>

                      {/* Dropdown to pick offices */}
                      <select
                        className="select"
                        value=""
                        onChange={(e) => {
                          if (e.target.value) {
                            addOfficeForChange(change.id, e.target.value);
                          }
                        }}
                        disabled={isReadOnly}
                        style={{ marginBottom: '8px' }}
                      >
                        <option value="">+ Choose an office to add...</option>
                        {offices.map((off) => {
                          const isSelected = (change.selectedOffices || []).includes(off);
                          return (
                            <option key={off} value={off} disabled={isSelected}>
                              {off} — {OFFICE_NAMES[off] || 'Campus'} {isSelected ? '(Already Added)' : ''}
                            </option>
                          );
                        })}
                      </select>

                      {/* Selected Offices Listed Below as Removable Chips */}
                      <div
                        style={{
                          display: 'flex',
                          flexWrap: 'wrap',
                          gap: '6px',
                          padding: (change.selectedOffices || []).length > 0 ? '8px 10px' : '0',
                          background: (change.selectedOffices || []).length > 0 ? 'var(--panel)' : 'transparent',
                          borderRadius: 'var(--radius)',
                          border: (change.selectedOffices || []).length > 0 ? '1px solid var(--border)' : 'none',
                          minHeight: (change.selectedOffices || []).length > 0 ? 'auto' : '0',
                        }}
                      >
                        {(change.selectedOffices || []).length === 0 ? (
                          <div style={{ fontSize: '0.8rem', color: '#f87171', padding: '4px 0' }}>
                            Please select at least one office from the dropdown above.
                          </div>
                        ) : (
                          (change.selectedOffices || []).map((off) => (
                            <span
                              key={off}
                              style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '6px',
                                background: 'rgba(59, 130, 246, 0.15)',
                                border: '1px solid #3b82f6',
                                color: '#93c5fd',
                                padding: '4px 10px',
                                borderRadius: '16px',
                                fontSize: '0.8rem',
                                fontWeight: 500,
                              }}
                            >
                              <Building size={12} color="#60a5fa" />
                              <span>{off} ({OFFICE_NAMES[off] || 'Campus'})</span>
                              {!isReadOnly && (
                                <button
                                  type="button"
                                  onClick={() => removeOfficeFromChange(change.id, off)}
                                  style={{
                                    background: 'transparent',
                                    border: 'none',
                                    color: '#93c5fd',
                                    cursor: 'pointer',
                                    padding: '0 2px',
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    marginLeft: '2px',
                                    borderRadius: '50%',
                                  }}
                                  title={`Remove ${off}`}
                                >
                                  <X size={13} />
                                </button>
                              )}
                            </span>
                          ))
                        )}
                      </div>
                    </div>
                  )}

                  {/* If Level is SHIFT */}
                  {change.level === 'SHIFT' && (
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '12px' }}>
                      <div>
                        <label className="form-label">Office</label>
                        <select
                          className="select"
                          value={change.shiftOfficeId}
                          onChange={(e) => updateChange(change.id, 'shiftOfficeId', e.target.value)}
                          disabled={isReadOnly}
                        >
                          {offices.map((off) => (
                            <option key={off} value={off}>
                              {off} — {OFFICE_NAMES[off] || 'Campus'}
                            </option>
                          ))}
                        </select>
                      </div>

                      <div>
                        <label className="form-label">Shift</label>
                        <select
                          className="select"
                          value={change.shiftId}
                          onChange={(e) => updateChange(change.id, 'shiftId', e.target.value)}
                          disabled={isReadOnly}
                        >
                          {SHIFTS.map((sh) => (
                            <option key={sh} value={sh}>
                              {sh}
                            </option>
                          ))}
                        </select>
                      </div>
                    </div>
                  )}

                  {/* New Value */}
                  <div>
                    <label className="form-label">Value to Apply</label>
                    <input
                      type="text"
                      className="input"
                      value={change.value}
                      onChange={(e) => updateChange(change.id, 'value', e.target.value)}
                      placeholder="e.g. 20m"
                      disabled={isReadOnly}
                      required
                      style={{ fontFamily: 'var(--mono)' }}
                    />
                  </div>
                </div>
              );
            })}
          </div>

          {/* "+ Add Another Setting / Change" button */}
          <div style={{ marginBottom: '24px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={handleAddChange}
              disabled={isReadOnly}
              style={{ width: '100%', borderStyle: 'dashed' }}
            >
              <Plus size={15} />
              <span>+ Add Another Setting / Change (In Same Version)</span>
            </button>
          </div>

          {/* Common Author and Reason fields for this Version */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '16px', marginBottom: '20px' }}>
            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Author</label>
              <input
                type="text"
                className="input"
                value={author}
                onChange={(e) => setAuthor(e.target.value)}
                disabled={isReadOnly}
                required
              />
            </div>

            <div className="form-group" style={{ marginBottom: 0 }}>
              <label className="form-label">Reason for this Version (Audit Trail)</label>
              <input
                type="text"
                className="input"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="Reason for this batch of changes"
                disabled={isReadOnly}
                required
              />
            </div>
          </div>

          {/* Submit Action */}
          <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={isReadOnly || submitting}
              style={{ minWidth: 200 }}
            >
              <Save size={16} />
              <span>
                {submitting
                  ? 'Committing to Backend...'
                  : `Commit All Changes under Version v${currentVersion + 1}`}
              </span>
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={onCancel}
            >
              Cancel
            </button>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginLeft: 'auto' }}>
              All {changes.length} change(s) will be committed together as <strong>1 version</strong> on <strong>1 line</strong> in History.
            </span>
          </div>
        </form>
      </div>
    </div>
  );
}
