import type { FormEvent } from 'react';
import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as harmApi from '../api/harmonization';
import { ErrorAlert, Loading, SuccessAlert } from '../components/Feedback';
import type { RuleRequest } from '../types';
import { ApiError } from '../types';

export function HarmonizationPage() {
  const { id: projectId = '' } = useParams();
  const queryClient = useQueryClient();
  const [suggestType, setSuggestType] = useState<'applicant' | 'inventor'>('applicant');
  const [query, setQuery] = useState('');
  const [debounced, setDebounced] = useState('');
  const [selected, setSelected] = useState<string[]>([]);
  const [ruleName, setRuleName] = useState('');
  const [ruleAcronym, setRuleAcronym] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const t = window.setTimeout(() => setDebounced(query.trim()), 300);
    return () => window.clearTimeout(t);
  }, [query]);

  const suggestionsQuery = useQuery({
    queryKey: ['suggestions', projectId, suggestType, debounced],
    queryFn: () => harmApi.getSuggestions(projectId, suggestType, debounced),
    enabled: !!projectId && debounced.length >= 2,
  });

  const rulesQuery = useQuery({
    queryKey: ['rules', projectId],
    queryFn: () => harmApi.listRules(projectId),
    enabled: !!projectId,
  });

  const createMutation = useMutation({
    mutationFn: (data: RuleRequest) => harmApi.createRule(projectId, data),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['rules', projectId] });
      setFeedback('Regra criada.');
      setRuleName('');
      setRuleAcronym('');
      setSelected([]);
      setError(null);
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : 'Falha ao criar regra.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (ruleId: string) => harmApi.deleteRule(projectId, ruleId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['rules', projectId] });
      setFeedback('Regra removida.');
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : 'Falha ao remover regra.');
    },
  });

  const applyMutation = useMutation({
    mutationFn: () => harmApi.applyAllRules(projectId),
    onSuccess: (data) => {
      setFeedback(data.message || `${data.applied} regra(s) aplicada(s).`);
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : 'Falha ao aplicar regras.');
    },
  });

  const applyOneMutation = useMutation({
    mutationFn: (ruleId: string) => harmApi.applyRule(projectId, ruleId),
    onSuccess: (data) => setFeedback(data.message),
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : 'Falha ao aplicar regra.');
    },
  });

  const suggestions = suggestionsQuery.data?.suggestions || [];

  const selectedSet = useMemo(() => new Set(selected), [selected]);

  function toggleSuggestion(name: string) {
    setSelected((prev) =>
      prev.includes(name) ? prev.filter((s) => s !== name) : [...prev, name],
    );
  }

  function onCreate(e: FormEvent) {
    e.preventDefault();
    if (!ruleName.trim()) {
      setError('Informe o nome canônico da regra.');
      return;
    }
    createMutation.mutate({
      name: ruleName.trim(),
      acronym: ruleAcronym.trim() || null,
      type: suggestType === 'applicant' ? 'APPLICANT' : 'INVENTOR',
      substitutions: selected,
    });
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Harmonização</h1>
          <p>Busque sugestões e gerencie regras de normalização de nomes.</p>
        </div>
        <div className="stack-actions">
          <button
            type="button"
            className="btn"
            disabled={applyMutation.isPending}
            onClick={() => applyMutation.mutate()}
          >
            Aplicar todas as regras
          </button>
          <Link className="btn btn-secondary" to={`/projects/${projectId}/patents`}>
            Patentes
          </Link>
        </div>
      </div>

      {feedback && <SuccessAlert message={feedback} />}
      {error && <ErrorAlert message={error} />}

      <div className="card">
        <h2>Sugestões</h2>
        <div className="form-grid two">
          <div className="field">
            <label htmlFor="suggestType">Tipo</label>
            <select
              id="suggestType"
              value={suggestType}
              onChange={(e) => {
                setSuggestType(e.target.value as 'applicant' | 'inventor');
                setSelected([]);
              }}
            >
              <option value="applicant">Depositante</option>
              <option value="inventor">Inventor</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="query">Busca</label>
            <input
              id="query"
              data-testid="harmonization-query"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Digite ao menos 2 caracteres"
            />
          </div>
        </div>

        {debounced.length >= 2 && (
          <div style={{ marginTop: '1rem' }}>
            {suggestionsQuery.isLoading ? (
              <Loading label="Buscando sugestões…" />
            ) : suggestionsQuery.isError ? (
              <ErrorAlert
                message={
                  suggestionsQuery.error instanceof Error
                    ? suggestionsQuery.error.message
                    : 'Erro nas sugestões.'
                }
              />
            ) : suggestions.length === 0 ? (
              <p className="muted">Nenhuma sugestão encontrada.</p>
            ) : (
              <div className="suggestions-box" role="listbox" aria-label="Sugestões" data-testid="suggestions-list">
                {suggestions.map((name) => (
                  <button
                    key={name}
                    type="button"
                    data-testid={`suggestion-${name}`}
                    onClick={() => toggleSuggestion(name)}
                    aria-selected={selectedSet.has(name)}
                  >
                    {selectedSet.has(name) ? '✓ ' : ''}
                    {name}
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

        {selected.length > 0 && (
          <div style={{ marginTop: '1rem' }}>
            <p className="muted">Substituições selecionadas:</p>
            <div className="chips">
              {selected.map((name) => (
                <button key={name} type="button" className="chip" onClick={() => toggleSuggestion(name)}>
                  {name} ✕
                </button>
              ))}
            </div>
          </div>
        )}

        <form className="form-grid" style={{ marginTop: '1.25rem' }} onSubmit={onCreate}>
          <div className="form-grid two">
            <div className="field">
              <label htmlFor="ruleName">Nome canônico</label>
              <input
                id="ruleName"
                value={ruleName}
                onChange={(e) => setRuleName(e.target.value)}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="ruleAcronym">Sigla</label>
              <input
                id="ruleAcronym"
                value={ruleAcronym}
                onChange={(e) => setRuleAcronym(e.target.value)}
              />
            </div>
          </div>
          <button type="submit" className="btn" disabled={createMutation.isPending}>
            {createMutation.isPending ? 'Criando…' : 'Criar regra'}
          </button>
        </form>
      </div>

      <div className="card">
        <h2>Regras</h2>
        {rulesQuery.isLoading ? (
          <Loading />
        ) : rulesQuery.isError ? (
          <ErrorAlert
            message={
              rulesQuery.error instanceof Error ? rulesQuery.error.message : 'Erro ao listar regras.'
            }
          />
        ) : !rulesQuery.data?.length ? (
          <div className="empty-block">Nenhuma regra cadastrada.</div>
        ) : (
          <div className="table-wrap">
            <table className="data">
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>Tipo</th>
                  <th>Sigla</th>
                  <th>Substituições</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {rulesQuery.data.map((rule) => {
                  const subs = Array.isArray(rule.substitutions)
                    ? rule.substitutions
                    : rule.substitutions
                      ? Array.from(rule.substitutions)
                      : [];
                  return (
                    <tr key={rule.id}>
                      <td>{rule.name}</td>
                      <td>
                        <span className="badge">{rule.type}</span>
                      </td>
                      <td>{rule.acronym || '—'}</td>
                      <td className="muted">{subs.join(', ') || '—'}</td>
                      <td>
                        <div className="stack-actions">
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            onClick={() => applyOneMutation.mutate(rule.id)}
                          >
                            Aplicar
                          </button>
                          <button
                            type="button"
                            className="btn btn-danger btn-sm"
                            onClick={() => {
                              if (confirm('Remover esta regra?')) {
                                deleteMutation.mutate(rule.id);
                              }
                            }}
                          >
                            Excluir
                          </button>
                        </div>
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
