import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as patentsApi from '../api/patents';
import * as projectsApi from '../api/projects';
import { ErrorAlert, Loading, SuccessAlert } from '../components/Feedback';
import { Pagination } from '../components/Pagination';
import { ApiError } from '../types';

function formatDate(value: string | null): string {
  if (!value) return '—';
  try {
    return new Date(value).toLocaleDateString('pt-BR');
  } catch {
    return value;
  }
}

export function PatentsPage() {
  const { id: projectId = '' } = useParams();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const [search, setSearch] = useState('');
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const size = 20;

  const projectQuery = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectsApi.getProject(projectId),
    enabled: !!projectId,
  });

  const patentsQuery = useQuery({
    queryKey: ['patents', projectId, page, size, search],
    queryFn: () => patentsApi.listPatents(projectId, { page, size, q: search || undefined }),
    enabled: !!projectId,
  });

  const deleteMutation = useMutation({
    mutationFn: (patentId: string) => patentsApi.deletePatent(patentId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['patents', projectId] });
      void queryClient.invalidateQueries({ queryKey: ['project', projectId] });
      setFeedback('Patente excluída.');
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : 'Falha ao excluir patente.');
    },
  });

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Patentes</h1>
          <p>
            Projeto:{' '}
            <strong>{projectQuery.data?.title || projectId}</strong>
            {projectQuery.data ? ` · ${projectQuery.data.patentCount} patente(s)` : ''}
          </p>
        </div>
        <div className="stack-actions">
          <Link className="btn btn-secondary" to={`/projects/${projectId}/import`}>
            Importar
          </Link>
          <Link className="btn btn-secondary" to={`/projects/${projectId}/reports`}>
            Relatórios
          </Link>
          <Link className="btn btn-secondary" to="/projects">
            Voltar
          </Link>
        </div>
      </div>

      {feedback && <SuccessAlert message={feedback} />}
      {error && <ErrorAlert message={error} />}
      {patentsQuery.isError && (
        <ErrorAlert
          message={
            patentsQuery.error instanceof Error
              ? patentsQuery.error.message
              : 'Erro ao carregar patentes.'
          }
        />
      )}

      <div className="card" style={{ marginBottom: '1rem' }}>
        <form
          className="stack-actions"
          onSubmit={(e) => {
            e.preventDefault();
            setPage(0);
            setSearch(q.trim());
          }}
        >
          <div className="field" style={{ flex: 1, minWidth: 200 }}>
            <label htmlFor="q">Buscar por título</label>
            <input id="q" value={q} onChange={(e) => setQ(e.target.value)} />
          </div>
          <button type="submit" className="btn" style={{ alignSelf: 'end' }}>
            Buscar
          </button>
        </form>
      </div>

      <div className="card">
        {patentsQuery.isLoading ? (
          <Loading />
        ) : !patentsQuery.data?.content.length ? (
          <div className="empty-block">Nenhuma patente neste projeto.</div>
        ) : (
          <>
            <div className="table-wrap">
              <table className="data">
                <thead>
                  <tr>
                    <th>Título</th>
                    <th>Publicação</th>
                    <th>País</th>
                    <th>Classificação</th>
                    <th>Depositantes</th>
                    <th>Inventores</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {patentsQuery.data.content.map((patent) => (
                    <tr key={patent.id}>
                      <td>
                        <Link to={`/projects/${projectId}/patents/${patent.id}`}>
                          {patent.title || '(sem título)'}
                        </Link>
                      </td>
                      <td>
                        <div>{patent.publicationNumber || '—'}</div>
                        <div className="muted">{formatDate(patent.publicationDate)}</div>
                      </td>
                      <td>{patent.applicationCountry || '—'}</td>
                      <td>{patent.mainClassification || '—'}</td>
                      <td className="muted">
                        {(patent.applicants || []).slice(0, 3).join(', ') || '—'}
                        {(patent.applicants || []).length > 3 ? '…' : ''}
                      </td>
                      <td className="muted">
                        {(patent.inventors || []).slice(0, 3).join(', ') || '—'}
                        {(patent.inventors || []).length > 3 ? '…' : ''}
                      </td>
                      <td>
                        <div className="stack-actions">
                          <Link
                            className="btn btn-secondary btn-sm"
                            to={`/projects/${projectId}/patents/${patent.id}`}
                          >
                            Detalhe
                          </Link>
                          <button
                            type="button"
                            className="btn btn-danger btn-sm"
                            onClick={() => {
                              if (confirm('Excluir esta patente?')) {
                                deleteMutation.mutate(patent.id);
                              }
                            }}
                          >
                            Excluir
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination
              page={patentsQuery.data.page}
              totalPages={patentsQuery.data.totalPages}
              totalElements={patentsQuery.data.totalElements}
              onChange={setPage}
            />
          </>
        )}
      </div>
    </div>
  );
}
