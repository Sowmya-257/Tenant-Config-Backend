// MoveInSync Real-Time API Service — Directly Connected to Spring Boot Backend

export const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

export const TENANTS = [
  { id: 'MICROSOFT', name: 'Microsoft India', offices: ['BLR-01', 'BLR-02', 'HYD-01', 'DEL-01'] },
  { id: 'GOOGLE', name: 'Google Workspace Corp', offices: ['BLR-01', 'HYD-01', 'MUM-01'] },
  { id: 'AMAZON', name: 'Amazon Web Services', offices: ['HYD-01', 'BLR-01', 'DEL-01', 'CHN-01'] },
  { id: 'UBER', name: 'Uber Mobility Systems', offices: ['BLR-01', 'HYD-01', 'PUN-01'] },
];

export const OFFICE_NAMES = {
  'BLR-01': 'Bangalore Main Tech Campus',
  'BLR-02': 'Bangalore Whitefield Park',
  'HYD-01': 'Hyderabad Financial District',
  'DEL-01': 'Gurugram CyberHub Tower',
  'MUM-01': 'Mumbai Bandra Kurla Complex',
  'CHN-01': 'Chennai OMR Tech Zone',
  'PUN-01': 'Pune Hinjawadi Tech Park',
};

export const SHIFTS = ['MORNING', 'GENERAL', 'EVENING', 'NIGHT'];

/**
 * Direct HTTP API Client connected to Spring Boot Backend on /api/v1
 */
