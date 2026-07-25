import type { FormEvent } from 'react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import * as projectsApi from '../api/projects';
import { ErrorAlert, Loading, SuccessAlert } from '../components/Feedback';
import { Modal } from '../components/Modal';
import type { Project, ProjectRequest } from '../types';
import { ApiError } from '../types';

function formatDate(value: string | null): string {
  if (!value) return '—';
  try {
    return new Date(value).toLocaleString('pt-BR');
  } catch {
    return value;
  }
}

const emptyForm: ProjectRequest = { title: '', description: '', isPublic: false };

export function ProjectsPage() {
  const queryClient = useQueryClient();
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Project | null>(null);
  const [form, setForm] = useState<ProjectRequest>(emptyForm);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const { data, isLoading, isError, error: queryError } = useQuery({
    queryKey: ['projects'],
    queryFn: projectsApi.listProjects,
  });

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (editing) {
        return projectsApi.updateProject(editing.id, form);
      }
      return projectsApi.createProject(form);
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['projects'] });
      setModalOpen(false);
      setEditing(null);
      setForm(emptyForm);
      setFeedback(editing ? 'Projeto atualizado.' : 'Projeto criado.');
      setError(null);
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : 'Falha ao salvar projeto.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => projectsApi.deleteProject(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['projects'] });
      setFeedback('Projeto excluído.');
    },
    onError: (err: unknown) => {
      setError(err instanceof ApiError ? err.message : 'Falha ao excluir projeto.');
    },
  });

  function openCreate() {
    setEditing(null);
    setForm(emptyForm);
    setModalOpen(true);
    setError(null);
  }

  function openEdit(project: Project) {
    setEditing(project);
    setForm({
      title: project.title,
      description: project.description || '',
      isPublic: project.isPublic ?? false,
    });
    setModalOpen(true);
    setError(null);
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!form.title.trim()) {
      setError('O título é obrigatório.');
      return;
    }
    saveMutation.mutate();
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Projetos</h1>
          <p>Gerencie os projetos de análise de patentes.</p>
        </div>
        <button type="button" className="btn" onClick={openCreate}>
          Novo projeto
        </button>
      </div>

      {feedback && <SuccessAlert message={feedback} />}
      {error && <ErrorAlert message={error} />}
      {isError && (
        <ErrorAlert
          message={queryError instanceof Error ? queryError.message : 'Erro ao carregar projetos.'}
        />
      )}

      <div className="card">
        {isLoading ? (
          <Loading />
        ) : !data?.length ? (
          <div className="empty-block">Nenhum projeto encontrado. Crie o primeiro.</div>
        ) : (
          <div className="table-wrap">
            <table className="data">
              <thead>
                <tr>
                  <th>Título</th>
                  <th>Descrição</th>
                  <th>Dono</th>
                  <th>Patentes</th>
                  <th>Criado</th>
                  <th>Atualizado</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {data.map((project) => (
                  <tr key={project.id}>
                    <td>
                      <Link to={`/projects/${project.id}/patents`}>{project.title}</Link>
                      {project.isPublic ? (
                        <div>
                          <span className="badge">Público</span>
                        </div>
                      ) : null}
                    </td>
                    <td className="muted">{project.description || '—'}</td>
                    <td>{project.ownerName || '—'}</td>
                    <td>{project.patentCount}</td>
                    <td>{formatDate(project.createdAt)}</td>
                    <td>{formatDate(project.updateAt)}</td>
                    <td>
                      <div className="stack-actions">
                        <Link
                          className="btn btn-secondary btn-sm"
                          to={`/projects/${project.id}/patents`}
                        >
                          Abrir
                        </Link>
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          onClick={() => openEdit(project)}
                        >
                          Editar
                        </button>
                        <button
                          type="button"
                          className="btn btn-danger btn-sm"
                          onClick={() => {
                            if (confirm(`Excluir o projeto "${project.title}"?`)) {
                              deleteMutation.mutate(project.id);
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
        )}
      </div>

      <Modal
        title={editing ? 'Editar projeto' : 'Novo projeto'}
        open={modalOpen}
        onClose={() => setModalOpen(false)}
      >
        <form className="form-grid" onSubmit={onSubmit}>
          <div className="field">
            <label htmlFor="title">Título</label>
            <input
              id="title"
              value={form.title}
              onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="description">Descrição</label>
            <textarea
              id="description"
              rows={3}
              value={form.description || ''}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            />
          </div>
          <label className="field-inline">
            <input
              type="checkbox"
              checked={!!form.isPublic}
              onChange={(e) => setForm((f) => ({ ...f, isPublic: e.target.checked }))}
            />
            Projeto público
          </label>
          <div className="stack-actions">
            <button type="submit" className="btn" disabled={saveMutation.isPending}>
              {saveMutation.isPending ? 'Salvando…' : 'Salvar'}
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setModalOpen(false)}
            >
              Cancelar
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
