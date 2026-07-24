import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as patentsApi from '../api/patents';
import { downloadFile } from '../api/files';
import { ErrorAlert, Loading, SuccessAlert } from '../components/Feedback';
import type { Patent, PatentUpdateRequest } from '../types';
import { ApiError } from '../types';

function formatDate(value: string | null): string {
  if (!value) return '—';
  try {
    return new Date(value).toLocaleString('pt-BR');
  } catch {
    return value;
  }
}

function toForm(patent: Patent): PatentUpdateRequest {
  return {
    title: patent.title,
    abstractText: patent.abstractText,
    blacklisted: patent.blacklisted,
    completed: patent.completed,
  };
}

function PatentEditForm({
  patent,
  projectId,
}: {
  patent: Patent;
  projectId: string;
}) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<PatentUpdateRequest>(() => toForm(patent));
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const saveMutation = useMutation({
    mutationFn: () => patentsApi.updatePatent(patent.id, form),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['patent', patent.id] });
      void queryClient.invalidateQueries({ queryKey: ['patents', projectId] });
      setFeedback('Patente atualizada.');
      setError(null);
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : 'Falha ao salvar.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => patentsApi.deletePatent(patent.id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['patents', projectId] });
      navigate(`/projects/${projectId}/patents`);
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : 'Falha ao excluir.');
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    saveMutation.mutate();
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Detalhe da patente</h1>
          <p className="muted">{patent.publicationNumber || patent.id}</p>
        </div>
        <div className="stack-actions">
          <Link className="btn btn-secondary" to={`/projects/${projectId}/patents`}>
            Voltar à lista
          </Link>
          <button
            type="button"
            className="btn btn-danger"
            onClick={() => {
              if (confirm('Excluir esta patente?')) deleteMutation.mutate();
            }}
          >
            Excluir
          </button>
        </div>
      </div>

      {feedback && <SuccessAlert message={feedback} />}
      {error && <ErrorAlert message={error} />}

      <div className="card">
        <form className="form-grid" onSubmit={onSubmit}>
          <div className="field">
            <label htmlFor="title">Título</label>
            <input
              id="title"
              value={form.title || ''}
              onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
            />
          </div>
          <div className="field">
            <label htmlFor="abstractText">Resumo</label>
            <textarea
              id="abstractText"
              rows={5}
              value={form.abstractText || ''}
              onChange={(e) => setForm((f) => ({ ...f, abstractText: e.target.value }))}
            />
          </div>
          <div className="form-grid two">
            <label className="field-inline">
              <input
                type="checkbox"
                checked={!!form.blacklisted}
                onChange={(e) => setForm((f) => ({ ...f, blacklisted: e.target.checked }))}
              />
              Lista negra
            </label>
            <label className="field-inline">
              <input
                type="checkbox"
                checked={!!form.completed}
                onChange={(e) => setForm((f) => ({ ...f, completed: e.target.checked }))}
              />
              Completa
            </label>
          </div>
          <button type="submit" className="btn" disabled={saveMutation.isPending}>
            {saveMutation.isPending ? 'Salvando…' : 'Salvar alterações'}
          </button>
        </form>
      </div>

      <div className="card">
        <h2>Metadados</h2>
        <div className="table-wrap">
          <table className="data">
            <tbody>
              <tr>
                <th>Nº publicação</th>
                <td>{patent.publicationNumber || '—'}</td>
              </tr>
              <tr>
                <th>Data publicação</th>
                <td>{formatDate(patent.publicationDate)}</td>
              </tr>
              <tr>
                <th>Nº depósito</th>
                <td>{patent.applicationNumber || '—'}</td>
              </tr>
              <tr>
                <th>Data depósito</th>
                <td>{formatDate(patent.applicationDate)}</td>
              </tr>
              <tr>
                <th>País</th>
                <td>{patent.applicationCountry || '—'}</td>
              </tr>
              <tr>
                <th>Classificação principal</th>
                <td>{patent.mainClassification || '—'}</td>
              </tr>
              <tr>
                <th>Depositantes</th>
                <td>{(patent.applicants || []).join(', ') || '—'}</td>
              </tr>
              <tr>
                <th>Inventores</th>
                <td>{(patent.inventors || []).join(', ') || '—'}</td>
              </tr>
              <tr>
                <th>Arquivos</th>
                <td>
                  <div className="stack-actions">
                    {patent.presentationFileId ? (
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={() =>
                          void downloadFile(patent.presentationFileId!, 'presentation')
                        }
                      >
                        Apresentação
                      </button>
                    ) : (
                      <span className="muted">Sem apresentação</span>
                    )}
                    {patent.patentInfoFileId ? (
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={() => void downloadFile(patent.patentInfoFileId!, 'patent-info')}
                      >
                        Info patente
                      </button>
                    ) : (
                      <span className="muted">Sem info</span>
                    )}
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export function PatentDetailPage() {
  const { id: projectId = '', patentId = '' } = useParams();

  const patentQuery = useQuery({
    queryKey: ['patent', patentId],
    queryFn: () => patentsApi.getPatent(patentId),
    enabled: !!patentId,
  });

  if (patentQuery.isLoading) return <Loading />;
  if (patentQuery.isError || !patentQuery.data) {
    return (
      <ErrorAlert
        message={
          patentQuery.error instanceof Error
            ? patentQuery.error.message
            : 'Patente não encontrada.'
        }
      />
    );
  }

  return <PatentEditForm key={patentQuery.data.id} patent={patentQuery.data} projectId={projectId} />;
}