export const api = {
  // 1. Get all definitions from backend with validation rules
  async getDefinitions() {
    const res = await fetch(`${API_BASE}/api/v1/definitions`);
    if (!res.ok) {
      throw new Error(`Failed to load definitions from backend (Status: ${res.status})`);
    }
    return await res.json();
  },

  // 2. Get active monotonic version counter for a tenant
  async getVersion(tenantId) {
    const res = await fetch(`${API_BASE}/api/v1/sync/version?tenantId=${encodeURIComponent(tenantId)}`);
    if (!res.ok) {
      throw new Error(`Failed to fetch version for ${tenantId} from backend`);
    }
    const val = await res.json();
    return Number(val) || 1;
  },

  // 3. Resolve a single setting using the backend HierarchyResolutionService
  async resolveSetting(tenantId, key, officeId = null, shiftId = null) {
    const params = new URLSearchParams({ key });
    if (officeId) params.append('officeId', officeId);
    if (shiftId) params.append('shiftId', shiftId);

    const res = await fetch(`${API_BASE}/api/v1/configs/${encodeURIComponent(tenantId)}/resolve?${params.toString()}`);
    if (!res.ok) {
      throw new Error(`Backend resolution failed for ${key}`);
    }
    return await res.json();
  },

  // 4. Explain mode: Why a value won across all 4 hierarchy levels
  async explainSetting(tenantId, key, officeId = null, shiftId = null) {
    const params = new URLSearchParams({ key });
    if (officeId) params.append('officeId', officeId);
    if (shiftId) params.append('shiftId', shiftId);

    const res = await fetch(`${API_BASE}/api/v1/configs/${encodeURIComponent(tenantId)}/explain?${params.toString()}`);
    if (!res.ok) {
      throw new Error(`Explain resolution failed for ${key}`);
    }
    return await res.json();
  },

  // 5. Resolve all definitions for the current tenant, office, and shift directly from backend
  async resolveAllSettings(tenantId, officeId = null, shiftId = null) {
    const defs = await this.getDefinitions();
    const resolvedPromises = defs.map(async (def) => {
      try {
        const resolved = await this.resolveSetting(tenantId, def.key, officeId, shiftId);
        return {
          key: def.key,
          type: def.type,
          description: def.description,
          defaultValue: def.defaultValue,
          validationRulesJson: def.validationRulesJson,
          value: resolved.value,
          level: resolved.resolvedLevel,
          scope: resolved.resolvedLevel === 'SHIFT'
            ? `${officeId}#${shiftId}`
            : resolved.resolvedLevel === 'OFFICE'
            ? officeId
            : resolved.resolvedLevel === 'CLIENT'
            ? tenantId
            : 'Platform Default',
          author: resolved.author || 'system',
          version: resolved.version,
          reason: resolved.reason,
        };
      } catch {
        return {
          key: def.key,
          type: def.type,
          description: def.description,
          defaultValue: def.defaultValue,
          validationRulesJson: def.validationRulesJson,
          value: def.defaultValue,
          level: 'GLOBAL',
          scope: 'Platform Default',
          author: 'system',
          version: 0,
        };
      }
    });

    return await Promise.all(resolvedPromises);
  },

  // 6. Get full audit history from backend database
  async getHistory(tenantId) {
    const res = await fetch(`${API_BASE}/api/v1/configs/${encodeURIComponent(tenantId)}/history`);
    if (!res.ok) {
      throw new Error(`Failed to load history for ${tenantId} from backend`);
    }
    const historyList = await res.json();
    if (!Array.isArray(historyList)) return [];

    return historyList.map((cs) => ({
      id: cs.id,
      version: cs.version,
      author: cs.author,
      reason: cs.reason,
      timestamp: cs.createdAt,
      items: (cs.items || []).map((item) => ({
        settingKey: item.settingKey,
        level: item.level,
        officeId: item.officeId,
        shiftId: item.shiftId,
        oldValue: item.oldValue,
        value: item.deleted ? '(DELETED)' : item.newValue,
        type: item.type,
      })),
    }));
  },

  // 7. Commit atomic changeset directly to backend
  async applyBatchChanges(tenantId, items, author, reason) {
    const payload = {
      author,
      reason,
      items: items.map((it) => ({
        settingKey: it.settingKey,
        level: it.level,
        officeId: it.officeId || null,
        shiftId: it.shiftId || null,
        value: it.value,
        deleted: false,
      })),
    };

    const res = await fetch(`${API_BASE}/api/v1/configs/${encodeURIComponent(tenantId)}/changeset`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });

    if (!res.ok) {
      const errText = await res.text();
      let errMsg = `Backend error (${res.status})`;
      try {
        const errObj = JSON.parse(errText);
        errMsg = errObj.message || errObj.error || errMsg;
      } catch {
        if (errText) errMsg = errText;
      }
      throw new Error(errMsg);
    }

    const data = await res.json();
    return data.version;
  },

  // 8. Rollback directly via backend API
  async rollback(tenantId, targetVersion, author) {
    const params = new URLSearchParams({
      targetVersion: targetVersion.toString(),
      author,
    });

    const res = await fetch(`${API_BASE}/api/v1/configs/${encodeURIComponent(tenantId)}/rollback?${params.toString()}`, {
      method: 'POST',
    });

    if (!res.ok) {
      const errText = await res.text();
      let errMsg = `Rollback failed on backend (${res.status})`;
      try {
        const errObj = JSON.parse(errText);
        errMsg = errObj.message || errObj.error || errMsg;
      } catch {
        if (errText) errMsg = errText;
      }
      throw new Error(errMsg);
    }

    const diff = await res.json();
    return diff.newVersion;
  },

  // 9. Staged Rollouts (Canary) API
  async listRollouts(tenantId) {
    const res = await fetch(`${API_BASE}/api/v1/configs/${encodeURIComponent(tenantId)}/rollouts`);
    if (!res.ok) return [];
    return await res.json();
  },

  async createRollout(tenantId, settingKey, canaryValue, targetOffices, author, reason) {
    const params = new URLSearchParams({
      settingKey,
      canaryValue,
      targetOffices: targetOffices.join(','),
      author,
      reason: reason || 'Canary staged rollout',
    });

    const res = await fetch(`${API_BASE}/api/v1/configs/${encodeURIComponent(tenantId)}/rollouts?${params.toString()}`, {
      method: 'POST',
    });

    if (!res.ok) {
      const errText = await res.text();
      throw new Error(errText || 'Failed to start staged rollout');
    }
    return await res.json();
  },

  async promoteRollout(rolloutId, author, reason) {
    const params = new URLSearchParams({
      author,
      reason: reason || 'Canary promotion to all offices',
    });

    const res = await fetch(`${API_BASE}/api/v1/rollouts/${rolloutId}/promote?${params.toString()}`, {
      method: 'POST',
    });

    if (!res.ok) {
      const errText = await res.text();
      throw new Error(errText || 'Failed to promote rollout');
    }
    return await res.json();
  },

  async abortRollout(rolloutId, author, reason) {
    const params = new URLSearchParams({
      author,
      reason: reason || 'Canary aborted',
    });

    const res = await fetch(`${API_BASE}/api/v1/rollouts/${rolloutId}/abort?${params.toString()}`, {
      method: 'POST',
    });

    if (!res.ok) {
      const errText = await res.text();
      throw new Error(errText || 'Failed to abort rollout');
    }
    return await res.json();
  },

  // 10. Actuator Metrics
  async getMetrics() {
    try {
      const res = await fetch(`${API_BASE}/actuator/metrics`);
      if (res.ok) return await res.json();
    } catch {}
    return null;
  },

  // Helper for tenant offices
  getOffices(tenantId) {
    const t = TENANTS.find((x) => x.id === tenantId);
    return t ? t.offices : ['BLR-01', 'HYD-01'];
  },
};

// Default export alias for compatibility
export const store = api;
