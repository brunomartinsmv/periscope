import type { FormEvent } from 'react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import * as patentsApi from '../api/patents';
import { ErrorAlert, SuccessAlert } from '../components/Feedback';
import type { ImporterType, ImportResult } from '../types';
import { ApiError } from '../types';

const IMPORTERS: { value: ImporterType; label: string }[] = [
  { value: 'DPMA', label: 'DPMA' },
  { value: 'ESPACENET', label: 'Espacenet' },
  { value: 'PATENTSCOPE', label: 'Patentscope' },
];

export function ImportPage() {
  const { id: projectId = '' } = useParams();
  const [type, setType] = useState<ImporterType>('DPMA');
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const importMutation = useMutation({
    mutationFn: () => {
      if (!file) throw new Error('Selecione um arquivo.');
      return patentsApi.importPatents(projectId, file, type);
    },
    onSuccess: (data) => {
      setResult(data);
      setError(null);
    },
    onError: (err: unknown) => {
      setResult(null);
      setError(err instanceof ApiError || err instanceof Error ? err.message : 'Falha na importação.');
    },
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!file) {
      setError('Selecione um arquivo para importar.');
      return;
    }
    importMutation.mutate();
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h1>Importação de patentes</h1>
          <p>Envie um arquivo e escolha o importador correspondente.</p>
        </div>
        <Link className="btn btn-secondary" to={`/projects/${projectId}/patents`}>
          Ver patentes
        </Link>
      </div>

      {error && <ErrorAlert message={error} />}
      {result && (
        <SuccessAlert
          message={`Importadas ${result.imported} patente(s) via ${result.importer} (${result.fileName}).`}
        />
      )}

      <div className="card">
        <form className="form-grid" onSubmit={onSubmit}>
          <div className="field">
            <label htmlFor="type">Tipo de importador</label>
            <select
              id="type"
              value={type}
              onChange={(e) => setType(e.target.value as ImporterType)}
            >
              {IMPORTERS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="file">Arquivo</label>
            <input
              id="file"
              type="file"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
              required
            />
          </div>
          <button type="submit" className="btn" disabled={importMutation.isPending}>
            {importMutation.isPending ? 'Importando…' : 'Importar'}
          </button>
        </form>
      </div>

      {result && (
        <div className="card">
          <h2>Resultado</h2>
          <p>
            <strong>{result.imported}</strong> patente(s) importada(s) com o importador{' '}
            <strong>{result.importer}</strong>.
          </p>
          {result.messages?.length > 0 && (
            <ul>
              {result.messages.map((msg) => (
                <li key={msg}>{msg}</li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
